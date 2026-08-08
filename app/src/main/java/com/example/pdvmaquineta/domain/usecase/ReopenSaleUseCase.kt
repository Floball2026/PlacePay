package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject

// "Voltar ao carrinho" a partir da tela de pagamento: desfaz o AWAITING_PAYMENT
// e deixa a venda editável de novo.
class ReopenSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: Long) {
        saleRepository.reopenSale(saleId)
    }
}
