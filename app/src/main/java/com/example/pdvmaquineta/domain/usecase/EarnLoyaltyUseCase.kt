package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.loyalty.LoyaltyEngineFactory
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class EarnLoyaltyResult {
    data class Earned(val transaction: LoyaltyTransaction) : EarnLoyaltyResult()
    data object NoCustomer : EarnLoyaltyResult()
    data object NoActiveConfig : EarnLoyaltyResult()
}

// Chamado só quando o pagamento é de fato aprovado (não na finalização, que
// só move a venda pra AWAITING_PAYMENT) — gerar pontos/visita pra uma venda
// que acabou recusada ou expirou no timeout seria dado de fidelidade errado.
class EarnLoyaltyUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(sale: Sale, saleTotalCents: Long): EarnLoyaltyResult {
        val customerId = sale.customerId ?: return EarnLoyaltyResult.NoCustomer
        val config = loyaltyConfigRepository.getActive() ?: return EarnLoyaltyResult.NoActiveConfig
        val engine = LoyaltyEngineFactory.create(config)

        val earn = engine.calculateEarn(saleTotalCents)
        val transaction = loyaltyTransactionRepository.recordTransaction(
            customerId = customerId,
            saleId = sale.id,
            mode = config.mode,
            type = earn.type,
            points = earn.points,
            amountCents = null
        )

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            val action = if (earn.type == LoyaltyTransactionType.EARNED) {
                AuditAction.LOYALTY_POINTS_EARNED
            } else {
                AuditAction.LOYALTY_VISIT_COUNTED
            }
            val detail = if (earn.type == LoyaltyTransactionType.EARNED) {
                "Cliente #$customerId; Pontos: ${earn.points}"
            } else {
                "Cliente #$customerId; Visita contabilizada"
            }
            auditRepository.log(
                AuditEntry(userId = actor.id, username = actor.username, action = action, detail = detail, success = true)
            )
        }
        return EarnLoyaltyResult.Earned(transaction)
    }
}
