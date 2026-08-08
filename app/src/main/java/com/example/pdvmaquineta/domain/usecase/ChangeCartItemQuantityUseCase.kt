package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject

class ChangeCartItemQuantityUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: Long, productId: Long, delta: Int) {
        saleRepository.changeItemQuantity(saleId, productId, delta)
    }
}
