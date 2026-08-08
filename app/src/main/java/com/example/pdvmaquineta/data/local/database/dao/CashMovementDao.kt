package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pdvmaquineta.data.local.database.entity.CashMovementEntity

@Dao
interface CashMovementDao {
    @Insert
    suspend fun insert(movement: CashMovementEntity): Long

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM cash_movements " +
            "WHERE cashSessionId = :sessionId AND type = :type"
    )
    suspend fun sumByType(sessionId: Long, type: String): Long

    // Mesma agregação de sumByType, por período em vez de sessão de caixa —
    // usado no relatório (Fase 7b).
    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM cash_movements " +
            "WHERE type = :type " +
            "AND (:fromMillis IS NULL OR createdAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR createdAt <= :toMillis)"
    )
    suspend fun sumByTypeInRange(type: String, fromMillis: Long?, toMillis: Long?): Long
}
