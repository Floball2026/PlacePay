package com.example.pdvmaquineta.domain.payment

/**
 * Dados que a adquirente devolve numa transacao de cartao/PIX aprovada.
 *
 * Tudo aqui vem do `PayOsSdkTransactionStore` da PayTime e, ate a Onda 1, era
 * descartado — so o NSU da adquirente sobrevivia, no campo `transactionId`.
 *
 * `nsuRequest` e o mais importante: e a chave imutavel da transacao dentro do
 * SDK (primeiro parametro do construtor do store, sem setter) e o que o
 * `revertTransaction` exige. Sem guarda-lo, uma venda ja paga no cartao nao
 * tem como ser estornada pelo terminal.
 */
data class CardTransactionDetails(
    val nsuRequest: String?,
    val nsuAcquirer: String?,
    val acquirerName: String?,
    val brand: String?,
    val panMasked: String?,
    val installments: Int?
)
