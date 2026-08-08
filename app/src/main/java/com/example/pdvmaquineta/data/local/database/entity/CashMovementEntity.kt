package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = CashSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cashSessionId")]
)
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashSessionId: Long,
    val type: String,
    val amountCents: Long,
    val reason: String,
    val operatorId: Long,
    val operatorUsername: String,
    val authorizedByUsername: String? = null,
    val createdAt: Long
)
