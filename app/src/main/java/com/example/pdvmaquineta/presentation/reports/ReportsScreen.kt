package com.example.pdvmaquineta.presentation.reports

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.usecase.ReportData
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateOnlyFormat =
    SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())

private fun methodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Dinheiro"
    PaymentMethod.CREDIT_CARD -> "Cartão de crédito"
    PaymentMethod.DEBIT_CARD -> "Cartão de débito"
    PaymentMethod.PIX -> "Pix"
}

// "dd/MM/aaaa" -> início/fim do dia em millis, ou null se vazio/inválido.
private fun parseStartOfDay(text: String): Long? = parseDay(text, endOfDay = false)
private fun parseEndOfDay(text: String): Long? = parseDay(text, endOfDay = true)

private fun parseDay(text: String, endOfDay: Boolean): Long? {
    if (text.isBlank()) return null
    return try {
        dateOnlyFormat.isLenient = false
        val parsed = dateOnlyFormat.parse(text) ?: return null
        val calendar = Calendar.getInstance().apply {
            time = parsed
            if (endOfDay) {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            } else {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
        }
        calendar.timeInMillis
    } catch (e: ParseException) {
        null
    }
}

private fun formatDate(millis: Long): String = dateOnlyFormat.format(Date(millis))

private fun exportFormatLabel(format: ExportFormat): String = when (format) {
    ExportFormat.CSV -> "CSV"
    ExportFormat.PDF -> "PDF"
    ExportFormat.EXCEL -> "Excel"
}

@Composable
fun ReportsScreen(
    user: User,
    reportData: ReportData?,
    uiState: ReportsUiState,
    exportUiState: ExportUiState,
    onDateFilterChange: (fromMillis: Long?, toMillis: Long?) -> Unit,
    onExport: (ExportFormat) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var showFormatPicker by remember { mutableStateOf(false) }
    val canViewFull = AuthorizationPolicy.hasPermission(user.role, Permission.VIEW_FULL_REPORTS)
    val context = LocalContext.current

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
            "Relatórios",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (!canViewFull) {
            Text(
                "Período limitado aos últimos 30 dias.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)) {
            OutlinedTextField(
                value = fromText,
                onValueChange = {
                    fromText = it
                    onDateFilterChange(parseStartOfDay(it), parseEndOfDay(toText))
                },
                label = { Text("De (dd/mm/aaaa)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = toText,
                onValueChange = {
                    toText = it
                    onDateFilterChange(parseStartOfDay(fromText), parseEndOfDay(it))
                },
                label = { Text("Até (dd/mm/aaaa)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        if (reportData != null) {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            val periodText = if (reportData.fromMillis != null) {
                "Período consultado: ${formatDate(reportData.fromMillis)} até " +
                    (reportData.toMillis?.let { formatDate(it) } ?: "hoje")
            } else {
                "Período consultado: desde o início do histórico" +
                    (reportData.toMillis?.let { " até ${formatDate(it)}" } ?: "")
            }
            Text(periodText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(PdvDimens.SpacingLarge))
            SectionCard(title = "Vendas") {
                Text("Total vendido: ${formatCents(reportData.sales.totalSoldCents)}", style = MaterialTheme.typography.bodyLarge)
                Text("Vendas concluídas: ${reportData.sales.completedSalesCount}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text("Por forma de pagamento", style = MaterialTheme.typography.titleLarge)
                if (reportData.sales.totalsByMethod.isEmpty()) {
                    Text("Nenhum pagamento aprovado no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reportData.sales.totalsByMethod.forEach { (method, cents) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(methodLabel(method))
                            Text(formatCents(cents))
                        }
                    }
                }
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                TopProductsList(reportData.sales.topProducts)
            }

            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            SectionCard(title = "Caixa") {
                Text("Sessões fechadas no período: ${reportData.cash.closedSessionsCount}", style = MaterialTheme.typography.bodyLarge)
                Text("Total de abertura: ${formatCents(reportData.cash.totalOpeningCents)}", style = MaterialTheme.typography.bodyLarge)
                Text("Total informado no fechamento: ${formatCents(reportData.cash.totalInformedCents)}", style = MaterialTheme.typography.bodyLarge)
                Text("Sangrias: ${formatCents(reportData.cash.totalWithdrawalCents)}", style = MaterialTheme.typography.bodyLarge)
                Text("Suprimentos: ${formatCents(reportData.cash.totalSupplyCents)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text("Divergências identificadas", style = MaterialTheme.typography.titleLarge)
                if (reportData.cash.divergentSessions.isEmpty()) {
                    Text("Nenhuma divergência no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reportData.cash.divergentSessions.forEach { session -> DivergenceRow(session) }
                }
            }

            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            SectionCard(title = "Produtos") {
                Text("Estoque baixo agora", style = MaterialTheme.typography.titleLarge)
                if (reportData.products.lowStockProducts.isEmpty()) {
                    Text("Nenhum produto com estoque baixo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reportData.products.lowStockProducts.forEach { product -> LowStockRow(product) }
                }
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                TopProductsList(reportData.products.topProducts)
            }

            Spacer(Modifier.height(PdvDimens.SpacingLarge))
            if (canViewFull) {
                PdvOutlinedButton(
                    onClick = { showFormatPicker = true },
                    enabled = !exportUiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
                ) {
                    Text(if (exportUiState.isLoading) "Exportando..." else "Exportar")
                }
                exportUiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                exportUiState.successMessage?.let { message ->
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(message, color = MaterialTheme.colorScheme.secondary)
                    exportUiState.shareUri?.let { shareUri ->
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        PdvTextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = exportUiState.mimeType ?: "*/*"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(shareUri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
                            }
                        ) {
                            Text("Compartilhar")
                        }
                    }
                }
                Spacer(Modifier.height(PdvDimens.SpacingMedium))
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar ao caixa")
        }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }

    if (showFormatPicker) {
        AlertDialog(
            onDismissRequest = { showFormatPicker = false },
            title = { Text("Exportar relatório") },
            text = {
                Column {
                    ExportFormat.entries.forEach { format ->
                        PdvTextButton(
                            onClick = {
                                showFormatPicker = false
                                onExport(format)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(exportFormatLabel(format))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                PdvTextButton(onClick = { showFormatPicker = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        content()
    }
}

@Composable
private fun TopProductsList(topProducts: List<TopProduct>) {
    Text("Top produtos mais vendidos", style = MaterialTheme.typography.titleLarge)
    if (topProducts.isEmpty()) {
        Text("Nenhuma venda no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        topProducts.forEachIndexed { index, product ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}. ${product.productName}")
                Text("${product.totalQuantity}x — ${formatCents(product.totalRevenueCents)}")
            }
        }
    }
}

@Composable
private fun DivergenceRow(session: CashSession) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Caixa #${session.id} (${session.operatorUsername})")
        Text(formatCents(session.divergenceCents ?: 0), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun LowStockRow(product: Product) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(product.name)
        Text("${product.stockQuantity} (alerta: ${product.minStockAlert})", color = MaterialTheme.colorScheme.error)
    }
}
