package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject

class SuspendSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: Long) {
        saleRepository.suspendSale(saleId)
    }
}
