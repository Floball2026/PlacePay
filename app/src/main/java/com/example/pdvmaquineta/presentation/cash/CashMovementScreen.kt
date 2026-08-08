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
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.presentation.authorization.SupervisorAuthorizationDialog
import com.example.pdvmaquineta.presentation.format.parseToCents
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

private fun movementTitle(type: CashMovementType): String = when (type) {
    CashMovementType.WITHDRAWAL -> "Sangria"
    CashMovementType.SUPPLY -> "Suprimento"
}

@Composable
fun CashMovementScreen(
    type: CashMovementType,
    uiState: MovementUiState,
    onSubmit: (amountCents: Long, reason: String) -> Unit,
    onAuthorize: (username: String, password: String) -> Unit,
    onDismissAuthorization: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val amountCents = parseToCents(amountText)
    val title = movementTitle(type)
    val awaitingAuthorization = uiState.authorizationRequired && uiState.authorization == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        Text(text = title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)

        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Valor (R$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Motivo") },
            modifier = Modifier.fillMaxWidth()
        )

        uiState.authorization?.let { authorization ->
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            Text(
                text = "Autorizado por: ${authorization.authorizedByUsername}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (!awaitingAuthorization) {
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { amountCents?.let { onSubmit(it, reason) } },
            enabled = !uiState.isLoading && amountCents != null && reason.isNotBlank() && !awaitingAuthorization,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Confirmando..." else "Confirmar $title")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onCancel) {
            Text("Cancelar")
        }
    }

    if (awaitingAuthorization) {
        SupervisorAuthorizationDialog(
            errorMessage = uiState.errorMessage,
            onAuthorize = onAuthorize,
            onDismiss = onDismissAuthorization
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CashMovementScreenPreview() {
    PDVMaquinetaTheme {
        CashMovementScreen(
            type = CashMovementType.WITHDRAWAL,
            uiState = MovementUiState(),
            onSubmit = { _, _ -> },
            onAuthorize = { _, _ -> },
            onDismissAuthorization = {},
            onCancel = {}
        )
    }
}
