package com.example.pdvmaquineta.domain.loyalty

import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType

class VisitCountDiscountEngine(
    private val visitsRequired: Int,
    private val discountPercentOnReward: Int
) : LoyaltyEngine {

    override fun calculateEarn(saleTotalCents: Long): LoyaltyEarnResult =
        LoyaltyEarnResult(type = LoyaltyTransactionType.VISIT_COUNTED)

    override fun calculateRedeemable(
        transactions: List<LoyaltyTransaction>,
        saleNetCents: Long
    ): LoyaltyRedeemable {
        val visitsCounted = transactions.count { it.type == LoyaltyTransactionType.VISIT_COUNTED }
        val rewardsRedeemed = transactions.count { it.type == LoyaltyTransactionType.REWARD_REDEEMED }
        val rewardsReversed = transactions.count { it.type == LoyaltyTransactionType.REDEMPTION_REVERSED }
        val netRewardsRedeemed = (rewardsRedeemed - rewardsReversed).coerceAtLeast(0)
        val visitsSinceLastReward = (visitsCounted - netRewardsRedeemed * visitsRequired).coerceAtLeast(0)
        val eligible = visitsRequired > 0 && visitsSinceLastReward >= visitsRequired
        val redeemableCents = if (eligible) {
            (saleNetCents * discountPercentOnReward / 100).coerceAtMost(saleNetCents)
        } else {
            0L
        }
        return LoyaltyRedeemable(
            redeemableCents = redeemableCents,
            visitsCount = visitsSinceLastReward,
            visitsRequired = visitsRequired
        )
    }
}
