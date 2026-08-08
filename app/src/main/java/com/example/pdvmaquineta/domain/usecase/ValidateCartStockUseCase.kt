package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.SaleItem
import com.example.pdvmaquineta.domain.repository.ProductRepository
import javax.inject.Inject

sealed class ValidateCartStockResult {
    data object Ok : ValidateCartStockResult()
    data class InsufficientStock(val productNames: List<String>) : ValidateCartStockResult()
}

// Chamado na finalização da venda (RF-035), antes mesmo de chegar à tela de
// pagamento: produtos com allowSaleWithoutStock=false e estoque insuficiente
// bloqueiam a venda aqui, não só depois do pagamento aprovado.
class ValidateCartStockUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(items: List<SaleItem>): ValidateCartStockResult {
        val insufficient = mutableListOf<String>()
        for (item in items) {
            val product = productRepository.getProduct(item.productId) ?: continue
            if (!product.allowSaleWithoutStock && product.stockQuantity < item.quantity) {
                insufficient += product.name
            }
        }
        return if (insufficient.isEmpty()) {
            ValidateCartStockResult.Ok
        } else {
            ValidateCartStockResult.InsufficientStock(insufficient)
        }
    }
}
