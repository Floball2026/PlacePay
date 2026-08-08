package com.example.pdvmaquineta.domain.receipt

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.usecase.CartOverview

sealed class ReceiptPrintResult {
    data object Success : ReceiptPrintResult()
    data class Failure(val reason: String) : ReceiptPrintResult()
}

// Abstração sobre a impressora do terminal. Hoje só existe MockReceiptPrinter
// (cliente ainda não definiu o modelo/tipo de impressora) — trocar pela
// integração real não deve exigir mudança nenhuma em caso de uso, ViewModel
// ou tela, só uma nova implementação desta interface. Mesmo padrão de
// PaymentGateway.
interface ReceiptPrinter {
    suspend fun print(cart: CartOverview, payment: Payment): ReceiptPrintResult
}
