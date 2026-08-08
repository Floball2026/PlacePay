package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyConfigDao {
    @Query("SELECT * FROM loyalty_configs WHERE deactivatedAt IS NULL LIMIT 1")
    suspend fun findActive(): LoyaltyConfigEntity?

    @Query("SELECT * FROM loyalty_configs WHERE deactivatedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<LoyaltyConfigEntity?>

    @Insert
    suspend fun insert(config: LoyaltyConfigEntity): Long

    @Update
    suspend fun update(config: LoyaltyConfigEntity)
}
