package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val userId: Long?,
    val username: String,
    val action: String,
    val detail: String?,
    val success: Boolean
)
