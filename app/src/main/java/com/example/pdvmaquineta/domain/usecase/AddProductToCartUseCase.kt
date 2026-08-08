package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class AddProductToCartUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(cashSessionId: Long, product: Product) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val sale = saleRepository.getOrCreateOpenSale(cashSessionId, user)
        saleRepository.addItem(sale.id, product)
    }
}
