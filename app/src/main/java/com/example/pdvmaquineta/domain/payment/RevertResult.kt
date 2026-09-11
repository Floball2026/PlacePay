package com.example.pdvmaquineta.domain.payment

/**
 * Resultado de um pedido de estorno na adquirente.
 *
 * `Reverted` significa que a adquirente CONFIRMOU a reversao — e so com essa
 * confirmacao a venda pode ser marcada como cancelada. Qualquer outra coisa
 * mantem a venda valida: melhor uma venda que o operador nao conseguiu
 * cancelar do que uma venda cancelada nos relatorios com o dinheiro cobrado.
 */
sealed class RevertResult {
    data object Reverted : RevertResult()
    data class Failed(val reason: String) : RevertResult()
    /** O meio de pagamento nao passa por adquirente (dinheiro) ou o gateway nao estorna. */
    data object NotApplicable : RevertResult()
}
