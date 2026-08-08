package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSuspendedSalesUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    operator fun invoke(cashSessionId: Long): Flow<List<Sale>> =
        saleRepository.observeSuspendedSales(cashSessionId)
}
