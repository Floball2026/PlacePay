package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.payment.PaymentGateway
import javax.inject.Inject

// Reimprime o comprovante da ultima transacao de cartao/PIX (via REIMPRESSAO).
class ReprintCardReceiptUseCase @Inject constructor(
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(): Boolean = paymentGateway.reprintLastReceipt()
}
