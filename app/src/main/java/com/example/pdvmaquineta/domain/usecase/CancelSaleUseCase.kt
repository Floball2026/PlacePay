package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class CancelSaleResult {
    data object Success : CancelSaleResult()
    data object ReasonRequired : CancelSaleResult()
}

class CancelSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val undoLoyaltyRedemptionUseCase: UndoLoyaltyRedemptionUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(saleId: Long, reason: String): CancelSaleResult {
        if (reason.isBlank()) return CancelSaleResult.ReasonRequired

        val saleBeforeCancel = saleRepository.findById(saleId)

        saleRepository.cancelSale(saleId, reason)

        // Uma venda cancelada com resgate de fidelidade já aplicado não pode
        // deixar o cliente com pontos/valor consumidos por uma compra que
        // nunca foi paga.
        if (saleBeforeCancel != null) {
            undoLoyaltyRedemptionUseCase(saleBeforeCancel, LoyaltyRedemptionReversalReason.SALE_CANCELLED)
        }

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.SALE_CANCELLED,
                    detail = "Motivo: $reason",
                    success = true
                )
            )
        }
        return CancelSaleResult.Success
    }
}
