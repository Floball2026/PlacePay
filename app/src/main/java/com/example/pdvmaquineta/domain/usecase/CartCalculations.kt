package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem

// Compartilhado entre ObserveCartUseCase (carrinho reativo, venda OPEN) e
// GetSaleReceiptDataUseCase (venda já concluída, lida uma vez do histórico) —
// mesma matemática de totais, uma única fonte de verdade.
fun buildCartOverview(sale: Sale, items: List<SaleItem>): CartOverview {
    val subtotal = items.sumOf { it.unitPriceCents * it.quantity }
    val discount = subtotal * sale.discountPercent / 100
    val netBeforeLoyalty = (subtotal - discount).coerceAtLeast(0)
    val total = (netBeforeLoyalty - sale.loyaltyDiscountCents).coerceAtLeast(0)
    return CartOverview(
        sale = sale,
        items = items,
        subtotalCents = subtotal,
        discountCents = discount,
        netBeforeLoyaltyCents = netBeforeLoyalty,
        loyaltyDiscountCents = sale.loyaltyDiscountCents,
        totalCents = total,
        itemCount = items.sumOf { it.quantity }
    )
}
