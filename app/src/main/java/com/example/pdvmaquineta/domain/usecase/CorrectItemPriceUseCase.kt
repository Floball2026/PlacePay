package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject

class CorrectItemPriceUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: Long, productId: Long, newUnitPriceCents: Long) {
        saleRepository.changeItemPrice(saleId, productId, newUnitPriceCents)
    }
}
