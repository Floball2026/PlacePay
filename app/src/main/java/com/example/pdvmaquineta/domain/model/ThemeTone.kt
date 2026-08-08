package com.example.pdvmaquineta.domain.model

// Três variações de luminosidade da mesma paleta navy da Place Pay — nunca
// muda o matiz nem o dourado de ação, só clareia o fundo/superfície.
enum class ThemeTone {
    NAVY_DARK,
    NAVY_MEDIUM,
    NAVY_LIGHT,
    BRAND_LIGHT
}

fun ThemeTone.next(): ThemeTone = when (this) {
    ThemeTone.NAVY_DARK -> ThemeTone.NAVY_MEDIUM
    ThemeTone.NAVY_MEDIUM -> ThemeTone.NAVY_LIGHT
    ThemeTone.NAVY_LIGHT -> ThemeTone.BRAND_LIGHT
    ThemeTone.BRAND_LIGHT -> ThemeTone.NAVY_DARK
}
