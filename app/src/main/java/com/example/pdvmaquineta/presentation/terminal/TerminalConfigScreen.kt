package com.example.pdvmaquineta.presentation.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.PrinterType
import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.model.TerminalEnvironment
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

private fun environmentLabel(environment: TerminalEnvironment): String = when (environment) {
    TerminalEnvironment.HOMOLOGACAO -> "Homologação"
    TerminalEnvironment.PRODUCAO -> "Produção"
}

private fun printerTypeLabel(printerType: PrinterType): String = when (printerType) {
    PrinterType.NONE -> "Nenhuma"
    PrinterType.INTEGRATED -> "Integrada"
    PrinterType.BLUETOOTH -> "Bluetooth"
}

@Composable
fun TerminalConfigScreen(
    config: TerminalConfig?,
    uiState: TerminalConfigUiState,
    onSave: (
        terminalName: String,
        storeName: String,
        environment: TerminalEnvironment,
        printerType: PrinterType
    ) -> Unit,
    activationState: ActivationUiState,
    initialBaseUrl: String,
    isActivated: Boolean,
    terminalId: String,
    onActivate: (baseUrl: String, code: String) -> Unit,
    isOutdated: Boolean,
    currentVersion: Int,
    minVersion: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var terminalName by remember(config) { mutableStateOf(config?.terminalName.orEmpty()) }
    var storeName by remember(config) { mutableStateOf(config?.storeName.orEmpty()) }
    var environment by remember(config) { mutableStateOf(config?.environment ?: TerminalEnvironment.HOMOLOGACAO) }
    var printerType by remember(config) { mutableStateOf(config?.printerType ?: PrinterType.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text(
            "Terminal",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        // ----- Integração com o servidor (SaaS) -----
        var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
        var activationCode by remember { mutableStateOf("") }
        Text("Servidor (integração)", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        Text(
            text = if (isActivated)
                "Status: ativado${if (terminalId.isNotBlank()) " (terminal $terminalId)" else ""}"
            else "Status: não ativado",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActivated) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isOutdated) {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(
                text = "⚠ App desatualizado (versão $currentVersion; mínima exigida $minVersion). Atualize o aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL do servidor (ex.: https://seu-app.replit.app)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        OutlinedTextField(
            value = activationCode,
            onValueChange = { activationCode = it },
            label = { Text("Código de ativação") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvButton(
            onClick = { onActivate(baseUrl, activationCode) },
            enabled = !activationState.isLoading,
            modifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
        ) {
            Text(
                if (activationState.isLoading) "Ativando..."
                else if (isActivated) "Reativar terminal" else "Ativar terminal"
            )
        }
        activationState.message?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.tertiary)
        }
        activationState.error?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        OutlinedTextField(
            value = terminalName,
            onValueChange = { terminalName = it },
            label = { Text("Nome do terminal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Nome da loja") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        Text("Ambiente", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        TerminalEnvironment.entries.forEach { candidate ->
            val buttonModifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
            if (candidate == environment) {
                PdvButton(onClick = { environment = candidate }, modifier = buttonModifier) {
                    Text(environmentLabel(candidate))
                }
            } else {
                PdvOutlinedButton(onClick = { environment = candidate }, modifier = buttonModifier) {
                    Text(environmentLabel(candidate))
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text("Impressora", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PrinterType.entries.forEach { candidate ->
            val buttonModifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
            if (candidate == printerType) {
                PdvButton(onClick = { printerType = candidate }, modifier = buttonModifier) {
                    Text(printerTypeLabel(candidate))
                }
            } else {
                PdvOutlinedButton(onClick = { printerType = candidate }, modifier = buttonModifier) {
                    Text(printerTypeLabel(candidate))
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
        uiState.successMessage?.let { msg ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = msg, color = MaterialTheme.colorScheme.tertiary)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { onSave(terminalName, storeName, environment, printerType) },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Salvar")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar ao caixa")
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}
