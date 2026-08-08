package com.example.pdvmaquineta.domain.format

import java.text.NumberFormat
import java.util.Locale

// Formatação pura (sem dependência de Android/Compose) — vive em domain pra
// poder ser usada tanto por presentation (telas) quanto por data (geração de
// arquivos de exportação, Fase 7c) sem violar a direção de dependência da
// Clean Architecture.
private val brazilianCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(
    Locale.Builder().setLanguage("pt").setRegion("BR").build()
)

fun formatCents(cents: Long): String = brazilianCurrencyFormat.format(cents / 100.0)
