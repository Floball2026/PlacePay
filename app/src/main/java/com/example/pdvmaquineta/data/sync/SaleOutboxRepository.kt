package com.example.pdvmaquineta.data.sync

import com.example.pdvmaquineta.data.di.ApplicationScope
import com.example.pdvmaquineta.data.local.database.dao.CustomerDao
import com.example.pdvmaquineta.data.local.database.dao.PaymentDao
import com.example.pdvmaquineta.data.local.database.dao.ProductDao
import com.example.pdvmaquineta.data.local.database.dao.SaleOutboxDao
import com.example.pdvmaquineta.data.local.database.entity.SaleOutboxEntity
import com.example.pdvmaquineta.data.local.database.entity.PaymentEntity
import com.example.pdvmaquineta.data.sync.dto.PosTransactionInput
import com.example.pdvmaquineta.data.sync.dto.TransactionCancellationDto
import com.example.pdvmaquineta.data.sync.dto.TransactionCustomerDto
import com.example.pdvmaquineta.data.sync.dto.TransactionItemDto
import com.example.pdvmaquineta.data.sync.dto.TransactionPaymentDto
import com.example.pdvmaquineta.data.sync.dto.TransactionTotalsDto
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import com.example.pdvmaquineta.domain.usecase.buildCartOverview
import com.google.gson.Gson
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Fila de envio de vendas (outbox). A venda concluida vira uma linha em
// sale_outbox com o JSON pronto e um transaction_uuid estavel; o envio de
// rede acontece depois, em segundo plano, e reusa o mesmo UUID em cada
// tentativa (o servidor deduplica). Garante que nenhuma venda se perca mesmo
// sem internet: o dado duravel esta no banco antes de qualquer chamada de rede.
@Singleton
class SaleOutboxRepository @Inject constructor(
    private val api: PosApiService,
    private val settings: SyncSettings,
    private val saleRepository: SaleRepository,
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao,
    private val productDao: ProductDao,
    private val outboxDao: SaleOutboxDao,
    private val deviceInfo: DeviceInfoProvider,
    @ApplicationScope private val appScope: CoroutineScope
) : SaleSyncQueue {

    private val gson = Gson()
    private val flushMutex = Mutex()

    private val iso: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    override suspend fun enqueue(saleId: Long) {
        buildAndInsert(saleId)
        scheduleFlush()
    }

    override suspend fun reconcileAndFlush() {
        if (!settings.isActivated) return
        for (id in outboxDao.completedSalesNotQueued()) {
            runCatching { buildAndInsert(id) }
        }
        flush()
    }

    // Monta o payload e grava na fila. Idempotente: se a venda ja esta na fila,
    // nao faz nada (preserva o UUID original). So chama rede em flush().
    private suspend fun buildAndInsert(saleId: Long) {
        if (outboxDao.findBySaleId(saleId) != null) return
        val input = buildPayload(saleId, UUID.randomUUID().toString(), cancellation = null) ?: return
        val now = System.currentTimeMillis()
        outboxDao.insert(
            SaleOutboxEntity(
                saleId = saleId,
                transactionUuid = input.transactionUuid,
                payload = gson.toJson(input),
                status = SaleOutboxEntity.STATUS_PENDING,
                attempts = 0,
                lastError = null,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    // Monta o payload da venda. Devolve null so quando a venda nao existe mais
    // no banco local — nesse caso nao ha o que enviar.
    private suspend fun buildPayload(
        saleId: Long,
        transactionUuid: String,
        cancellation: TransactionCancellationDto?
    ): PosTransactionInput? {
        val sale = saleRepository.findById(saleId) ?: return null
        val items = saleRepository.observeItems(saleId).first()
        val payments = paymentDao.findAllApprovedForSale(saleId)

        if (payments.isEmpty()) {
            // Venda concluida sem pagamento aprovado e uma anomalia. Antes o
            // envio era abortado em silencio e a venda sumia para sempre da
            // retaguarda. Agora ela sobe assim mesmo: o servidor a grava como
            // divergente ("Venda sem nenhum pagamento") e ela aparece na fila
            // de excecao do painel, onde alguem pode olhar.
            Log.w(TAG, "venda $saleId concluida sem pagamento aprovado; subindo como divergente")
        }

        // Totais pela MESMA funcao que a tela do caixa e o cupom usam
        // (buildCartOverview, em CartCalculations.kt). Antes o outbox refazia a
        // conta por fora: `netCents = payment.amountCents` e o desconto deduzido
        // por subtracao. Com um unico pagamento isso batia por acidente; num
        // pagamento dividido, a venda subia pela metade. Agora o numero enviado
        // ao SaaS e, por construcao, o mesmo que o cliente viu.
        val cart = buildCartOverview(sale, items)
        val grossCents = cart.subtotalCents
        val discountCents = cart.discountCents
        val loyaltyCents = cart.loyaltyDiscountCents
        val netCents = cart.totalCents
        val itemCount = cart.itemCount

        val customerDto = sale.customerId?.let { cid ->
            customerDao.findById(cid)?.let { c ->
                TransactionCustomerDto(document = c.document, name = c.name)
            }
        }

        val itemDtos = items.map { item ->
            val product = productDao.findById(item.productId)
            TransactionItemDto(
                productRefId = product?.remoteId,
                barcode = product?.barcode,
                name = item.productName,
                quantity = item.quantity,
                unitPriceCents = item.unitPriceCents,
                totalCents = item.unitPriceCents * item.quantity
            )
        }

        val paymentDtos = payments.map { toPaymentDto(it) }

        // Momento da conclusao: o ultimo pagamento aprovado; sem pagamento,
        // a propria atualizacao da venda.
        val completedAtMillis = payments.maxOfOrNull { it.createdAt } ?: sale.updatedAt

        return PosTransactionInput(
            transactionUuid = transactionUuid,
            localTransactionNumber = sale.id,
            operatorUsername = sale.operatorUsername,
            customer = customerDto,
            origin = "android_pos",
            appVersion = runCatching { deviceInfo.build().appVersion }.getOrNull(),
            schemaVersion = SCHEMA_VERSION,
            startedAt = iso.format(Date(sale.createdAt)),
            completedAt = iso.format(Date(completedAtMillis)),
            totals = TransactionTotalsDto(
                grossCents = grossCents,
                discountCents = discountCents,
                loyaltyDiscountCents = loyaltyCents,
                netCents = netCents,
                itemCount = itemCount
            ),
            items = itemDtos,
            payments = paymentDtos,
            cancellation = cancellation
        )
    }

    private fun toPaymentDto(payment: PaymentEntity): TransactionPaymentDto {
        val isCash = payment.method == PaymentMethod.CASH.name
        // NSU da adquirente. Vendas gravadas antes da v16 do banco nao tem a
        // coluna preenchida; ai o `transactionId` (que guardava o NSU de
        // resposta) ainda serve de fallback.
        val nsu = if (isCash) null else (payment.nsuAcquirer ?: payment.transactionId)
        return TransactionPaymentDto(
            method = payment.method,
            amountCents = payment.amountCents,
            receivedCents = payment.receivedCents,
            changeCents = payment.changeCents,
            nsu = nsu,
            authorizationCode = payment.transactionId,
            // Nome real devolvido pela adquirente. Antes ia "paytime" fixo, que
            // e o facilitador e nao a adquirente da transacao.
            acquirer = if (isCash) null else payment.acquirerName,
            installments = payment.installments,
            isOffline = false
        )
    }

    // Envia o que esta pendente. Mutex evita dois flushes concorrentes
    // postarem a mesma linha (enqueue agenda um flush a cada venda).
    override suspend fun flush() {
        if (!settings.isActivated) return
        flushMutex.withLock {
            val pending = outboxDao.pending(limit = 50)
            for (entry in pending) {
                try {
                    val input = gson.fromJson(entry.payload, PosTransactionInput::class.java)
                    api.postTransaction(input)
                    val now = System.currentTimeMillis()
                    outboxDao.markSent(entry.id, now)
                    settings.lastSyncAt = iso.format(java.util.Date(now))
                } catch (e: IOException) {
                    // Sem rede: para o lote; tenta de novo no proximo gatilho.
                    outboxDao.markFailed(entry.id, e.message ?: "Falha de rede", System.currentTimeMillis())
                    break
                } catch (e: HttpException) {
                    outboxDao.markFailed(entry.id, "HTTP ${e.code()}", System.currentTimeMillis())
                    // 401: token invalido/expirado — reativar o terminal.
                    if (e.code() == 401) break
                } catch (e: Exception) {
                    outboxDao.markFailed(entry.id, e.message ?: "Erro", System.currentTimeMillis())
                }
            }
        }
    }

    override suspend fun enqueueCancellation(
        saleId: Long,
        reason: String,
        cancelledBy: String?,
        cancelledAtMillis: Long
    ) {
        // So faz sentido avisar o SaaS de uma venda que chegou a entrar na fila.
        // Venda cancelada antes de ser concluida nunca existiu la.
        val entry = outboxDao.findBySaleId(saleId) ?: return
        val cancellation = TransactionCancellationDto(
            reason = reason,
            cancelledAt = iso.format(Date(cancelledAtMillis)),
            cancelledBy = cancelledBy
        )
        // Parte do payload ORIGINAL e so acrescenta o cancelamento, em vez de
        // remontar a venda do zero: no momento em que este metodo roda, o
        // estorno de fidelidade ja mexeu na venda, e remontar mudaria os totais
        // de uma venda que ja subiu. O que o SaaS recebe continua sendo,
        // numero a numero, o que ele recebeu da primeira vez.
        //
        // O transactionUuid tambem e o original de proposito: e ele que faz o
        // servidor reconhecer o replay e aplicar o cancelamento na venda certa.
        val original = runCatching {
            gson.fromJson(entry.payload, PosTransactionInput::class.java)
        }.getOrNull() ?: buildPayload(saleId, entry.transactionUuid, cancellation = null) ?: return

        val input = original.copy(cancellation = cancellation)
        outboxDao.replacePayload(entry.id, gson.toJson(input), System.currentTimeMillis())
        scheduleFlush()
    }

    private fun scheduleFlush() {
        appScope.launch { runCatching { flush() } }
    }

    private companion object {
        const val TAG = "SaleOutbox"
        const val SCHEMA_VERSION = 2
    }
}
