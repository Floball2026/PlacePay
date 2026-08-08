package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operatorId: Long,
    val operatorUsername: String,
    val openingBalanceCents: Long,
    val openedAt: Long,
    val closedAt: Long? = null,
    val expectedCashCents: Long? = null,
    val informedCashCents: Long? = null,
    val divergenceCents: Long? = null,
    val status: String
)
