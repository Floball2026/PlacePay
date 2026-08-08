package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import kotlinx.coroutines.flow.Flow

interface LoyaltyConfigRepository {
    suspend fun getActive(): LoyaltyConfig?
    fun observeActive(): Flow<LoyaltyConfig?>

    // Fecha a config ativa atual (deactivatedAt = agora) e insere a nova como
    // ativa — histórico da config anterior fica congelado, não apagado.
    suspend fun changeConfig(
        mode: LoyaltyMode,
        pointsPerCurrencyUnit: Double?,
        pointValueInCents: Long?,
        visitsRequired: Int?,
        discountPercentOnReward: Int?,
        changedByUserId: Long?
    ): LoyaltyConfig
}
