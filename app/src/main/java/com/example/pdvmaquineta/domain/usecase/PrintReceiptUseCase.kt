package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.receipt.ReceiptPrintResult
import com.example.pdvmaquineta.domain.receipt.ReceiptPrinter
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class PrintReceiptUseCase @Inject constructor(
    private val receiptPrinter: ReceiptPrinter,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(cart: CartOverview, payment: Payment): ReceiptPrintResult {
        val result = receiptPrinter.print(cart, payment)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.RECEIPT_PRINTED,
                    detail = "Venda #${cart.sale.id}",
                    success = result is ReceiptPrintResult.Success
                )
            )
        }
        return result
    }
}
