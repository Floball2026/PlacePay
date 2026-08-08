package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.LoyaltyTransactionDao
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyTransactionEntity
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoyaltyTransactionRepositoryImpl @Inject constructor(
    private val loyaltyTransactionDao: LoyaltyTransactionDao
) : LoyaltyTransactionRepository {

    override suspend fun recordTransaction(
        customerId: Long,
        saleId: Long?,
        mode: LoyaltyMode,
        type: LoyaltyTransactionType,
        points: Int?,
        amountCents: Long?
    ): LoyaltyTransaction {
        val entity = LoyaltyTransactionEntity(
            customerId = customerId,
            saleId = saleId,
            mode = mode.name,
            type = type.name,
            points = points,
            amountCents = amountCents,
            timestamp = System.currentTimeMillis()
        )
        val id = loyaltyTransactionDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun findForCustomerSince(customerId: Long, sinceMillis: Long): List<LoyaltyTransaction> =
        loyaltyTransactionDao.findForCustomerSince(customerId, sinceMillis).map { it.toDomain() }

    override fun observeForCustomer(customerId: Long): Flow<List<LoyaltyTransaction>> =
        loyaltyTransactionDao.observeForCustomer(customerId).map { list -> list.map { it.toDomain() } }

    override suspend fun findLatestRedemptionForSale(saleId: Long): LoyaltyTransaction? =
        loyaltyTransactionDao.findLatestRedemptionForSale(saleId)?.toDomain()

    private fun LoyaltyTransactionEntity.toDomain() = LoyaltyTransaction(
        id = id,
        customerId = customerId,
        saleId = saleId,
        mode = LoyaltyMode.valueOf(mode),
        type = LoyaltyTransactionType.valueOf(type),
        points = points,
        amountCents = amountCents,
        timestamp = timestamp
    )
}
