package com.example.pdvmaquineta.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pdvmaquineta.R

// Fonte Inter embutida no app (funciona offline, sem depender de rede). Pesos
// disponiveis: 400 (Normal), 600 (SemiBold), 700 (Bold) e 900 (Black). Pesos
// intermediarios (ex.: 500/800) sao mapeados para o mais proximo pelo Compose.
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_black, FontWeight.Black)
)

// Aplica a Inter a TODOS os estilos de texto do Material 3, preservando os
// tamanhos padrao dos estilos que nao sobrescrevemos abaixo.
private fun Typography.withInter(): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = titleLarge.copy(fontFamily = InterFontFamily),
    titleMedium = titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = labelSmall.copy(fontFamily = InterFontFamily)
)

// Tipografia com tamanhos acima do padrao Material para leitura rapida em
// tela pequena e a distancia de braco (valores de venda, totais, mensagens).
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    )
).withInter()
