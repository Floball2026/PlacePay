package com.example.pdvmaquineta.presentation.loyalty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import kotlin.math.floor
import kotlin.math.roundToLong

private fun modeLabel(mode: LoyaltyMode): String = when (mode) {
    LoyaltyMode.POINTS_PER_VALUE -> "Pontos por valor gasto"
    LoyaltyMode.VISIT_COUNT_DISCOUNT -> "Desconto por frequência de visitas"
}

// "50.0" -> "50", "0.333..." fica como está (sem casas fixas) — só evita o
// ".0" feio em resultados inteiros.
private fun formatPlainNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
fun LoyaltyConfigScreen(
    activeConfig: LoyaltyConfig?,
    uiState: LoyaltyConfigUiState,
    pendingChange: PendingLoyaltyChange?,
    onRequestChange: (
        mode: LoyaltyMode,
        pointsPerCurrencyUnit: Double?,
        pointValueInCents: Long?,
        visitsRequired: Int?,
        discountPercentOnReward: Int?
    ) -> Unit,
    onConfirmChange: () -> Unit,
    onDismissChangeConfirmation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(activeConfig?.mode ?: LoyaltyMode.POINTS_PER_VALUE) }

    // Campos do modo "pontos por valor" já na forma direta que o dono do
    // comércio pensa ("a cada R$X gasto, ganho Y pontos"), não invertida.
    // Ao editar uma config existente, reconstrói uma representação
    // equivalente âncorando ganho=1 ponto e resgate=100 pontos (não dá pra
    // recuperar os números exatos que foram digitados originalmente, só a
    // razão resultante).
    var spendThresholdText by remember {
        mutableStateOf(
            activeConfig?.pointsPerCurrencyUnit?.takeIf { it > 0 }
                ?.let { formatPlainNumber(1.0 / it) }.orEmpty()
        )
    }
    var pointsEarnedText by remember {
        mutableStateOf(if (activeConfig?.pointsPerCurrencyUnit != null) "1" else "")
    }
    var pointsRequiredText by remember {
        mutableStateOf(if (activeConfig?.pointValueInCents != null) "100" else "")
    }
    var redeemValueText by remember {
        mutableStateOf(activeConfig?.pointValueInCents?.let { formatPlainNumber(it.toDouble()) }.orEmpty())
    }

    var visitsRequiredText by remember {
        mutableStateOf(activeConfig?.visitsRequired?.toString().orEmpty())
    }
    var discountPercentText by remember {
        mutableStateOf(activeConfig?.discountPercentOnReward?.toString().orEmpty())
    }

    val spendThreshold = spendThresholdText.toDoubleOrNull()
    val pointsEarned = pointsEarnedText.toIntOrNull()
    val pointsRequired = pointsRequiredText.toIntOrNull()
    val redeemValueReais = redeemValueText.toDoubleOrNull()

    val pointsPerCurrencyUnit = if (spendThreshold != null && spendThreshold > 0 && pointsEarned != null) {
        pointsEarned / spendThreshold
    } else {
        null
    }
    val pointValueInCents = if (pointsRequired != null && pointsRequired > 0 && redeemValueReais != null) {
        ((redeemValueReais * 100) / pointsRequired).roundToLong()
    } else {
        null
    }

    val exampleText = buildString {
        if (pointsPerCurrencyUnit != null) {
            val pointsAt100 = floor(100 * pointsPerCurrencyUnit).toInt()
            append("Gastando R$ 100, o cliente ganha $pointsAt100 ponto(s).")
        }
        if (pointsRequired != null && pointsRequired > 0 && redeemValueReais != null) {
            if (isNotEmpty()) append(" ")
            append("Com $pointsRequired pontos, resgata ${formatCents((redeemValueReais * 100).roundToLong())}.")
        }
    }.ifEmpty { null }

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
            "Fidelidade",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (activeConfig != null) {
            Text("Modo ativo: ${modeLabel(activeConfig.mode)}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            when (activeConfig.mode) {
                LoyaltyMode.POINTS_PER_VALUE -> {
                    val activeSpendThreshold = activeConfig.pointsPerCurrencyUnit?.takeIf { it > 0 }
                        ?.let { 1.0 / it }
                    Text(
                        buildString {
                            if (activeSpendThreshold != null) {
                                append(
                                    "A cada R$ ${formatPlainNumber(activeSpendThreshold)} gastos, " +
                                        "o cliente ganha 1 ponto. "
                                )
                            }
                            activeConfig.pointValueInCents?.let {
                                append("A cada 100 pontos, resgata ${formatCents(it * 100)}.")
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                LoyaltyMode.VISIT_COUNT_DISCOUNT -> Text(
                    "${activeConfig.discountPercentOnReward}% de desconto a cada " +
                        "${activeConfig.visitsRequired} visitas",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Text(
                "Nenhum programa de fidelidade ativo ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        Text("Modo", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        LoyaltyMode.entries.forEach { candidate ->
            val buttonModifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
            if (candidate == mode) {
                PdvButton(onClick = { mode = candidate }, modifier = buttonModifier) {
                    Text(modeLabel(candidate))
                }
            } else {
                PdvOutlinedButton(onClick = { mode = candidate }, modifier = buttonModifier) {
                    Text(modeLabel(candidate))
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        if (mode == LoyaltyMode.POINTS_PER_VALUE) {
            Text("Ganho de pontos", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            OutlinedTextField(
                value = spendThresholdText,
                onValueChange = { spendThresholdText = it },
                label = { Text("A cada R$ [valor] gastos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            OutlinedTextField(
                value = pointsEarnedText,
                onValueChange = { pointsEarnedText = it },
                label = { Text("o cliente ganha [quantidade] ponto(s)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(PdvDimens.SpacingLarge))
            Text("Resgate de pontos", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            OutlinedTextField(
                value = pointsRequiredText,
                onValueChange = { pointsRequiredText = it },
                label = { Text("A cada [quantidade] pontos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            OutlinedTextField(
                value = redeemValueText,
                onValueChange = { redeemValueText = it },
                label = { Text("resgata R$ [valor]") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            exampleText?.let {
                Spacer(Modifier.height(PdvDimens.SpacingMedium))
                Text(
                    "Exemplo: $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            OutlinedTextField(
                value = visitsRequiredText,
                onValueChange = { visitsRequiredText = it },
                label = { Text("A cada quantas visitas") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            OutlinedTextField(
                value = discountPercentText,
                onValueChange = { discountPercentText = it },
                label = { Text("Desconto ao completar (%)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = {
                onRequestChange(
                    mode,
                    pointsPerCurrencyUnit,
                    pointValueInCents,
                    visitsRequiredText.toIntOrNull(),
                    discountPercentText.toIntOrNull()
                )
            },
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

    if (pendingChange != null) {
        AlertDialog(
            onDismissRequest = onDismissChangeConfirmation,
            title = { Text("Trocar configuração de fidelidade") },
            text = {
                Text(
                    "A configuração atual será encerrada agora e uma nova vigência começa a partir " +
                        "deste momento. O histórico da configuração anterior não é apagado — continua " +
                        "consultável normalmente, só deixa de valer para novas transações."
                )
            },
            confirmButton = {
                PdvTextButton(onClick = onConfirmChange) { Text("Confirmar") }
            },
            dismissButton = {
                PdvTextButton(onClick = onDismissChangeConfirmation) { Text("Cancelar") }
            }
        )
    }
}
