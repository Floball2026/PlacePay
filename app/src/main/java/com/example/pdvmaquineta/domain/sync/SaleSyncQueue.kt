package com.example.pdvmaquineta.domain.sync

// Porta de dominio para a fila de envio de vendas ao SaaS. Mantida no dominio
// para que o fluxo de pagamento (use case) nao dependa da camada de dados nem
// de detalhes de rede — a implementacao concreta (outbox + Retrofit) fica em
// data/sync.
interface SaleSyncQueue {
    // Enfileira a venda concluida (grava local, nao bloqueia em rede) e agenda
    // um envio em segundo plano. Idempotente por venda.
    suspend fun enqueue(saleId: Long)

    // Tenta enviar tudo que esta pendente. Silencioso quando offline/nao ativado.
    suspend fun flush()

    // Enfileira vendas concluidas que ficaram de fora da fila e entao envia.
    // Chamado no inicio do app.
    suspend fun reconcileAndFlush()
}
