package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.receipt.DigitalReceiptSender
import com.example.pdvmaquineta.domain.receipt.ReceiptChannel
import com.example.pdvmaquineta.domain.receipt.ReceiptSendResult
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class SendReceiptDigitallyUseCase @Inject constructor(
    private val digitalReceiptSender: DigitalReceiptSender,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        cart: CartOverview,
        payment: Payment,
        channel: ReceiptChannel,
        destination: String?
    ): ReceiptSendResult {
        val result = digitalReceiptSender.send(cart, payment, channel, destination)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.RECEIPT_SENT_DIGITALLY,
                    detail = "Venda #${cart.sale.id}; Canal: ${channel.name}",
                    success = result is ReceiptSendResult.Success
                )
            )
        }
        return result
    }
}
