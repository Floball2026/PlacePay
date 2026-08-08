package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val role: String,
    val active: Boolean = true,
    val mustChangePin: Boolean = false,
    val createdByUserId: Long? = null,
    val themeTone: String = "BRAND_LIGHT",
    val remoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
