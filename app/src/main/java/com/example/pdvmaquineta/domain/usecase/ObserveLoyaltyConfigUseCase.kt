package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLoyaltyConfigUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository
) {
    operator fun invoke(): Flow<LoyaltyConfig?> = loyaltyConfigRepository.observeActive()
}
