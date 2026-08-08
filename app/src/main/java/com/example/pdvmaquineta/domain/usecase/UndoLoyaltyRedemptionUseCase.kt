package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class UndoLoyaltyRedemptionResult {
    data object Success : UndoLoyaltyRedemptionResult()
    data object NothingToUndo : UndoLoyaltyRedemptionResult()
}

// Diferencia no histórico de auditoria um estorno pedido manualmente pelo
// operador (botão "Desfazer resgate") de um estorno automático disparado
// pelo cancelamento da venda — mesma ação, motivo diferente.
enum class LoyaltyRedemptionReversalReason {
    MANUAL,
    SALE_CANCELLED
}

// Estorna o resgate ativo da venda: grava uma transação REDEMPTION_REVERSED
// (a original nunca é apagada) e zera loyaltyDiscountCents, liberando o
// carrinho pra edição de novo. Também usado pelo CancelSaleUseCase — uma
// venda cancelada com resgate já aplicado não pode deixar o cliente com
// pontos/valor consumidos por uma compra que nunca foi paga.
class UndoLoyaltyRedemptionUseCase @Inject constructor(
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository,
    private val saleRepository: SaleRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        sale: Sale,
        reason: LoyaltyRedemptionReversalReason = LoyaltyRedemptionReversalReason.MANUAL
    ): UndoLoyaltyRedemptionResult {
        val customerId = sale.customerId ?: return UndoLoyaltyRedemptionResult.NothingToUndo
        if (sale.loyaltyDiscountCents <= 0) return UndoLoyaltyRedemptionResult.NothingToUndo

        val redemption = loyaltyTransactionRepository.findLatestRedemptionForSale(sale.id)
            ?: return UndoLoyaltyRedemptionResult.NothingToUndo

        loyaltyTransactionRepository.recordTransaction(
            customerId = customerId,
            saleId = sale.id,
            mode = redemption.mode,
            type = LoyaltyTransactionType.REDEMPTION_REVERSED,
            points = redemption.points,
            amountCents = redemption.amountCents
        )
        saleRepository.setLoyaltyDiscount(sale.id, 0)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            val detail = when (reason) {
                LoyaltyRedemptionReversalReason.MANUAL ->
                    "Cliente #$customerId; Estorno de: ${redemption.amountCents ?: 0} centavos"
                LoyaltyRedemptionReversalReason.SALE_CANCELLED ->
                    "Cliente #$customerId; Estorno automático por cancelamento da venda #${sale.id}; " +
                        "Valor: ${redemption.amountCents ?: 0} centavos"
            }
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.LOYALTY_REDEMPTION_REVERSED,
                    detail = detail,
                    success = true
                )
            )
        }
        return UndoLoyaltyRedemptionResult.Success
    }
}
