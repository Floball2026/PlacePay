package com.example.pdvmaquineta.presentation.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Wrappers finos sobre os botões do Material 3 só pra aplicar o raio de 8dp
// do protótipo: Button/OutlinedButton/TextButton usam pílula fixa por padrão
// (ShapeDefaults.Full), não o Shapes do tema — então isso precisa ser
// passado em cada chamada, e esses wrappers evitam repetir o `shape=` em
// toda tela.
private val PdvButtonShape = RoundedCornerShape(PdvDimens.CornerRadiusSmall)

@Composable
fun PdvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PdvButtonShape,
        colors = colors,
        content = content
    )
}

@Composable
fun PdvOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PdvButtonShape,
        content = content
    )
}

@Composable
fun PdvTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PdvButtonShape,
        content = content
    )
}
