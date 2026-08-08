package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.ProductRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// Baixa automática de estoque (RF-035), disparada no mesmo gancho já usado
// pra fidelidade: dentro de ProcessPaymentUseCase, só quando o pagamento é
// aprovado. Decrementa incondicionalmente pela quantidade vendida — mesmo
// pra produtos com allowSaleWithoutStock=true, o que pode deixar
// stockQuantity negativo (estoque "a repor"), de propósito.
class DecrementStockForSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(saleId: Long) {
        val items = saleRepository.observeItems(saleId).first()
        for (item in items) {
            productRepository.decrementStock(item.productId, item.quantity)
        }
    }
}
