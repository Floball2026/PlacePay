package com.example.pdvmaquineta.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Paleta baseada na identidade visual da Place Pay (adquirente do cliente):
// azul-marinho escuro + amarelo/dourado + texto branco. Substitui a paleta
// anterior (derivada do protótipo genérico da Fase 2). O matiz e o dourado de
// ação são fixos — só a luminosidade do fundo/superfície varia por
// ThemeTone (Fase 6), derivada programaticamente a partir destes valores.

// Clareia uma cor aumentando sua luminosidade (HSL) em `amount` (0..1),
// mantendo matiz e saturação — usado pra derivar NAVY_MEDIUM/NAVY_LIGHT a
// partir do NAVY_DARK sem precisar calcular hex à mão.
internal fun Color.lightened(amount: Float): Color {
    val r = red
    val g = green
    val b = blue

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f

    if (maxC == minC) {
        val newL = (l + amount).coerceIn(0f, 1f)
        return Color(newL, newL, newL, alpha)
    }

    val delta = maxC - minC
    val s = if (l > 0.5f) delta / (2f - maxC - minC) else delta / (maxC + minC)
    val h = 60f * when (maxC) {
        r -> ((g - b) / delta).let { if (g < b) it + 6f else it }
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    }

    val newL = (l + amount).coerceIn(0f, 1f)
    val c = (1f - abs(2f * newL - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = newL - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r1 + m, g1 + m, b1 + m, alpha)
}

private const val MEDIUM_LIGHTEN = 0.15f
private const val LIGHT_LIGHTEN = 0.30f

// Ajuste específico de texto/erro só pro tom mais claro (verificado por
// contraste WCAG — ver ROADMAP): no fundo NAVY_LIGHT o texto secundário e o
// vermelho de erro (pensados pro fundo bem mais escuro) perdem contraste
// suficiente; DARK/MEDIUM não precisam disso.
private const val TEXT_LIGHTEN_FOR_LIGHT_TONE = 0.20f

// Fundo/superfície — azul-marinho escuro (tom padrão, NAVY_DARK; valores
// inalterados desde a Fase 1)
val PdvNavyBackground = Color(0xFF0B1F4E)     // fundo principal da tela
val PdvNavySurface = Color(0xFF122A63)        // painéis/cards, levemente mais claro que o fundo
val PdvNavySurfaceVariant = Color(0xFF2A3B6B) // pill/badge, diálogo, bordas sutis

// Tom NAVY_MEDIUM — derivado (+15% de luminosidade)
val PdvNavyBackgroundMedium = PdvNavyBackground.lightened(MEDIUM_LIGHTEN)
val PdvNavySurfaceMedium = PdvNavySurface.lightened(MEDIUM_LIGHTEN)
val PdvNavySurfaceVariantMedium = PdvNavySurfaceVariant.lightened(MEDIUM_LIGHTEN)

// Tom NAVY_LIGHT — derivado (+30% de luminosidade)
val PdvNavyBackgroundLight = PdvNavyBackground.lightened(LIGHT_LIGHTEN)
val PdvNavySurfaceLight = PdvNavySurface.lightened(LIGHT_LIGHTEN)
val PdvNavySurfaceVariantLight = PdvNavySurfaceVariant.lightened(LIGHT_LIGHTEN)

// Texto sobre fundo navy (tom padrão — DARK e MEDIUM usam estes mesmos valores)
val PdvTextPrimary = Color(0xFFFFFFFF)   // texto principal (branco)
val PdvTextSecondary = Color(0xFFA9B4CC) // texto secundário (cinza-azulado claro)

// Variantes só pro tom NAVY_LIGHT (contraste verificado, ver comentário acima)
val PdvTextSecondaryOnLight = PdvTextSecondary.lightened(TEXT_LIGHTEN_FOR_LIGHT_TONE)
val PdvErrorOnNavyLight = Color(0xFFFF6B6B).lightened(TEXT_LIGHTEN_FOR_LIGHT_TONE)

// Bordas — brancas semi-transparentes (bordas pretas seriam invisíveis no
// fundo escuro)
val PdvBorder = Color(0x1FFFFFFF)       // rgba(255,255,255,0.12)
val PdvBorderStrong = Color(0x38FFFFFF) // rgba(255,255,255,0.22)

// Ação primária: amarelo/dourado com texto navy (contraste alto, não branco)
val PdvGold = Color(0xFFFFC933)
val PdvOnGold = Color(0xFF0B1F4E)

// Erro como texto direto sobre o fundo navy (é assim que as telas usam hoje
// — cor de texto de validação, não uma pílula com fundo próprio). Mais claro
// que um vermelho "de fundo claro" pra não perder contraste no escuro.
val PdvErrorOnNavy = Color(0xFFFF6B6B)

// Estados semânticos em pares fundo+texto (pílulas/badges) — continuam
// funcionando sobre fundo escuro porque cada um carrega o próprio fundo
// claro. Propositalmente longe do dourado pra não confundir com o CTA.
val PdvBgSuccess = Color(0xFFEAF3DE)
val PdvTextSuccess = Color(0xFF27500A)
val PdvBgWarning = Color(0xFFFAEEDA)
val PdvTextWarning = Color(0xFF633806)
val PdvBgDanger = Color(0xFFFCEBEB)
val PdvTextDanger = Color(0xFF791F1F)

// Ação destrutiva preenchida (ex.: um futuro "Confirmar cancelamento")
val PdvFillDanger = Color(0xFFE24B4A)
val PdvOnDanger = Color(0xFFFFFFFF)

// Cores para o modo claro (BRAND_LIGHT)
val PdvLightBackground = Color(0xFFFFFFFF)
val PdvLightSurface = Color(0xFFF8F9FA)
val PdvLightSurfaceVariant = Color(0xFFE9ECEF)
val PdvTextPrimaryLight = Color(0xFF0B1F4E)    // Texto em azul marinho
val PdvTextSecondaryLight = Color(0xFF667085)  // Cinza médio
val PdvBorderLight = Color(0x1F0B1F4E)         // Azul marinho semi-transparente
val PdvErrorLight = Color(0xFFD32F2F)         // Vermelho padrão para modo claro
