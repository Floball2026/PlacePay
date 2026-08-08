package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.CashSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM cash_sessions WHERE status = 'OPEN' LIMIT 1")
    fun observeOpenSession(): Flow<CashSessionEntity?>

    @Query("SELECT * FROM cash_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CashSessionEntity?

    // Usado no relatório (Fase 7b) — só sessões já fechadas (com
    // divergência/totais definitivos), filtradas por closedAt no período
    // (nulo = sem filtro).
    @Query(
        "SELECT * FROM cash_sessions WHERE status = 'CLOSED' " +
            "AND (:fromMillis IS NULL OR closedAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR closedAt <= :toMillis) " +
            "ORDER BY closedAt DESC"
    )
    suspend fun findClosedInRange(fromMillis: Long?, toMillis: Long?): List<CashSessionEntity>

    @Insert
    suspend fun insert(session: CashSessionEntity): Long

    @Update
    suspend fun update(session: CashSessionEntity)
}
