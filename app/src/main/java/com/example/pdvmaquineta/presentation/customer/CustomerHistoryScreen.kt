package com.example.pdvmaquineta.presentation.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.model.LoyaltyTransactionType
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun typeLabel(type: LoyaltyTransactionType): String = when (type) {
    LoyaltyTransactionType.EARNED -> "Pontos ganhos"
    LoyaltyTransactionType.REDEEMED -> "Pontos resgatados"
    LoyaltyTransactionType.VISIT_COUNTED -> "Visita contabilizada"
    LoyaltyTransactionType.REWARD_REDEEMED -> "Prêmio resgatado"
    LoyaltyTransactionType.REDEMPTION_REVERSED -> "Estorno de resgate"
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.Builder().setLanguage("pt").setRegion("BR").build())

@Composable
fun CustomerHistoryScreen(
    customer: Customer,
    transactions: List<LoyaltyTransaction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "Histórico de fidelidade",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = customer.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (transactions.isEmpty()) {
            Text(
                "Nenhuma transação de fidelidade ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            transactions.forEach { transaction ->
                HistoryRow(transaction)
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar")
        }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun HistoryRow(transaction: LoyaltyTransaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(typeLabel(transaction.type), style = MaterialTheme.typography.bodyLarge)
            Text(dateFormat.format(Date(transaction.timestamp)), style = MaterialTheme.typography.bodyLarge)
        }
        transaction.points?.let {
            Text("Pontos: $it", style = MaterialTheme.typography.bodyLarge)
        }
        transaction.amountCents?.let {
            Text("Valor: ${formatCents(it)}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
