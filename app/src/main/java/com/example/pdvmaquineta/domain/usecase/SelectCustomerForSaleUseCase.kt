package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

// Mesma lógica de "criar o carrinho na hora" do AddProductToCartUseCase —
// selecionar um cliente antes de adicionar qualquer item também precisa de
// uma venda OPEN pra existir, já que customerId vive na linha da venda.
class SelectCustomerForSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(cashSessionId: Long, customerId: Long) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val sale = saleRepository.getOrCreateOpenSale(cashSessionId, user)
        saleRepository.setCustomer(sale.id, customerId)
    }
}
