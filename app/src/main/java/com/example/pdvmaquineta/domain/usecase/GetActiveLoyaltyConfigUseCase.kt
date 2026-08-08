package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import javax.inject.Inject

class GetActiveLoyaltyConfigUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository
) {
    suspend operator fun invoke(): LoyaltyConfig? = loyaltyConfigRepository.getActive()
}
