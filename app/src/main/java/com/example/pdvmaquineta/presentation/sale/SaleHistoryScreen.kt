package com.example.pdvmaquineta.presentation.sale

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
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateTimeFormat =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.Builder().setLanguage("pt").setRegion("BR").build())
private val dateOnlyFormat =
    SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())

// "dd/MM/aaaa" -> intervalo [00:00, 23:59:59.999] daquele dia, ou null se o
// texto não for uma data válida (inclui campo vazio, que significa "sem
// filtro").
private fun parseDayRange(text: String): Pair<Long, Long>? {
    if (text.isBlank()) return null
    return try {
        dateOnlyFormat.isLenient = false
        val parsed = dateOnlyFormat.parse(text) ?: return null
        val start = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }
        start.timeInMillis to end.timeInMillis
    } catch (e: java.text.ParseException) {
        null
    }
}

@Composable
fun SaleHistoryScreen(
    sales: List<Sale>,
    onDateFilterChange: (fromMillis: Long?, toMillis: Long?) -> Unit,
    onOpenSale: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dateText by remember { mutableStateOf("") }

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
            text = "Histórico de vendas",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        OutlinedTextField(
            value = dateText,
            onValueChange = {
                dateText = it
                val range = parseDayRange(it)
                onDateFilterChange(range?.first, range?.second)
            },
            label = { Text("Buscar por data (dd/mm/aaaa)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (sales.isEmpty()) {
            Text(
                "Nenhuma venda concluída encontrada.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sales.forEach { sale ->
                SaleHistoryRow(sale = sale, onClick = { onOpenSale(sale.id) })
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar ao caixa")
        }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun SaleHistoryRow(sale: Sale, onClick: () -> Unit) {
    PdvButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(PdvDimens.ButtonHeight)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Venda #${sale.id}")
            Text(dateTimeFormat.format(Date(sale.createdAt)))
        }
    }
}
