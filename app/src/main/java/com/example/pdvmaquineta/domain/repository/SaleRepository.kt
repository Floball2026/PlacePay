package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.model.User
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun observeOpenSale(cashSessionId: Long): Flow<Sale?>
    fun observeSuspendedSales(cashSessionId: Long): Flow<List<Sale>>
    fun observeItems(saleId: Long): Flow<List<SaleItem>>

    suspend fun findById(saleId: Long): Sale?
    suspend fun getOrCreateOpenSale(cashSessionId: Long, operator: User): Sale
    suspend fun addItem(saleId: Long, product: Product)
    suspend fun changeItemQuantity(saleId: Long, productId: Long, delta: Int)
    suspend fun changeItemPrice(saleId: Long, productId: Long, newUnitPriceCents: Long)
    suspend fun removeItem(saleId: Long, productId: Long)
    suspend fun setDiscount(saleId: Long, percent: Int)
    suspend fun setCustomer(saleId: Long, customerId: Long)
    suspend fun clearCustomer(saleId: Long)
    suspend fun setLoyaltyDiscount(saleId: Long, cents: Long)
    suspend fun suspendSale(saleId: Long)
    suspend fun resumeSale(saleId: Long)
    suspend fun cancelSale(saleId: Long, reason: String)
    suspend fun finalizeSale(saleId: Long): Sale
    suspend fun completeSale(saleId: Long)
    suspend fun reopenSale(saleId: Long)
    suspend fun setTerminalSnapshot(saleId: Long, terminalName: String, storeName: String)

    // fromMillis/toMillis nulos = sem filtro de data (histórico completo).
    fun observeCompletedSales(fromMillis: Long?, toMillis: Long?): Flow<List<Sale>>

    // Usados no relatório (Fase 7b) — mesma convenção de fromMillis/toMillis
    // nulos = sem filtro.
    suspend fun countCompletedSales(fromMillis: Long?, toMillis: Long?): Int
    suspend fun getTopSellingProducts(fromMillis: Long?, toMillis: Long?, limit: Int): List<TopProduct>
}
