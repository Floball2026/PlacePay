package com.example.pdvmaquineta.domain.loyalty

import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode

// A config ativa muda em runtime (admin troca de modo), então o engine não dá
// pra injetar fixo via Hilt como o PaymentGateway — é montado a partir da
// LoyaltyConfig do momento.
object LoyaltyEngineFactory {
    fun create(config: LoyaltyConfig): LoyaltyEngine = when (config.mode) {
        LoyaltyMode.POINTS_PER_VALUE -> PointsPerValueEngine(
            pointsPerCurrencyUnit = config.pointsPerCurrencyUnit ?: 0.0,
            pointValueInCents = config.pointValueInCents ?: 0
        )
        LoyaltyMode.VISIT_COUNT_DISCOUNT -> VisitCountDiscountEngine(
            visitsRequired = config.visitsRequired ?: 0,
            discountPercentOnReward = config.discountPercentOnReward ?: 0
        )
    }
}
