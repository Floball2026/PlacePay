package com.example.pdvmaquineta.domain.model

data class TopProduct(
    val productId: Long,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenueCents: Long
)
