package com.example.pdvmaquineta.domain.model

enum class LoyaltyTransactionType {
    EARNED,
    REDEEMED,
    VISIT_COUNTED,
    REWARD_REDEEMED,
    // Estorno de um REDEEMED ou REWARD_REDEEMED — não apaga a transação
    // original, só devolve o valor/contagem pro saldo do cliente.
    REDEMPTION_REVERSED
}

data class LoyaltyTransaction(
    val id: Long,
    val customerId: Long,
    val saleId: Long?,
    val mode: LoyaltyMode,
    val type: LoyaltyTransactionType,
    val points: Int?,
    val amountCents: Long?,
    val timestamp: Long
)
