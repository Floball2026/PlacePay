package com.example.pdvmaquineta.presentation.sale

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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.format.parseToCents
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

private fun methodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Dinheiro"
    PaymentMethod.CREDIT_CARD -> "Cartão de crédito"
    PaymentMethod.DEBIT_CARD -> "Cartão de débito"
    PaymentMethod.PIX -> "Pix"
}

private fun methodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.CASH -> Icons.Filled.AttachMoney
    PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> Icons.Filled.CreditCard
    PaymentMethod.PIX -> Icons.Filled.QrCode
}

@Composable
fun PaymentMethodScreen(
    totalCents: Long,
    selectedMethod: PaymentMethod?,
    acceptedMethods: List<PaymentMethod>,
    uiState: PaymentUiState,
    onSelectMethod: (PaymentMethod) -> Unit,
    onConfirm: (receivedCents: Long?) -> Unit,
    onBackToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var receivedText by remember { mutableStateOf("") }
    val receivedCents = parseToCents(receivedText)
    val changeCents = if (selectedMethod == PaymentMethod.CASH && receivedCents != null) {
        (receivedCents - totalCents).coerceAtLeast(0)
    } else {
        null
    }
    val canConfirm = when (selectedMethod) {
        null -> false
        PaymentMethod.CASH -> receivedCents != null && receivedCents >= totalCents
        else -> true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        Text("Total a pagar", style = MaterialTheme.typography.bodyLarge)
        Text(formatCents(totalCents), style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        acceptedMethods.forEach { method ->
            PaymentMethodOption(
                label = methodLabel(method),
                icon = methodIcon(method),
                selected = selectedMethod == method,
                onClick = { onSelectMethod(method) }
            )
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        if (selectedMethod == PaymentMethod.CASH) {
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            OutlinedTextField(
                value = receivedText,
                onValueChange = { receivedText = it },
                label = { Text("Valor recebido (R$)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            changeCents?.let {
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text("Troco: ${formatCents(it)}", style = MaterialTheme.typography.bodyLarge)
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { onConfirm(if (selectedMethod == PaymentMethod.CASH) receivedCents else null) },
            enabled = !uiState.isLoading && canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Confirmar pagamento")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onBackToCart) {
            Text("Voltar ao carrinho")
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun PaymentMethodOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(PdvDimens.ButtonHeight)

    if (selected) {
        PdvButton(onClick = onClick, modifier = modifier) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text(label)
        }
    } else {
        PdvOutlinedButton(onClick = onClick, modifier = modifier) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text(label)
        }
    }
}
