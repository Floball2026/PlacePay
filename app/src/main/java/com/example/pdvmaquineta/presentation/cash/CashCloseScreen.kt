package com.example.pdvmaquineta.presentation.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdvmaquineta.domain.usecase.CashOverview
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.format.parseToCents
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

@Composable
fun CashCloseScreen(
    overview: CashOverview?,
    uiState: CloseCashUiState,
    onConfirm: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var informedText by remember { mutableStateOf("") }
    val informedCents = parseToCents(informedText)
    val divergenceCents = if (overview != null && informedCents != null) {
        informedCents - overview.expectedCashCents
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        Text(
            text = "Fechamento de caixa",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (overview != null) {
            Text(
                text = "Vendas em dinheiro: ${formatCents(overview.cashSalesCents)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Vendas em cartão: ${formatCents(overview.cardSalesCents)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Vendas em Pix: ${formatCents(overview.pixSalesCents)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(
                text = "Saldo esperado em dinheiro: ${formatCents(overview.expectedCashCents)}",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        OutlinedTextField(
            value = informedText,
            onValueChange = { informedText = it },
            label = { Text("Valor contado em dinheiro (R$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        divergenceCents?.let { divergence ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            val label = when {
                divergence == 0L -> "Sem divergência"
                divergence > 0L -> "Sobra de ${formatCents(divergence)}"
                else -> "Falta de ${formatCents(-divergence)}"
            }
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { informedCents?.let(onConfirm) },
            enabled = !uiState.isLoading && informedCents != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Fechando..." else "Fechar caixa")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onCancel) {
            Text("Cancelar")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CashCloseScreenPreview() {
    PDVMaquinetaTheme {
        CashCloseScreen(
            overview = null,
            uiState = CloseCashUiState(),
            onConfirm = {},
            onCancel = {}
        )
    }
}
