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
    // Chave imutavel da transacao na adquirente, exigida pelo estorno. Nula em
    // dinheiro e em vendas gravadas antes da v16 do banco — essas nao podem ser
    // estornadas pelo terminal.
    val nsuRequest: String?,
    val declineReason: String?,
    val createdAt: Long
)
