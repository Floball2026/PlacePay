package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import javax.inject.Inject

sealed class CancelSaleResult {
    data object Success : CancelSaleResult()
    data object ReasonRequired : CancelSaleResult()
}

class CancelSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val undoLoyaltyRedemptionUseCase: UndoLoyaltyRedemptionUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager,
    private val saleSyncQueue: SaleSyncQueue
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

        // Avisa a retaguarda. Ate aqui o cancelamento morria no terminal: a
        // venda ja tinha subido como concluida e nunca mais era corrigida, entao
        // o painel mostrava faturamento maior que o real e o estoque nao voltava.
        // Nao bloqueia o cancelamento se falhar — o outbox tenta de novo depois.
        runCatching {
            saleSyncQueue.enqueueCancellation(
                saleId = saleId,
                reason = reason,
                cancelledBy = actor?.username,
                cancelledAtMillis = System.currentTimeMillis()
            )
        }

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
