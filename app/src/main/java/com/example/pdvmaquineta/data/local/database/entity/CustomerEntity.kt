package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    // Telefone deixou de ser unico (alinhado ao SaaS, onde phone e opcional e
    // nao-unico) — assim clientes sincronizados sem telefone nao colidem.
    indices = [Index(value = ["phone"], unique = false)]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val document: String?,
    val remoteId: String? = null,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
