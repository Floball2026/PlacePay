package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class CartOverview(
    val sale: Sale,
    val items: List<SaleItem>,
    val subtotalCents: Long,
    val discountCents: Long,
    // O que falta pagar depois do desconto manual, antes do desconto de
    // fidelidade — é contra isso (não o subtotal bruto) que o resgate de
    // fidelidade deve ser calculado e limitado.
    val netBeforeLoyaltyCents: Long,
    val loyaltyDiscountCents: Long,
    val totalCents: Long,
    val itemCount: Int
)

class ObserveCartUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(cashSessionId: Long): Flow<CartOverview?> =
        saleRepository.observeOpenSale(cashSessionId).flatMapLatest { sale ->
            if (sale == null) {
                flowOf(null)
            } else {
                saleRepository.observeItems(sale.id).map { items -> buildCartOverview(sale, items) }
            }
        }
}
