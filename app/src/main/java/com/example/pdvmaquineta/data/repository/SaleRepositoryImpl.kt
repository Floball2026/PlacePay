package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.SaleDao
import com.example.pdvmaquineta.data.local.database.dao.SaleItemDao
import com.example.pdvmaquineta.data.local.database.entity.SaleEntity
import com.example.pdvmaquineta.data.local.database.entity.SaleItemEntity
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem
import com.example.pdvmaquineta.domain.model.SaleStatus
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.repository.SaleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao
) : SaleRepository {

    override fun observeOpenSale(cashSessionId: Long): Flow<Sale?> =
        saleDao.observeOpenSale(cashSessionId).map { it?.toDomain() }

    override fun observeSuspendedSales(cashSessionId: Long): Flow<List<Sale>> =
        saleDao.observeSuspendedSales(cashSessionId).map { list -> list.map { it.toDomain() } }

    override fun observeItems(saleId: Long): Flow<List<SaleItem>> =
        saleItemDao.observeItems(saleId).map { list -> list.map { it.toDomain() } }

    override suspend fun findById(saleId: Long): Sale? = saleDao.findById(saleId)?.toDomain()

    override suspend fun getOrCreateOpenSale(cashSessionId: Long, operator: User): Sale {
        val existing = saleDao.observeOpenSale(cashSessionId).first()
        if (existing != null) return existing.toDomain()

        val now = System.currentTimeMillis()
        val entity = SaleEntity(
            cashSessionId = cashSessionId,
            operatorId = operator.id,
            operatorUsername = operator.username,
            status = SaleStatus.OPEN.name,
            createdAt = now,
            updatedAt = now
        )
        val id = saleDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun addItem(saleId: Long, product: Product) {
        val existingItem = saleItemDao.findItem(saleId, product.id)
        if (existingItem != null) {
            saleItemDao.update(existingItem.copy(quantity = existingItem.quantity + 1))
        } else {
            saleItemDao.insert(
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productName = product.name,
                    unitPriceCents = product.priceCents,
                    quantity = 1
                )
            )
        }
        touchSale(saleId)
    }

    override suspend fun changeItemQuantity(saleId: Long, productId: Long, delta: Int) {
        val item = saleItemDao.findItem(saleId, productId) ?: return
        val newQuantity = item.quantity + delta
        if (newQuantity <= 0) {
            saleItemDao.delete(item)
        } else {
            saleItemDao.update(item.copy(quantity = newQuantity))
        }
        touchSale(saleId)
        deleteSaleIfEmpty(saleId)
    }

    override suspend fun changeItemPrice(saleId: Long, productId: Long, newUnitPriceCents: Long) {
        if (newUnitPriceCents < 0) return
        val item = saleItemDao.findItem(saleId, productId) ?: return
        saleItemDao.update(item.copy(unitPriceCents = newUnitPriceCents))
        touchSale(saleId)
    }

    override suspend fun removeItem(saleId: Long, productId: Long) {
        val item = saleItemDao.findItem(saleId, productId) ?: return
        saleItemDao.delete(item)
        touchSale(saleId)
        deleteSaleIfEmpty(saleId)
    }

    override suspend fun setDiscount(saleId: Long, percent: Int) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(discountPercent = percent, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setCustomer(saleId: Long, customerId: Long) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(customerId = customerId, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun clearCustomer(saleId: Long) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(customerId = null, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setLoyaltyDiscount(saleId: Long, cents: Long) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(loyaltyDiscountCents = cents, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun suspendSale(saleId: Long) {
        updateStatus(saleId, SaleStatus.SUSPENDED)
    }

    override suspend fun resumeSale(saleId: Long) {
        updateStatus(saleId, SaleStatus.OPEN)
    }

    override suspend fun cancelSale(saleId: Long, reason: String) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(
            sale.copy(
                status = SaleStatus.CANCELLED.name,
                cancellationReason = reason,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun finalizeSale(saleId: Long): Sale {
        val sale = saleDao.findById(saleId) ?: error("Venda $saleId não encontrada")
        val updated = sale.copy(status = SaleStatus.AWAITING_PAYMENT.name, updatedAt = System.currentTimeMillis())
        saleDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun completeSale(saleId: Long) {
        updateStatus(saleId, SaleStatus.COMPLETED)
    }

    override suspend fun reopenSale(saleId: Long) {
        updateStatus(saleId, SaleStatus.OPEN)
    }

    override suspend fun setTerminalSnapshot(saleId: Long, terminalName: String, storeName: String) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(
            sale.copy(
                terminalNameSnapshot = terminalName,
                storeNameSnapshot = storeName,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override fun observeCompletedSales(fromMillis: Long?, toMillis: Long?): Flow<List<Sale>> =
        saleDao.observeCompletedSales(fromMillis, toMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun countCompletedSales(fromMillis: Long?, toMillis: Long?): Int =
        saleDao.countCompletedInRange(fromMillis, toMillis)

    override suspend fun getTopSellingProducts(fromMillis: Long?, toMillis: Long?, limit: Int): List<TopProduct> =
        saleItemDao.getTopSellingProducts(fromMillis, toMillis, limit).map {
            TopProduct(
                productId = it.productId,
                productName = it.productName,
                totalQuantity = it.totalQuantity,
                totalRevenueCents = it.totalRevenueCents
            )
        }

    private suspend fun updateStatus(saleId: Long, status: SaleStatus) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(status = status.name, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun touchSale(saleId: Long) {
        val sale = saleDao.findById(saleId) ?: return
        saleDao.update(sale.copy(updatedAt = System.currentTimeMillis()))
    }

    // Um carrinho OPEN esvaziado item a item não deve ficar como um registro
    // "fantasma" bloqueando a retomada de outras vendas suspensas.
    private suspend fun deleteSaleIfEmpty(saleId: Long) {
        val sale = saleDao.findById(saleId) ?: return
        if (sale.status == SaleStatus.OPEN.name && saleItemDao.countItems(saleId) == 0) {
            saleDao.delete(sale)
        }
    }

    private fun SaleEntity.toDomain() = Sale(
        id = id,
        cashSessionId = cashSessionId,
        operatorId = operatorId,
        operatorUsername = operatorUsername,
        status = SaleStatus.valueOf(status),
        customerId = customerId,
        discountPercent = discountPercent,
        loyaltyDiscountCents = loyaltyDiscountCents,
        cancellationReason = cancellationReason,
        terminalNameSnapshot = terminalNameSnapshot,
        storeNameSnapshot = storeNameSnapshot,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SaleItemEntity.toDomain() = SaleItem(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName,
        unitPriceCents = unitPriceCents,
        quantity = quantity
    )
}
