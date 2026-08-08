package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE cashSessionId = :cashSessionId AND status = 'OPEN' LIMIT 1")
    fun observeOpenSale(cashSessionId: Long): Flow<SaleEntity?>

    @Query(
        "SELECT * FROM sales WHERE cashSessionId = :cashSessionId AND status = 'SUSPENDED' " +
            "ORDER BY updatedAt DESC"
    )
    fun observeSuspendedSales(cashSessionId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun findById(id: Long): SaleEntity?

    @Query(
        "SELECT * FROM sales WHERE status = 'COMPLETED' " +
            "AND (:fromMillis IS NULL OR createdAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR createdAt <= :toMillis) " +
            "ORDER BY createdAt DESC"
    )
    fun observeCompletedSales(fromMillis: Long?, toMillis: Long?): Flow<List<SaleEntity>>

    @Query(
        "SELECT COUNT(*) FROM sales WHERE status = 'COMPLETED' " +
            "AND (:fromMillis IS NULL OR createdAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR createdAt <= :toMillis)"
    )
    suspend fun countCompletedInRange(fromMillis: Long?, toMillis: Long?): Int

    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    @Delete
    suspend fun delete(sale: SaleEntity)
}
