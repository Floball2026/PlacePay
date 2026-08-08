package com.example.pdvmaquineta.domain.model

// Vocabulário completo do RF-004. Fase 1 só dispara LOGIN_*, LOGOUT e
// SESSION_*; as demais entradas existem agora para as próximas fases
// gravarem auditoria sem precisar alterar esse enum ou o schema do Room.
enum class AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    SESSION_LOCKED,
    SESSION_UNLOCKED,
    DISCOUNT_AUTHORIZED,
    SALE_CANCELLED,
    CASH_OPENED,
    CASH_WITHDRAWAL,
    CASH_SUPPLY,
    CASH_CLOSED,
    PRODUCT_CREATED,
    PRODUCT_PRICE_CHANGED,
    PRODUCT_STATUS_CHANGED,
    SALE_FINALIZED,
    PAYMENT_CONFIRMED,
    PAYMENT_DECLINED,
    USER_CREATED,
    USER_ROLE_CHANGED,
    USER_ACTIVATED,
    USER_DEACTIVATED,
    USER_DEACTIVATION_BLOCKED,
    USER_PIN_RESET_FORCED,
    USER_PIN_CHANGED,
    CUSTOMER_CREATED,
    LOYALTY_CONFIG_CHANGED,
    LOYALTY_POINTS_EARNED,
    LOYALTY_VISIT_COUNTED,
    LOYALTY_POINTS_REDEEMED,
    LOYALTY_REWARD_REDEEMED,
    LOYALTY_REDEMPTION_REVERSED,
    RECEIPT_PRINTED,
    RECEIPT_SENT_DIGITALLY,
    TERMINAL_CONFIG_UPDATED,
    STOCK_ADJUSTED,
    REPORT_EXPORTED
}

data class AuditEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val userId: Long?,
    val username: String,
    val action: AuditAction,
    val detail: String? = null,
    val success: Boolean
)
