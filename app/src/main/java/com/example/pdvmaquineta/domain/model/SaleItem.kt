package com.example.pdvmaquineta.domain.model

// productName/unitPriceCents são um retrato do produto no momento em que foi
// adicionado à venda, não uma referência viva — editar o preço de um produto
// depois não pode alterar o total de vendas já registradas.
data class SaleItem(
    val id: Long,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitPriceCents: Long,
    val quantity: Int
)
