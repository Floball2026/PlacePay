package com.example.pdvmaquineta.domain.model

enum class CashSessionStatus {
    OPEN,
    CLOSED
}

data class CashSession(
    val id: Long,
    val operatorId: Long,
    val operatorUsername: String,
    val openingBalanceCents: Long,
    val openedAt: Long,
    val closedAt: Long?,
    val expectedCashCents: Long?,
    val informedCashCents: Long?,
    val divergenceCents: Long?,
    val status: CashSessionStatus
)
