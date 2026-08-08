package com.example.pdvmaquineta.data.payment

import com.example.pdvmaquineta.domain.payment.PaymentGateway
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.payment.PaymentRequest
import com.example.pdvmaquineta.domain.payment.PaymentResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val CASH_PROCESSING_DELAY_MS = 300L
private const val CARD_PROCESSING_DELAY_MS = 1_200L
private const val TIMEOUT_DELAY_MS = 3_000L

// Últimos dois dígitos do valor decidem o resultado simulado — só usado pra
// QA testar recusa/timeout manualmente sem mexer em código. Isso é
// deliberadamente invisível pro resto do app: só PaymentResult sai daqui.
// Quando a Plasyplay tiver SDK/doc, essa classe inteira é trocada por uma
// implementação real de PaymentGateway, sem tocar em domain/presentation.
private const val DECLINE_CENTS_MARKER = 13L
private const val TIMEOUT_CENTS_MARKER = 66L

class MockPaymentGateway @Inject constructor() : PaymentGateway {

    override suspend fun charge(request: PaymentRequest): PaymentResult {
        if (request.method == PaymentMethod.CASH) {
            delay(CASH_PROCESSING_DELAY_MS)
            val received = request.receivedCents ?: request.amountCents
            val change = (received - request.amountCents).coerceAtLeast(0)
            return PaymentResult.Approved(transactionId = newTransactionId(), changeCents = change)
        }

        return when (request.amountCents % 100) {
            TIMEOUT_CENTS_MARKER -> {
                delay(TIMEOUT_DELAY_MS)
                PaymentResult.Timeout
            }
            DECLINE_CENTS_MARKER -> {
                delay(CARD_PROCESSING_DELAY_MS)
                PaymentResult.Declined("Pagamento recusado pela operadora (simulado)")
            }
            else -> {
                delay(CARD_PROCESSING_DELAY_MS)
                PaymentResult.Approved(transactionId = newTransactionId())
            }
        }
    }

    private fun newTransactionId(): String = "MOCK-${UUID.randomUUID().toString().take(8)}"
}
