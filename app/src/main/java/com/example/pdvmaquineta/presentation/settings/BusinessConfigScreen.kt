package com.example.pdvmaquineta.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.BusinessConfig
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton

// Configuracoes do negocio, editadas LOCALMENTE no terminal (admin/supervisor).
// Salva no BusinessConfigStore; o fluxo de venda consulta esses valores.
@Composable
fun BusinessConfigScreen(
    config: BusinessConfig,
    savedMessage: String?,
    onChange: (BusinessConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text(
            "Configurações do negócio",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        SectionTitle("Recursos")
        ToggleRow("Cashback habilitado", config.enableCashback) {
            onChange(config.copy(enableCashback = it))
        }
        ToggleRow(
            "Pedir CPF do cliente",
            config.requireCustomerCpf,
            subtitle = "Só lembra o operador; não obriga a venda."
        ) { onChange(config.copy(requireCustomerCpf = it)) }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        SectionTitle("Desconto")
        OutlinedTextField(
            value = config.maxDiscountPercent.toString(),
            onValueChange = { text ->
                val n = text.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceIn(0, 100) ?: 0
                onChange(config.copy(maxDiscountPercent = n))
            },
            label = { Text("Desconto máximo (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        SectionTitle("Formas de pagamento aceitas")
        ToggleRow("Dinheiro", config.payCash) { onChange(config.copy(payCash = it)) }
        ToggleRow("Crédito", config.payCredit) { onChange(config.copy(payCredit = it)) }
        ToggleRow("Débito", config.payDebit) { onChange(config.copy(payDebit = it)) }
        ToggleRow("PIX", config.payPix) { onChange(config.copy(payPix = it)) }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        SectionTitle("Exigir supervisor para")
        ToggleRow("Correção de preço", config.requireSupervisorPriceChange) {
            onChange(config.copy(requireSupervisorPriceChange = it))
        }
        ToggleRow("Cancelar venda", config.requireSupervisorCancel) {
            onChange(config.copy(requireSupervisorCancel = it))
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        PdvButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
        ) { Text("Salvar") }
        savedMessage?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvOutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
        ) { Text("Voltar") }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(PdvDimens.SpacingSmall))
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PdvDimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
