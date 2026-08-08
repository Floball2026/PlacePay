package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.CashMovementDao
import com.example.pdvmaquineta.data.local.database.dao.CashSessionDao
import com.example.pdvmaquineta.data.local.database.entity.CashMovementEntity
import com.example.pdvmaquineta.data.local.database.entity.CashSessionEntity
import com.example.pdvmaquineta.domain.model.CashMovement
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.CashSessionStatus
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.repository.CashMovementTotals
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CashSessionRepositoryImpl @Inject constructor(
    private val cashSessionDao: CashSessionDao,
    private val cashMovementDao: CashMovementDao
) : CashSessionRepository {

    override fun observeOpenSession(): Flow<CashSession?> =
        cashSessionDao.observeOpenSession().map { it?.toDomain() }

    override suspend fun openSession(operator: User, openingBalanceCents: Long): CashSession {
        val entity = CashSessionEntity(
            operatorId = operator.id,
            operatorUsername = operator.username,
            openingBalanceCents = openingBalanceCents,
            openedAt = System.currentTimeMillis(),
            status = CashSessionStatus.OPEN.name
        )
        val id = cashSessionDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun closeSession(
        sessionId: Long,
        expectedCashCents: Long,
        informedCashCents: Long,
        divergenceCents: Long
    ): CashSession {
        val entity = cashSessionDao.findById(sessionId)
            ?: error("Sessão de caixa $sessionId não encontrada")
        val closed = entity.copy(
            closedAt = System.currentTimeMillis(),
            expectedCashCents = expectedCashCents,
            informedCashCents = informedCashCents,
            divergenceCents = divergenceCents,
            status = CashSessionStatus.CLOSED.name
        )
        cashSessionDao.update(closed)
        return closed.toDomain()
    }

    override suspend fun addMovement(
        sessionId: Long,
        type: CashMovementType,
        amountCents: Long,
        reason: String,
        operator: User,
        authorizedByUsername: String?
    ): CashMovement {
        val entity = CashMovementEntity(
            cashSessionId = sessionId,
            type = type.name,
            amountCents = amountCents,
            reason = reason,
            operatorId = operator.id,
            operatorUsername = operator.username,
            authorizedByUsername = authorizedByUsername,
            createdAt = System.currentTimeMillis()
        )
        val id = cashMovementDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun getMovementTotals(sessionId: Long): CashMovementTotals = CashMovementTotals(
        withdrawalCents = cashMovementDao.sumByType(sessionId, CashMovementType.WITHDRAWAL.name),
        supplyCents = cashMovementDao.sumByType(sessionId, CashMovementType.SUPPLY.name)
    )

    override suspend fun getClosedSessionsInRange(fromMillis: Long?, toMillis: Long?): List<CashSession> =
        cashSessionDao.findClosedInRange(fromMillis, toMillis).map { it.toDomain() }

    override suspend fun getMovementTotalsInRange(fromMillis: Long?, toMillis: Long?): CashMovementTotals =
        CashMovementTotals(
            withdrawalCents = cashMovementDao.sumByTypeInRange(
                CashMovementType.WITHDRAWAL.name, fromMillis, toMillis
            ),
            supplyCents = cashMovementDao.sumByTypeInRange(CashMovementType.SUPPLY.name, fromMillis, toMillis)
        )

    private fun CashSessionEntity.toDomain() = CashSession(
        id = id,
        operatorId = operatorId,
        operatorUsername = operatorUsername,
        openingBalanceCents = openingBalanceCents,
        openedAt = openedAt,
        closedAt = closedAt,
        expectedCashCents = expectedCashCents,
        informedCashCents = informedCashCents,
        divergenceCents = divergenceCents,
        status = CashSessionStatus.valueOf(status)
    )

    private fun CashMovementEntity.toDomain() = CashMovement(
        id = id,
        cashSessionId = cashSessionId,
        type = CashMovementType.valueOf(type),
        amountCents = amountCents,
        reason = reason,
        operatorId = operatorId,
        operatorUsername = operatorUsername,
        authorizedByUsername = authorizedByUsername,
        createdAt = createdAt
    )
}
