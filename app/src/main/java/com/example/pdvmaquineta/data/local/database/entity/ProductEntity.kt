package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index("active"), Index(value = ["barcode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val priceCents: Long,
    val category: String?,
    val active: Boolean = true,
    val stockQuantity: Int = 0,
    val minStockAlert: Int? = null,
    val allowSaleWithoutStock: Boolean = false,
    // Opcional (decisão de negócio) — índice único permite vários produtos
    // sem código (NULL não conflita em índice único no SQLite), mas impede
    // duplicidade entre os que têm (RF-009/010).
    val barcode: String? = null,
    val imagePath: String? = null,
    val remoteId: String? = null,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
