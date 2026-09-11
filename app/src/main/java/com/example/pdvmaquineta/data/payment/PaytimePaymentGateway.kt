package com.example.pdvmaquineta.data.payment

import android.content.Context
import android.util.Log
import com.example.pdvmaquineta.data.sync.PaytimeConfigStore
import com.example.pdvmaquineta.domain.payment.CardTransactionDetails
import com.example.pdvmaquineta.domain.payment.PaymentGateway
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.payment.PaymentRequest
import com.example.pdvmaquineta.domain.payment.PaymentResult
import com.paytime.payossdk.PayOsSdkPayment
import com.paytime.payossdk.PayOsSdkDevice
import com.paytime.payossdk.PayOsSdkPrinter
import com.pax.dal.entity.EFontTypeAscii
import com.pax.dal.entity.EFontTypeExtCode
import com.paytime.payossdk.external.model.transaction.PayOsSdkTransactionParams
import com.paytime.payossdk.external.model.transaction.PayOsSdkTransactionStatus
import com.paytime.payossdk.external.model.transaction.PayOsSdkTransactionStore
import com.paytime.payossdk.external.model.transaction.PayOsSdkTransactionType
import com.paytime.payossdk.external.model.auth.PayOsSdkEstablishment
import com.tectoy.dm_sdk.external.model.ConnectorCallback
import com.tectoy.dm_sdk.external.model.CreditType
import com.tectoy.dm_sdk.external.model.CvvTypeEnum
import com.tectoy.dm_sdk.external.model.RequestFlowEnum
import com.tectoy.payment.external.model.NotificationType
import com.tectoy.payment.external.model.ReturnCodes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// Pagamento real pela maquineta (PAX A960) via SmartPOS SDK da PayTime.
// Fluxo conforme a doc: configure() -> init(codigo) -> updateTerminalConfigurations()
// -> tableLoad() (carga de tabelas EMV, necessaria pra ler cartao) -> makeTransaction().
// Dinheiro continua resolvido localmente (nao passa pelo SDK).
@Singleton
class PaytimePaymentGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paytimeConfig: PaytimeConfigStore,
    private val statusBus: PaymentStatusBus
) : PaymentGateway {

    private val payment = PayOsSdkPayment.instance
    private val sdkPrinter = PayOsSdkPrinter.instance

    @Volatile private var printerConfigured = false
    @Volatile private var lastCardReceipt: String? = null
    @Volatile private var lastNsu: String? = null
    // Chave imutavel da transacao no SDK — e o que o revertTransaction exige.
    @Volatile private var lastNsuRequest: String? = null
    @Volatile private var lastMethod: PaymentMethod? = null
    @Volatile private var hasApproved = false

    @Volatile private var deviceConfigured = false
    @Volatile private var estabDoc: String? = null
    @Volatile private var estabName: String? = null
    @Volatile private var termSerial: String? = null

    @Volatile private var configured = false
    @Volatile private var tablesReady = false
    @Volatile private var initMessage: String? = null

    override suspend fun charge(request: PaymentRequest): PaymentResult {
        // Dinheiro: local, sem SDK (calcula troco e aprova).
        if (request.method == PaymentMethod.CASH) {
            val received = request.receivedCents ?: request.amountCents
            val change = (received - request.amountCents).coerceAtLeast(0)
            return PaymentResult.Approved(transactionId = "CASH", changeCents = change)
        }

        return withContext(Dispatchers.Main) {
            statusBus.update("Preparando terminal...")
            runCatching {
                val initError = ensureReady()
                if (initError != null) {
                    return@runCatching PaymentResult.Error(initError)
                }
                val params = PayOsSdkTransactionParams(
                    request.amountCents,
                    CreditType.NO_INSTALLMENT,
                    txType(request.method),
                    null
                )
                val result = awaitTransaction(request.method, params)
                if (result is PaymentResult.Approved) {
                    runCatching { printCardSlips(request.method, lastCardReceipt.orEmpty(), lastNsu) }
                        .onFailure { Log.w(TAG, "falha ao imprimir comprovante", it) }
                }
                result
            }.getOrElse { e ->
                // Cancelamento (usuario tocou "Cancelar") deve propagar, nao
                // virar "recusado". abort() ja roda via invokeOnCancellation.
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "charge exception", e)
                PaymentResult.Error(e.message ?: "Erro no pagamento")
            }.also { statusBus.clear() }
        }
    }

    override suspend fun reprintLastReceipt(): Boolean {
        if (!hasApproved) return false
        val method = lastMethod ?: return false
        return runCatching { printCardSlips(method, lastCardReceipt.orEmpty(), lastNsu, reprint = true) }
            .onFailure { Log.w(TAG, "falha na reimpressao do comprovante", it) }
            .isSuccess
    }

    // configure (1x) -> init (1x) -> update config + tableLoad (1x por processo).
    // Retorna null se pronto; mensagem de erro caso o init falhe.
    private suspend fun ensureReady(): String? {
        if (!configured) {
            payment.configure(context)
            configured = true
        }
        if (!payment.isInitialized()) {
            val initErr = doInit()
            if (initErr != null) return initErr
        }
        if (!tablesReady) {
            // Puxa a configuracao do terminal e carrega as tabelas EMV/AID.
            // Sem a carga de tabelas o terminal nao consegue ler o cartao.
            statusBus.update("Preparando terminal...")
            val upd = awaitOp(60_000, "updateConfig") { cb -> payment.updateTerminalConfigurations(cb) }
            Log.d(TAG, "updateTerminalConfigurations -> ${upd.ifBlank { "ok" }}")
            val tl = awaitOp(120_000, "tableLoad") { cb -> payment.tableLoad(cb) }
            Log.d(TAG, "tableLoad -> ${tl.ifBlank { "ok" }}")
            // Roda so uma vez por processo (mesmo se demorar); erros ficam no log.
            tablesReady = true
        }
        return null
    }

    private suspend fun doInit(): String? {
        initMessage = null
        payment.init(paytimeConfig.getActivationCode(), object : ConnectorCallback {
            override fun onApproved(p0: Any?) {}
            override fun onError(p0: ReturnCodes, p1: List<String>, p2: Int?) {
                initMessage = "PayTime init erro: " + p1.joinToString("; ").ifBlank { p0.toString() }
                Log.w(TAG, initMessage ?: "")
            }
            override fun onRequest(p0: RequestFlowEnum, p1: Any?) { Log.d(TAG, "init onRequest $p0 / $p1") }
            override fun onMessage(p0: NotificationType, p1: String) {
                initMessage = "PayTime init: $p1"
                statusBus.update(p1)
                Log.d(TAG, "init onMessage $p0 / $p1")
            }
        })
        withTimeoutOrNull(25_000) {
            while (!payment.isInitialized() && initMessage?.startsWith("PayTime init erro") != true) delay(200)
        }
        return if (payment.isInitialized()) {
            null
        } else {
            initMessage ?: "Terminal PayTime nao respondeu ao init. Verifique rede, cadastro do equipamento e o Codigo de Ativacao."
        }
    }

    // Executa uma operacao do SDK baseada em callback e espera concluir.
    // Retorna "" em sucesso, ou uma mensagem em erro/timeout.
    private suspend fun awaitOp(
        timeoutMs: Long,
        tag: String,
        start: (ConnectorCallback) -> Unit
    ): String = (withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            start(object : ConnectorCallback {
                override fun onApproved(p0: Any?) { if (cont.isActive) cont.resume("") }
                override fun onError(p0: ReturnCodes, p1: List<String>, p2: Int?) {
                    val msg = "$tag erro: " + p1.joinToString("; ").ifBlank { p0.toString() }
                    Log.w(TAG, msg)
                    if (cont.isActive) cont.resume(msg)
                }
                override fun onRequest(p0: RequestFlowEnum, p1: Any?) { Log.d(TAG, "$tag onRequest $p0 / $p1") }
                override fun onMessage(p0: NotificationType, p1: String) { statusBus.update(p1); Log.d(TAG, "$tag onMessage $p0 / $p1") }
            })
        }
    }) ?: "$tag: tempo esgotado"

    private suspend fun awaitTransaction(
        method: PaymentMethod,
        params: PayOsSdkTransactionParams
    ): PaymentResult {
        lastCardReceipt = null
        lastNsu = null
        lastNsuRequest = null
        lastMethod = method
        hasApproved = false
        // PIX espera o cliente pagar (status PENDING ate confirmar); cartao e rapido.
        val timeout = if (method == PaymentMethod.PIX) 360_000L else 120_000L
        return (withTimeoutOrNull(timeout) {
            suspendCancellableCoroutine { cont ->
                payment.makeTransaction(params, object : ConnectorCallback {
                    override fun onApproved(p0: Any?) {
                        val store = p0 as? PayOsSdkTransactionStore
                        val status = store?.transactionStatus
                        Log.d(TAG, "tx onApproved status=$status nsu=${store?.nsuResponse}")
                        when (status) {
                            // PIX: primeiro retorno vem PENDING; aguarda o proximo callback.
                            PayOsSdkTransactionStatus.PENDING -> {}
                            PayOsSdkTransactionStatus.CONFIRMED, null -> {
                                lastCardReceipt = store?.transactionReceipt
                                lastNsu = store?.nsuResponse
                                lastNsuRequest = store?.nsuRequest
                                hasApproved = true
                                Log.d(TAG, "tx CONFIRMED status ok, receiptLen=${store?.transactionReceipt?.length ?: 0}")
                                val txId = store?.nsuResponse ?: store?.authAcquirer ?: store?.auto ?: "OK"
                                // Guarda tudo que identifica a transacao na adquirente. Ate
                                // aqui so o NSU de resposta sobrevivia — sem o nsuRequest
                                // nenhuma venda paga podia ser estornada depois.
                                val card = store?.let {
                                    CardTransactionDetails(
                                        nsuRequest = it.nsuRequest,
                                        nsuAcquirer = it.nsuAcquirer ?: it.nsuResponse,
                                        // O SDK nao expoe o nome da adquirente ao Kotlin
                                        // (`acquirerName` e privado na declaracao, mesmo
                                        // aparecendo publico no bytecode). Enquanto a PayTime
                                        // for o unico meio de captura, o canal identifica a
                                        // origem; a coluna no banco fica pronta pro dia em que
                                        // houver de onde tirar o nome real.
                                        acquirerName = ACQUIRER_NAME,
                                        brand = it.brand,
                                        panMasked = it.panMasked,
                                        installments = it.installments
                                    )
                                }
                                if (cont.isActive) {
                                    cont.resume(
                                        PaymentResult.Approved(transactionId = txId, card = card)
                                    )
                                }
                            }
                            else -> { // CANCELLED, REFUNDED, FAILED
                                val reason = status?.name ?: "Transacao nao confirmada"
                                if (cont.isActive) cont.resume(PaymentResult.Declined(reason))
                            }
                        }
                    }

                    override fun onError(p0: ReturnCodes, p1: List<String>, p2: Int?) {
                        val reason = p1.joinToString("; ").ifBlank { p0.toString() }
                        Log.w(TAG, "tx onError $p0 / $reason")
                        if (cont.isActive) cont.resume(PaymentResult.Declined(reason))
                    }

                    // Interacoes do fluxo (doc PayTime). Chip/aproximacao normalmente so
                    // dispara APPLICATION; os demais sao de entrada manual (digitado).
                    override fun onRequest(p0: RequestFlowEnum, p1: Any?) {
                        Log.d(TAG, "tx onRequest $p0 / $p1")
                        runCatching {
                            when (p0) {
                                RequestFlowEnum.APPLICATION -> payment.selectApplication(0)
                                RequestFlowEnum.LAST_DIGITS_CONFIRMATION ->
                                    (p1 as? String)?.let { payment.setLastDigits(it) }
                                RequestFlowEnum.CVV_TYPE -> payment.setCvvType(CvvTypeEnum.NOT_REQUESTED)
                                else -> {}
                            }
                        }.onFailure { Log.w(TAG, "onRequest handle fail: ${it.message}") }
                    }

                    override fun onMessage(p0: NotificationType, p1: String) {
                        statusBus.update(p1)
                        Log.d(TAG, "tx onMessage $p0 / $p1")
                    }
                })
                cont.invokeOnCancellation { runCatching { payment.abort() } }
            }
        }) ?: PaymentResult.Timeout
    }

    // Imprime o comprovante da transacao em DUAS vias (estabelecimento + cliente),
    // conforme exigido pela homologacao PayTime: rotulo da via, nome + CPF/CNPJ
    // do estabelecimento, "1a VIA"/"REIMPRESSAO", corpo do comprovante da
    // adquirente, e no rodape CV (credito/debito) ou E2E (PIX) + TERM (serial).
    private suspend fun printCardSlips(
        method: PaymentMethod,
        body: String,
        nsu: String?,
        reprint: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ensureEstablishment()
        ensureSerial()
        printOneSlip("VIA DO ESTABELECIMENTO", method, body, nsu, reprint)
        printOneSlip("VIA DO CLIENTE", method, body, nsu, reprint)
    }

    private fun printOneSlip(
        via: String,
        method: PaymentMethod,
        body: String,
        nsu: String?,
        reprint: Boolean
    ) {
        if (!printerConfigured) {
            sdkPrinter.configure(context)
            printerConfigured = true
        }
        sdkPrinter.init()
        sdkPrinter.setGray(3)
        sdkPrinter.fontSet(EFontTypeAscii.FONT_12_24, EFontTypeExtCode.FONT_16_16)
        for (line in buildSlipHeader(via, reprint)) sdkPrinter.printStr(line + "\n", "utf-8")
        if (body.isNotBlank()) for (line in body.split("\n")) sdkPrinter.printStr(line + "\n", "utf-8")
        for (line in buildSlipFooter(method, nsu)) sdkPrinter.printStr(line + "\n", "utf-8")
        sdkPrinter.step(120)
        val r = sdkPrinter.start()
        Log.d(TAG, "printOneSlip '$via' start -> $r")
    }

    // Busca (uma vez) os dados do estabelecimento pro cabecalho do comprovante.
    private suspend fun ensureEstablishment() {
        if (estabDoc != null || estabName != null) return
        runCatching {
            val e: PayOsSdkEstablishment? = payment.getActiveEstablishmentDetails()
            if (e != null) {
                estabDoc = e.document
                estabName = e.softDescriptor?.takeIf { it.isNotBlank() }
                    ?: "${e.firstName} ${e.lastName}".trim()
            }
        }.onFailure { Log.w(TAG, "getActiveEstablishmentDetails falhou", it) }
    }

    // Busca (uma vez) o numero de serie do terminal (label TERM).
    private fun ensureSerial() {
        if (termSerial != null) return
        runCatching {
            val dev = PayOsSdkDevice.instance
            if (!deviceConfigured) {
                dev.configure(context)
                deviceConfigured = true
            }
            termSerial = dev.getSerialNumber().getOrNull()
        }.onFailure { Log.w(TAG, "getSerialNumber falhou", it) }
    }

    private fun buildSlipHeader(via: String, reprint: Boolean): List<String> {
        val l = mutableListOf<String>()
        l += center(via)
        estabName?.takeIf { it.isNotBlank() }?.let { l += center(it.uppercase()) }
        formatDoc(estabDoc)?.let { l += center(it) }
        l += center(if (reprint) "REIMPRESSAO" else "1a VIA")
        l += "-".repeat(COLS)
        return l
    }

    private fun buildSlipFooter(method: PaymentMethod, nsu: String?): List<String> {
        val l = mutableListOf<String>()
        l += "-".repeat(COLS)
        if (!nsu.isNullOrBlank()) {
            when (method) {
                PaymentMethod.PIX -> l += "E2E: $nsu"
                PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> l += "CV: $nsu"
                else -> {}
            }
        }
        termSerial?.takeIf { it.isNotBlank() }?.let { l += "TERM: $it" }
        return l
    }

    private fun formatDoc(doc: String?): String? {
        val d = doc?.filter { it.isDigit() } ?: return null
        return when (d.length) {
            11 -> "CPF: ${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}"
            14 -> "CNPJ: ${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}"
            else -> if (d.isBlank()) null else "DOC: $d"
        }
    }

    private fun center(s: String): String {
        if (s.length >= COLS) return s.take(COLS)
        val pad = (COLS - s.length) / 2
        return " ".repeat(pad) + s
    }

    private fun txType(method: PaymentMethod): PayOsSdkTransactionType = when (method) {
        PaymentMethod.CREDIT_CARD -> PayOsSdkTransactionType.CREDIT
        PaymentMethod.DEBIT_CARD -> PayOsSdkTransactionType.DEBIT
        PaymentMethod.PIX -> PayOsSdkTransactionType.PIX
        PaymentMethod.CASH -> PayOsSdkTransactionType.CREDIT // nao chega aqui
    }

    private companion object {
        const val ACQUIRER_NAME = "paytime"
        const val TAG = "PaytimePay"
        const val COLS = 32
    }
}
