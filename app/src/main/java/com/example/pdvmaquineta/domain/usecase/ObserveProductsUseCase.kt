package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.repository.ProductRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = productRepository.observeAll()
}
