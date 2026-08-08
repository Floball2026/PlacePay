package com.example.pdvmaquineta.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.pdvmaquineta.domain.model.ThemeTone

// Sempre um tom da mesma identidade navy+dourado da Place Pay — nunca varia
// com o tema claro/escuro do sistema. O que muda por ThemeTone (Fase 6) é só
// a luminosidade do fundo/superfície (e, no tom mais claro, o texto
// secundário/erro, ajustados por contraste); matiz e dourado de ação são
// fixos nos três tons.
private fun colorSchemeFor(tone: ThemeTone): ColorScheme {
    if (tone == ThemeTone.BRAND_LIGHT) {
        return lightColorScheme(
            primary = PdvGold,
            onPrimary = PdvOnGold,
            tertiary = PdvTextPrimaryLight, // valores/precos: azul-marinho (legivel no branco)
            onTertiary = Color.White,
            secondary = PdvNavyBackground, // Azul Marinho como cor secundária
            onSecondary = Color.White,
            error = PdvErrorLight,
            onError = Color.White,
            background = PdvLightBackground,
            onBackground = PdvTextPrimaryLight,
            surface = PdvLightSurface,
            onSurface = PdvTextPrimaryLight,
            onSurfaceVariant = PdvTextSecondaryLight,
            outline = PdvBorderLight,
            outlineVariant = PdvBorderLight,
            surfaceContainer = PdvLightSurfaceVariant,
            surfaceContainerHigh = PdvLightSurfaceVariant,
            surfaceContainerHighest = PdvLightSurfaceVariant
        )
    }

    val (background, surface, surfaceVariant, textSecondary, errorColor) = when (tone) {
        ThemeTone.NAVY_DARK -> ToneColors(
            PdvNavyBackground, PdvNavySurface, PdvNavySurfaceVariant, PdvTextSecondary, PdvErrorOnNavy
        )
        ThemeTone.NAVY_MEDIUM -> ToneColors(
            PdvNavyBackgroundMedium, PdvNavySurfaceMedium, PdvNavySurfaceVariantMedium,
            PdvTextSecondary, PdvErrorOnNavy
        )
        ThemeTone.NAVY_LIGHT -> ToneColors(
            PdvNavyBackgroundLight, PdvNavySurfaceLight, PdvNavySurfaceVariantLight,
            PdvTextSecondaryOnLight, PdvErrorOnNavyLight
        )
        else -> ToneColors( // Fallback (não deve ocorrer devido ao if acima)
            PdvNavyBackground, PdvNavySurface, PdvNavySurfaceVariant, PdvTextSecondary, PdvErrorOnNavy
        )
    }

    return darkColorScheme(
        primary = PdvGold,
        onPrimary = PdvOnGold,
        tertiary = PdvGold, // valores/precos: dourado sobre o navy
        onTertiary = PdvOnGold,
        secondary = PdvGold,
        onSecondary = PdvOnGold,
        error = errorColor,
        onError = background,
        background = background,
        onBackground = PdvTextPrimary,
        surface = surface,
        onSurface = PdvTextPrimary,
        onSurfaceVariant = textSecondary,
        outline = PdvBorderStrong,
        outlineVariant = PdvBorder,
        surfaceContainer = surfaceVariant,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant
    )
}

private data class ToneColors(
    val background: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val surfaceVariant: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color
)

@Composable
fun PDVMaquinetaTheme(
    tone: ThemeTone = ThemeTone.BRAND_LIGHT,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(tone),
        typography = Typography,
        shapes = PdvShapes,
        content = content
    )
}
