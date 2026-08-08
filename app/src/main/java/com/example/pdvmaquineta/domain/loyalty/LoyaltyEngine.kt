package com.example.pdvmaquineta.domain.loyalty

import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType

// O que uma venda gera de fidelidade — pontos (modo por valor) ou uma visita
// contada (modo por frequência). O campo que não se aplica ao modo fica null.
data class LoyaltyEarnResult(
    val type: LoyaltyTransactionType,
    val points: Int? = null
)

// Quanto dá pra resgatar agora, em centavos de desconto pra venda atual, mais
// um resumo pra exibir na tela (pontos disponíveis ou progresso de visitas).
// `pointsAvailable` é o saldo total do cliente (pra exibição); `pointsToConsume`
// é só o que essa venda específica de fato consome — resgate é parcial
// automático, nunca gasta mais pontos do que o necessário pra cobrir a venda.
data class LoyaltyRedeemable(
    val redeemableCents: Long,
    val pointsAvailable: Int? = null,
    val pointsToConsume: Int? = null,
    val visitsCount: Int? = null,
    val visitsRequired: Int? = null
)

// Abstração sobre a regra de fidelidade ativa — troca de modo (admin) não
// deve exigir mudança nenhuma em caso de uso, ViewModel ou tela, só a
// implementação escolhida pela LoyaltyEngineFactory a partir da config ativa.
interface LoyaltyEngine {
    fun calculateEarn(saleTotalCents: Long): LoyaltyEarnResult

    // `transactions` já deve vir filtrado pela janela da config ativa
    // (activatedAt até agora) por quem chama. `saleNetCents` é o que falta
    // pagar depois do desconto manual (subtotal - discountCents), não o
    // subtotal bruto — resgatar não pode valer mais do que isso.
    fun calculateRedeemable(transactions: List<LoyaltyTransaction>, saleNetCents: Long): LoyaltyRedeemable
}
