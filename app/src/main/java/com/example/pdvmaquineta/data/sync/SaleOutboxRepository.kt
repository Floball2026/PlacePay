package com.example.pdvmaquineta.data.sync

import com.example.pdvmaquineta.data.di.ApplicationScope
import com.example.pdvmaquineta.data.local.database.dao.CustomerDao
import com.example.pdvmaquineta.data.local.database.dao.PaymentDao
import com.example.pdvmaquineta.data.local.database.dao.ProductDao
import com.example.pdvmaquineta.data.local.database.dao.SaleDao
import com.example.pdvmaquineta.data.local.database.dao.SaleItemDao
import com.example.pdvmaquineta.data.local.database.dao.SaleOutboxDao
import com.example.pdvmaquineta.data.local.database.entity.SaleOutboxEntity
import com.example.pdvmaquineta.data.sync.dto.PosTransactionInput
import com.example.pdvmaquineta.data.sync.dto.TransactionCustomerDto
import com.example.pdvmaquineta.data.sync.dto.TransactionItemDto
import com.example.pdvmaquineta.data.sync.dto.TransactionPaymentDto
import com.example.pdvmaquineta.data.sync.dto.TransactionTotalsDto
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
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
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao,
    private val productDao: ProductDao,
    private val outboxDao: SaleOutboxDao,
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
        val sale = saleDao.findById(saleId) ?: return
        val items = saleItemDao.getItemsForSale(saleId)
        val payment = paymentDao.findApprovedForSale(saleId) ?: return

        val grossCents = items.sumOf { it.unitPriceCents * it.quantity }
        val loyaltyCents = sale.loyaltyDiscountCents
        val netCents = payment.amountCents
        val discountCents = (grossCents - loyaltyCents - netCents).coerceAtLeast(0)
        val itemCount = items.sumOf { it.quantity }

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

        val paymentDto = TransactionPaymentDto(
            method = payment.method,
            amountCents = payment.amountCents,
            receivedCents = payment.receivedCents,
            changeCents = payment.changeCents,
            authorizationCode = payment.transactionId,
            isOffline = false
        )

        val input = PosTransactionInput(
            transactionUuid = UUID.randomUUID().toString(),
            localTransactionNumber = sale.id,
            operatorUsername = sale.operatorUsername,
            customer = customerDto,
            origin = "android_pos",
            appVersion = null,
            schemaVersion = 1,
            startedAt = iso.format(Date(sale.createdAt)),
            completedAt = iso.format(Date(payment.createdAt)),
            totals = TransactionTotalsDto(
                grossCents = grossCents,
                discountCents = discountCents,
                loyaltyDiscountCents = loyaltyCents,
                netCents = netCents,
                itemCount = itemCount
            ),
            items = itemDtos,
            payments = listOf(paymentDto)
        )

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

    private fun scheduleFlush() {
        appScope.launch { runCatching { flush() } }
    }
}
