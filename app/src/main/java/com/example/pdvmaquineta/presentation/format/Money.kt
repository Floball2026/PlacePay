package com.example.pdvmaquineta.presentation.format

import kotlin.math.roundToLong

// Aceita "12,34" ou "12.34" digitado pelo operador; null se não for um valor válido.
fun parseToCents(input: String): Long? {
    val normalized = input.trim().replace(",", ".")
    val value = normalized.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return (value * 100).roundToLong()
}
