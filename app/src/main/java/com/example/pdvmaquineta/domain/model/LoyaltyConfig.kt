package com.example.pdvmaquineta.domain.model

enum class LoyaltyMode {
    POINTS_PER_VALUE,
    VISIT_COUNT_DISCOUNT
}

// Uma linha por período de vigência: trocar de modo/parâmetros fecha a atual
// (deactivatedAt = agora) e insere uma nova, em vez de sobrescrever — o
// histórico fica congelado e consultável sozinho, sem precisar de flag extra.
data class LoyaltyConfig(
    val id: Long,
    val mode: LoyaltyMode,
    val pointsPerCurrencyUnit: Double?,
    val pointValueInCents: Long?,
    val visitsRequired: Int?,
    val discountPercentOnReward: Int?,
    val activatedAt: Long,
    val deactivatedAt: Long?,
    val changedByUserId: Long?
)
