package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_configs")
data class LoyaltyConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val pointsPerCurrencyUnit: Double?,
    val pointValueInCents: Long?,
    val visitsRequired: Int?,
    val discountPercentOnReward: Int?,
    val activatedAt: Long,
    val deactivatedAt: Long?,
    val changedByUserId: Long?
)
