package com.example.pdvmaquineta.presentation.theme

import androidx.compose.ui.unit.dp

// Dimensões padrão da UI da maquineta: botões grandes o suficiente para
// toque rápido/impreciso no balcão, espaçamentos generosos para tela pequena.
object PdvDimens {
    val ButtonHeight = 64.dp
    // Botão-herói (ex.: "Nova Venda" no caixa): mais alto que os demais para
    // se destacar como ação primária.
    val HeroButtonHeight = 88.dp
    // Botão compacto usado no grid do caixa: menor que ButtonHeight para caber
    // muitas ações (perfil admin) sem rolar, mas ainda no mínimo de toque (48dp).
    val GridButtonHeight = 48.dp
    val ButtonMinWidth = 140.dp
    val TouchTargetMin = 56.dp

    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingExtraLarge = 32.dp

    // Botões/inputs (do protótipo). Nota: o Material 3 só aplica isso a
    // TextField/OutlinedTextField automaticamente (via shapes.extraSmall);
    // Button/OutlinedButton/TextButton usam pílula fixa e precisam de
    // `shape=` explícito por chamada — ver Shapes.kt.
    val CornerRadiusSmall = 12.dp

    // Painéis/cards.
    val CornerRadius = 16.dp

    // Grade de produtos (Fase 7 - Modernização)
    val ProductTileHeight = 110.dp
    val CategoryTabHeight = 48.dp
    val BottomBarHeight = 100.dp
}
