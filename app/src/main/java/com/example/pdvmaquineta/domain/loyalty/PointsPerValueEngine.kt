package com.example.pdvmaquineta.domain.loyalty

import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import kotlin.math.floor

class PointsPerValueEngine(
    private val pointsPerCurrencyUnit: Double,
    private val pointValueInCents: Long
) : LoyaltyEngine {

    override fun calculateEarn(saleTotalCents: Long): LoyaltyEarnResult {
        val points = floor(saleTotalCents / 100.0 * pointsPerCurrencyUnit).toInt()
        return LoyaltyEarnResult(type = LoyaltyTransactionType.EARNED, points = points)
    }

    override fun calculateRedeemable(
        transactions: List<LoyaltyTransaction>,
        saleNetCents: Long
    ): LoyaltyRedeemable {
        val earned = transactions.filter { it.type == LoyaltyTransactionType.EARNED }.sumOf { it.points ?: 0 }
        val redeemed = transactions.filter { it.type == LoyaltyTransactionType.REDEEMED }.sumOf { it.points ?: 0 }
        val reversed = transactions.filter { it.type == LoyaltyTransactionType.REDEMPTION_REVERSED }
            .sumOf { it.points ?: 0 }
        val balance = (earned - redeemed + reversed).coerceAtLeast(0)

        // Resgate parcial automático: só consome os pontos (inteiros)
        // necessários pra cobrir o que falta pagar, nunca o saldo inteiro —
        // o resto continua disponível pra uso futuro.
        val pointsToConsume = if (pointValueInCents > 0) {
            (saleNetCents / pointValueInCents).toInt().coerceIn(0, balance)
        } else {
            0
        }
        val redeemableCents = pointsToConsume.toLong() * pointValueInCents

        return LoyaltyRedeemable(
            redeemableCents = redeemableCents,
            pointsAvailable = balance,
            pointsToConsume = pointsToConsume
        )
    }
}
