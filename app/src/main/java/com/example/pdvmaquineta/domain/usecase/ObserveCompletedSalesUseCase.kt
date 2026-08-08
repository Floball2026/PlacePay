package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCompletedSalesUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    operator fun invoke(fromMillis: Long? = null, toMillis: Long? = null): Flow<List<Sale>> =
        saleRepository.observeCompletedSales(fromMillis, toMillis)
}
