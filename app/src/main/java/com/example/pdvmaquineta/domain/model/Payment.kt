package com.example.pdvmaquineta.domain.model

import com.example.pdvmaquineta.domain.payment.PaymentMethod

enum class PaymentStatus {
    APPROVED,
    DECLINED,
    TIMEOUT
}

// Uma linha por tentativa de pagamento (não uma coluna na venda) — permite
// registrar recusa/timeout seguidos de uma nova tentativa aprovada, e deixa
// o schema pronto pra pagamento misto (Fase 4 ainda não implementa isso,
// só não fecha a porta).
data class Payment(
    val id: Long,
    val saleId: Long,
    val method: PaymentMethod,
    val amountCents: Long,
    val receivedCents: Long?,
    val changeCents: Long?,
    val status: PaymentStatus,
    val transactionId: String?,
    val declineReason: String?,
    val createdAt: Long
)
