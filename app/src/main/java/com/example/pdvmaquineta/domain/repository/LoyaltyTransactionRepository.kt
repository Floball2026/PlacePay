package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import kotlinx.coroutines.flow.Flow

interface LoyaltyTransactionRepository {
    suspend fun recordTransaction(
        customerId: Long,
        saleId: Long?,
        mode: LoyaltyMode,
        type: LoyaltyTransactionType,
        points: Int?,
        amountCents: Long?
    ): LoyaltyTransaction

    suspend fun findForCustomerSince(customerId: Long, sinceMillis: Long): List<LoyaltyTransaction>
    fun observeForCustomer(customerId: Long): Flow<List<LoyaltyTransaction>>

    // Última tentativa de resgate (REDEEMED ou REWARD_REDEEMED) feita nesta
    // venda — usado pelo "Desfazer resgate" pra saber o que estornar.
    suspend fun findLatestRedemptionForSale(saleId: Long): LoyaltyTransaction?
}
