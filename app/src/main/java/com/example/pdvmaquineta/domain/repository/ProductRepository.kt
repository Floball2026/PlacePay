package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeAll(): Flow<List<Product>>
    fun observeActive(query: String): Flow<List<Product>>
    suspend fun getProduct(id: Long): Product?
    suspend fun createProduct(
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ): Product

    suspend fun updateProduct(
        id: Long,
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ): Product

    suspend fun setActive(id: Long, active: Boolean)

    // Busca exata usada pra checagem de duplicidade (RF-009) e pra resolver
    // um código lido por câmera/leitor físico na tela de venda (RF-010/011).
    suspend fun findByBarcode(barcode: String): Product?

    // Baixa automática de estoque na venda (RF-035) — pode deixar
    // stockQuantity negativo se o produto permitir venda sem estoque.
    suspend fun decrementStock(id: Long, quantity: Int)

    // Estado atual (não histórico) — usado no relatório (Fase 7b).
    suspend fun getLowStockProducts(): List<Product>
}
