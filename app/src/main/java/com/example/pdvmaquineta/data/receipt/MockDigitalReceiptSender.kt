package com.example.pdvmaquineta.data.receipt

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.receipt.DigitalReceiptSender
import com.example.pdvmaquineta.domain.receipt.ReceiptChannel
import com.example.pdvmaquineta.domain.receipt.ReceiptSendResult
import com.example.pdvmaquineta.domain.usecase.CartOverview
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val SEND_DELAY_MS = 500L

// Sem provedor de WhatsApp/SMS/e-mail contratado ainda — só simula sucesso.
// Quando um provedor for escolhido, essa classe é trocada por uma
// implementação real de DigitalReceiptSender, sem tocar em
// domain/presentation.
class MockDigitalReceiptSender @Inject constructor() : DigitalReceiptSender {
    override suspend fun send(
        cart: CartOverview,
        payment: Payment,
        channel: ReceiptChannel,
        destination: String?
    ): ReceiptSendResult {
        delay(SEND_DELAY_MS)
        return ReceiptSendResult.Success
    }
}
