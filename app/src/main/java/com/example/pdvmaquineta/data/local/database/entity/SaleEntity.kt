package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = CashSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cashSessionId"), Index("status")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashSessionId: Long,
    val operatorId: Long,
    val operatorUsername: String,
    val status: String,
    val customerId: Long? = null,
    val discountPercent: Int = 0,
    val loyaltyDiscountCents: Long = 0,
    val cancellationReason: String? = null,
    val terminalNameSnapshot: String? = null,
    val storeNameSnapshot: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
