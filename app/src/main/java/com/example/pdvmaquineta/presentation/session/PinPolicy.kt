package com.example.pdvmaquineta.presentation.session

// Politica de tamanho do PIN. O SaaS aceita PIN de >= 4 digitos (sem maximo
// rigido); o app passa a aceitar de 4 a 10 (antes era fixo em 6), pra que
// operadores criados na retaguarda com PIN de 4-5 digitos consigam logar. O
// PIN local de 6 digitos (admin) continua valido.
object PinPolicy {
    const val MIN = 4
    const val MAX = 10
    fun isValidLength(pin: String): Boolean = pin.length in MIN..MAX && pin.all { it.isDigit() }
}
