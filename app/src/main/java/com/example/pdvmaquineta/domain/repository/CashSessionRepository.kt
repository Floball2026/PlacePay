package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.CashMovement
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.User
import kotlinx.coroutines.flow.Flow

data class CashMovementTotals(
    val withdrawalCents: Long,
    val supplyCents: Long
)

interface CashSessionRepository {
    fun observeOpenSession(): Flow<CashSession?>

    suspend fun openSession(operator: User, openingBalanceCents: Long): CashSession

    suspend fun closeSession(
        sessionId: Long,
        expectedCashCents: Long,
        informedCashCents: Long,
        divergenceCents: Long
    ): CashSession

    suspend fun addMovement(
        sessionId: Long,
        type: CashMovementType,
        amountCents: Long,
        reason: String,
        operator: User,
        authorizedByUsername: String?
    ): CashMovement

    suspend fun getMovementTotals(sessionId: Long): CashMovementTotals

    // Usados no relatório (Fase 7b) — mesma convenção de fromMillis/toMillis
    // nulos = sem filtro.
    suspend fun getClosedSessionsInRange(fromMillis: Long?, toMillis: Long?): List<CashSession>
    suspend fun getMovementTotalsInRange(fromMillis: Long?, toMillis: Long?): CashMovementTotals
}
