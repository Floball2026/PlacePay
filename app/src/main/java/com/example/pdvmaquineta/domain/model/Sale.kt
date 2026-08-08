package com.example.pdvmaquineta.domain.model

enum class SaleStatus {
    OPEN,
    SUSPENDED,
    AWAITING_PAYMENT,
    COMPLETED,
    CANCELLED
}

data class Sale(
    val id: Long,
    val cashSessionId: Long,
    val operatorId: Long,
    val operatorUsername: String,
    val status: SaleStatus,
    val customerId: Long?,
    val discountPercent: Int,
    val loyaltyDiscountCents: Long,
    val cancellationReason: String?,
    // Congelados no instante da finalização (mesmo princípio do snapshot de
    // preço/nome em SaleItem): se o terminal for reconfigurado depois, o
    // comprovante de uma venda antiga não deve mudar retroativamente.
    val terminalNameSnapshot: String?,
    val storeNameSnapshot: String?,
    val createdAt: Long,
    val updatedAt: Long
)
