package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM products WHERE active = 1 AND " +
            "(name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' " +
            "OR barcode LIKE '%' || :query || '%') ORDER BY name"
    )
    fun observeActive(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findById(id: Long): ProductEntity?

    // Busca exata (não LIKE) usada pra checagem de duplicidade no cadastro e
    // pra resolução direta de um código lido por câmera/leitor físico.
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): ProductEntity?

    // Estado atual (não histórico) — usado no relatório (Fase 7b).
    @Query("SELECT * FROM products WHERE minStockAlert IS NOT NULL AND stockQuantity <= minStockAlert ORDER BY name")
    suspend fun findLowStock(): List<ProductEntity>

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)
}
