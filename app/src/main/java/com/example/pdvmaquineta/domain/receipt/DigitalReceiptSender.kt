package com.example.pdvmaquineta.domain.receipt

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.usecase.CartOverview

enum class ReceiptChannel {
    WHATSAPP,
    SMS,
    EMAIL
}

sealed class ReceiptSendResult {
    data object Success : ReceiptSendResult()
    data class Failure(val reason: String) : ReceiptSendResult()
}

// Abstração sobre o envio digital do comprovante. Hoje só existe
// MockDigitalReceiptSender (nenhum provedor de WhatsApp/SMS/e-mail contratado
// ainda) — mesmo padrão de PaymentGateway/ReceiptPrinter.
interface DigitalReceiptSender {
    suspend fun send(
        cart: CartOverview,
        payment: Payment,
        channel: ReceiptChannel,
        destination: String?
    ): ReceiptSendResult
}
