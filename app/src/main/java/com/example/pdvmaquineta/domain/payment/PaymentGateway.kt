package com.example.pdvmaquineta.domain.payment

// Abstração sobre a adquirente. Hoje só existe MockPaymentGateway (Place Pay
// ainda sem SDK/documentação disponível) — trocar pela integração real não
// deve exigir mudança nenhuma em caso de uso, ViewModel ou tela, só uma nova
// implementação desta interface.
interface PaymentGateway {
    suspend fun charge(request: PaymentRequest): PaymentResult

    // Reimprime o comprovante da ultima transacao de cartao/PIX (marcado como
    // REIMPRESSAO). Retorna false se nao houver comprovante em memoria.
    suspend fun reprintLastReceipt(): Boolean = false
}
