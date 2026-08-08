package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.LoyaltyConfigDao
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyConfigEntity
import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoyaltyConfigRepositoryImpl @Inject constructor(
    private val loyaltyConfigDao: LoyaltyConfigDao
) : LoyaltyConfigRepository {

    override suspend fun getActive(): LoyaltyConfig? = loyaltyConfigDao.findActive()?.toDomain()

    override fun observeActive(): Flow<LoyaltyConfig?> = loyaltyConfigDao.observeActive().map { it?.toDomain() }

    override suspend fun changeConfig(
        mode: LoyaltyMode,
        pointsPerCurrencyUnit: Double?,
        pointValueInCents: Long?,
        visitsRequired: Int?,
        discountPercentOnReward: Int?,
        changedByUserId: Long?
    ): LoyaltyConfig {
        val now = System.currentTimeMillis()

        val current = loyaltyConfigDao.findActive()
        if (current != null) {
            loyaltyConfigDao.update(current.copy(deactivatedAt = now))
        }

        val newEntity = LoyaltyConfigEntity(
            mode = mode.name,
            pointsPerCurrencyUnit = pointsPerCurrencyUnit,
            pointValueInCents = pointValueInCents,
            visitsRequired = visitsRequired,
            discountPercentOnReward = discountPercentOnReward,
            activatedAt = now,
            deactivatedAt = null,
            changedByUserId = changedByUserId
        )
        val id = loyaltyConfigDao.insert(newEntity)
        return newEntity.copy(id = id).toDomain()
    }

    private fun LoyaltyConfigEntity.toDomain() = LoyaltyConfig(
        id = id,
        mode = LoyaltyMode.valueOf(mode),
        pointsPerCurrencyUnit = pointsPerCurrencyUnit,
        pointValueInCents = pointValueInCents,
        visitsRequired = visitsRequired,
        discountPercentOnReward = discountPercentOnReward,
        activatedAt = activatedAt,
        deactivatedAt = deactivatedAt,
        changedByUserId = changedByUserId
    )
}
