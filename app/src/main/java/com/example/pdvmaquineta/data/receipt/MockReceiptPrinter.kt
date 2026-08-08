package com.example.pdvmaquineta.data.receipt

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.receipt.ReceiptPrintResult
import com.example.pdvmaquineta.domain.receipt.ReceiptPrinter
import com.example.pdvmaquineta.domain.usecase.CartOverview
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val PRINT_DELAY_MS = 500L

// Sem hardware real ainda (tipo de impressora não definido pelo cliente) —
// só simula sucesso. Quando o modelo de impressora for escolhido, essa
// classe é trocada por uma implementação real de ReceiptPrinter, sem tocar
// em domain/presentation.
class MockReceiptPrinter @Inject constructor() : ReceiptPrinter {
    override suspend fun print(cart: CartOverview, payment: Payment): ReceiptPrintResult {
        delay(PRINT_DELAY_MS)
        return ReceiptPrintResult.Success
    }
}
