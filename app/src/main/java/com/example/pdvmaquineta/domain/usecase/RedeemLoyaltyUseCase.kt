package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class RedeemLoyaltyResult {
    data class Success(val redeemedCents: Long) : RedeemLoyaltyResult()
    data object NoActiveConfig : RedeemLoyaltyResult()
    data object NothingToRedeem : RedeemLoyaltyResult()
}

// Resgate parcial automático: consome só o necessário (pontos ou prêmio) pra
// cobrir o que falta pagar, nunca mais que isso — o excedente de saldo
// permanece disponível pro cliente. Soma em loyaltyDiscountCents, ao lado do
// desconto manual já existente — os dois convivem e somam no total da venda.
class RedeemLoyaltyUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository,
    private val getLoyaltyStatusUseCase: GetLoyaltyStatusUseCase,
    private val saleRepository: SaleRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    // saleNetCents é o que falta pagar depois do desconto manual (subtotal -
    // discountCents), não o subtotal bruto — resgatar não pode valer mais do
    // que isso.
    suspend operator fun invoke(sale: Sale, customerId: Long, saleNetCents: Long): RedeemLoyaltyResult {
        val config = loyaltyConfigRepository.getActive() ?: return RedeemLoyaltyResult.NoActiveConfig
        val redeemable = getLoyaltyStatusUseCase(customerId, saleNetCents)
            ?: return RedeemLoyaltyResult.NoActiveConfig

        if (redeemable.redeemableCents <= 0) return RedeemLoyaltyResult.NothingToRedeem

        val type = if (config.mode == LoyaltyMode.POINTS_PER_VALUE) {
            LoyaltyTransactionType.REDEEMED
        } else {
            LoyaltyTransactionType.REWARD_REDEEMED
        }
        val points = if (config.mode == LoyaltyMode.POINTS_PER_VALUE) redeemable.pointsToConsume else null

        loyaltyTransactionRepository.recordTransaction(
            customerId = customerId,
            saleId = sale.id,
            mode = config.mode,
            type = type,
            points = points,
            amountCents = redeemable.redeemableCents
        )
        saleRepository.setLoyaltyDiscount(sale.id, redeemable.redeemableCents)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            val action = if (config.mode == LoyaltyMode.POINTS_PER_VALUE) {
                AuditAction.LOYALTY_POINTS_REDEEMED
            } else {
                AuditAction.LOYALTY_REWARD_REDEEMED
            }
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = action,
                    detail = "Cliente #$customerId; Valor: ${redeemable.redeemableCents} centavos",
                    success = true
                )
            )
        }
        return RedeemLoyaltyResult.Success(redeemable.redeemableCents)
    }
}
