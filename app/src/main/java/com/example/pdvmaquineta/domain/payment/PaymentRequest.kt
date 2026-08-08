package com.example.pdvmaquineta.domain.payment

data class PaymentRequest(
    val method: PaymentMethod,
    val amountCents: Long,
    // Só relevante pra CASH (valor recebido, pra calcular troco).
    val receivedCents: Long? = null
)
