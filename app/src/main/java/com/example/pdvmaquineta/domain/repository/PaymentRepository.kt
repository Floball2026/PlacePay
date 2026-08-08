package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.PaymentStatus
import com.example.pdvmaquineta.domain.payment.PaymentMethod

interface PaymentRepository {
    suspend fun recordPayment(
        saleId: Long,
        method: PaymentMethod,
        amountCents: Long,
        receivedCents: Long?,
        changeCents: Long?,
        status: PaymentStatus,
        transactionId: String?,
        declineReason: String?
    ): Payment

    // Só pagamentos aprovados, agrupados por forma — usado no fechamento de
    // caixa (Fase 2) pra substituir o placeholder de "total por forma de
    // pagamento" e incluir vendas em dinheiro no saldo esperado.
    suspend fun getApprovedTotalsByMethod(cashSessionId: Long): Map<PaymentMethod, Long>

    // Usado pra reconstruir o comprovante de uma venda do histórico
    // (reimpressão), já que o Payment não fica em memória fora do fluxo
    // pós-venda em que foi originado.
    suspend fun findApprovedForSale(saleId: Long): Payment?

    // Mesma agregação de getApprovedTotalsByMethod, mas por período em vez de
    // sessão de caixa — usado no relatório (Fase 7b). fromMillis/toMillis
    // nulos = sem filtro.
    suspend fun getApprovedTotalsByMethodInRange(fromMillis: Long?, toMillis: Long?): Map<PaymentMethod, Long>
}
