package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed class ResumeSaleResult {
    data object Success : ResumeSaleResult()
    data object CurrentCartNotEmpty : ResumeSaleResult()
}

// Só permite retomar uma venda suspensa se o carrinho atual estiver vazio
// (nenhuma venda OPEN existente) — evita perder itens de uma venda em
// andamento ao trocar pra outra sem uma lógica de mesclagem.
class ResumeSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(cashSessionId: Long, saleId: Long): ResumeSaleResult {
        val currentOpen = saleRepository.observeOpenSale(cashSessionId).first()
        if (currentOpen != null) {
            return ResumeSaleResult.CurrentCartNotEmpty
        }
        saleRepository.resumeSale(saleId)
        return ResumeSaleResult.Success
    }
}
