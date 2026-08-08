package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.ProductDao
import com.example.pdvmaquineta.data.local.database.entity.ProductEntity
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.repository.ProductRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun observeAll(): Flow<List<Product>> =
        productDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(query: String): Flow<List<Product>> =
        productDao.observeActive(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getProduct(id: Long): Product? = productDao.findById(id)?.toDomain()

    override suspend fun createProduct(
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ): Product {
        val now = System.currentTimeMillis()
        val entity = ProductEntity(
            name = name,
            priceCents = priceCents,
            category = category,
            stockQuantity = stockQuantity,
            minStockAlert = minStockAlert,
            allowSaleWithoutStock = allowSaleWithoutStock,
            barcode = barcode,
            imagePath = imagePath,
            createdAt = now,
            updatedAt = now
        )
        val id = productDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun updateProduct(
        id: Long,
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ): Product {
        val existing = productDao.findById(id) ?: error("Produto $id não encontrado")
        val updated = existing.copy(
            name = name,
            priceCents = priceCents,
            category = category,
            stockQuantity = stockQuantity,
            minStockAlert = minStockAlert,
            allowSaleWithoutStock = allowSaleWithoutStock,
            barcode = barcode,
            imagePath = imagePath,
            updatedAt = System.currentTimeMillis()
        )
        productDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        val existing = productDao.findById(id) ?: return
        productDao.update(existing.copy(active = active, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun decrementStock(id: Long, quantity: Int) {
        val existing = productDao.findById(id) ?: return
        productDao.update(
            existing.copy(
                stockQuantity = existing.stockQuantity - quantity,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getLowStockProducts(): List<Product> =
        productDao.findLowStock().map { it.toDomain() }

    override suspend fun findByBarcode(barcode: String): Product? =
        productDao.findByBarcode(barcode)?.toDomain()

    private fun ProductEntity.toDomain() = Product(
        id = id,
        name = name,
        priceCents = priceCents,
        category = category,
        active = active,
        stockQuantity = stockQuantity,
        minStockAlert = minStockAlert,
        allowSaleWithoutStock = allowSaleWithoutStock,
        barcode = barcode,
        imagePath = imagePath,
        remoteId = remoteId,
        imageUrl = imageUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
