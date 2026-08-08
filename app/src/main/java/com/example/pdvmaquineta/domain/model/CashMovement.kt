package com.example.pdvmaquineta.domain.model

enum class CashMovementType {
    WITHDRAWAL,
    SUPPLY
}

data class CashMovement(
    val id: Long,
    val cashSessionId: Long,
    val type: CashMovementType,
    val amountCents: Long,
    val reason: String,
    val operatorId: Long,
    val operatorUsername: String,
    val authorizedByUsername: String?,
    val createdAt: Long
)
