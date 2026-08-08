package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pdvmaquineta.data.local.database.entity.SaleOutboxEntity

@Dao
interface SaleOutboxDao {

    // IGNORE: o índice único em saleId barra reentrada da mesma venda; se já
    // existe, insere nada (retorna -1) e o payload/UUID original é preservado.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: SaleOutboxEntity): Long

    @Query("SELECT * FROM sale_outbox WHERE saleId = :saleId LIMIT 1")
    suspend fun findBySaleId(saleId: Long): SaleOutboxEntity?

    @Query(
        "SELECT * FROM sale_outbox WHERE status = 'PENDING' " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun pending(limit: Int): List<SaleOutboxEntity>

    @Query("SELECT COUNT(*) FROM sale_outbox WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    @Query(
        "UPDATE sale_outbox SET status = 'SENT', lastError = NULL, " +
            "updatedAt = :now WHERE id = :id"
    )
    suspend fun markSent(id: Long, now: Long)

    @Query(
        "UPDATE sale_outbox SET attempts = attempts + 1, lastError = :error, " +
            "updatedAt = :now WHERE id = :id"
    )
    suspend fun markFailed(id: Long, error: String?, now: Long)

    // Vendas concluídas que ainda não entraram na fila (ex.: concluídas antes
    // desta funcionalidade existir, ou perdidas por um crash entre o
    // completeSale e o enqueue). Usado na reconciliação no boot.
    @Query(
        "SELECT s.id FROM sales s WHERE s.status = 'COMPLETED' " +
            "AND s.id NOT IN (SELECT saleId FROM sale_outbox)"
    )
    suspend fun completedSalesNotQueued(): List<Long>
}
