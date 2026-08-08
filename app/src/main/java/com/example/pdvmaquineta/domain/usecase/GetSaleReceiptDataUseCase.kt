package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class ReceiptData(val cart: CartOverview, val payment: Payment)

// Reconstrói o mesmo par (CartOverview, Payment) que o fluxo pós-venda em
// memória usa pra alimentar a ReceiptScreen, mas a partir de uma venda já
// concluída no histórico — permite reusar a tela de comprovante sem mudança
// nenhuma para o modo "reimpressão".
class GetSaleReceiptDataUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(saleId: Long): ReceiptData? {
        val sale = saleRepository.findById(saleId) ?: return null
        val items = saleRepository.observeItems(saleId).first()
        val payment = paymentRepository.findApprovedForSale(saleId) ?: return null
        return ReceiptData(cart = buildCartOverview(sale, items), payment = payment)
    }
}
