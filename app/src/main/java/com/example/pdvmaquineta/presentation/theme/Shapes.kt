package com.example.pdvmaquineta.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// extraSmall é o slot que o Material 3 usa por padrão para o raio de
// TextField/OutlinedTextField — por isso 8dp cai automaticamente em todo
// campo de texto do app. medium/large controlam Card, AlertDialog,
// BottomSheet etc. (12dp, igual aos painéis do protótipo).
//
// Button/OutlinedButton/TextButton NÃO usam nenhum desses slots: o Material 3
// fixa o formato deles em pílula (ShapeDefaults.Full) independente do tema.
// Aplicar CornerRadiusSmall neles exige `shape=` explícito em cada botão (ou
// um composable de botão compartilhado) — fica para quando mexermos nas
// telas.
val PdvShapes = Shapes(
    extraSmall = RoundedCornerShape(PdvDimens.CornerRadiusSmall),
    medium = RoundedCornerShape(PdvDimens.CornerRadius),
    large = RoundedCornerShape(PdvDimens.CornerRadius)
)
