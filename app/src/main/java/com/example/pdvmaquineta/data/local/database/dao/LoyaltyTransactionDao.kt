package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyTransactionDao {
    @Query(
        "SELECT * FROM loyalty_transactions WHERE customerId = :customerId AND timestamp >= :sinceMillis " +
            "ORDER BY timestamp"
    )
    suspend fun findForCustomerSince(customerId: Long, sinceMillis: Long): List<LoyaltyTransactionEntity>

    @Query("SELECT * FROM loyalty_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeForCustomer(customerId: Long): Flow<List<LoyaltyTransactionEntity>>

    @Query(
        "SELECT * FROM loyalty_transactions WHERE saleId = :saleId AND type IN ('REDEEMED', 'REWARD_REDEEMED') " +
            "ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun findLatestRedemptionForSale(saleId: Long): LoyaltyTransactionEntity?

    @Insert
    suspend fun insert(transaction: LoyaltyTransactionEntity): Long
}
