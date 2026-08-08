package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

data class TopProductRow(
    val productId: Long,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenueCents: Long
)

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun observeItems(saleId: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity>

    // Usado no relatório (Fase 7b) — top produtos por quantidade vendida,
    // só em vendas concluídas, no período informado (nulo = sem filtro).
    @Query(
        "SELECT si.productId as productId, si.productName as productName, " +
            "SUM(si.quantity) as totalQuantity, " +
            "SUM(si.unitPriceCents * si.quantity) as totalRevenueCents " +
            "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
            "WHERE s.status = 'COMPLETED' " +
            "AND (:fromMillis IS NULL OR s.createdAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR s.createdAt <= :toMillis) " +
            "GROUP BY si.productId " +
            "ORDER BY totalQuantity DESC " +
            "LIMIT :limit"
    )
    suspend fun getTopSellingProducts(fromMillis: Long?, toMillis: Long?, limit: Int): List<TopProductRow>

    @Query("SELECT COUNT(*) FROM sale_items WHERE saleId = :saleId")
    suspend fun countItems(saleId: Long): Int

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND productId = :productId LIMIT 1")
    suspend fun findItem(saleId: Long, productId: Long): SaleItemEntity?

    @Insert
    suspend fun insert(item: SaleItemEntity): Long

    @Update
    suspend fun update(item: SaleItemEntity)

    @Delete
    suspend fun delete(item: SaleItemEntity)
}
