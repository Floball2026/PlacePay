package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLoyaltyTransactionsUseCase @Inject constructor(
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository
) {
    operator fun invoke(customerId: Long): Flow<List<LoyaltyTransaction>> =
        loyaltyTransactionRepository.observeForCustomer(customerId)
}
