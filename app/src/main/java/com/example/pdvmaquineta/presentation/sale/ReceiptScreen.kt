package com.example.pdvmaquineta.presentation.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.receipt.ReceiptChannel
import com.example.pdvmaquineta.domain.usecase.CartOverview
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun methodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Dinheiro"
    PaymentMethod.CREDIT_CARD -> "Cartão de crédito"
    PaymentMethod.DEBIT_CARD -> "Cartão de débito"
    PaymentMethod.PIX -> "Pix"
}

private fun channelLabel(channel: ReceiptChannel): String = when (channel) {
    ReceiptChannel.WHATSAPP -> "WhatsApp"
    ReceiptChannel.SMS -> "SMS"
    ReceiptChannel.EMAIL -> "E-mail"
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.Builder().setLanguage("pt").setRegion("BR").build())

// Reusada tanto no fluxo pós-venda (Fase 4) quanto na reimpressão a partir do
// histórico (Fase 6/RF-032) — quem chama decide o rótulo/ação do botão
// inferior (nova venda vs. voltar ao histórico).
@Composable
fun ReceiptScreen(
    sale: CartOverview,
    payment: Payment,
    printUiState: ReceiptActionUiState,
    sendUiState: ReceiptActionUiState,
    onPrint: () -> Unit,
    onSendDigitally: (ReceiptChannel) -> Unit,
    bottomActionLabel: String,
    onBottomAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChannelPicker by remember { mutableStateOf(false) }
    var showEmailInput by remember { mutableStateOf(false) }
    var emailText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        Text(
            text = "Pagamento aprovado",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaperColor, contentColor = PaperInk),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, Color(0x22000000))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PdvDimens.SpacingLarge, vertical = PdvDimens.SpacingMedium)
            ) {
                Text(
                    (sale.sale.storeNameSnapshot ?: "MERCADO").uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                sale.sale.terminalNameSnapshot?.let {
                    Text(
                        "Terminal: $it",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    "CUPOM NAO FISCAL",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                ReceiptDashedLine()

                ReceiptLine("Venda", "#${sale.sale.id}")
                ReceiptLine("Data", dateFormat.format(Date(sale.sale.createdAt)))
                ReceiptLine("Operador", sale.sale.operatorUsername)

                ReceiptDashedLine()

                sale.items.forEach { item ->
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "  ${item.quantity} x ${formatCents(item.unitPriceCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            formatCents(item.unitPriceCents * item.quantity),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                ReceiptDashedLine()

                val subtotalCents = sale.items.sumOf { it.unitPriceCents * it.quantity }
                ReceiptLine("Subtotal", formatCents(subtotalCents))
                if (sale.sale.discountPercent > 0) {
                    ReceiptLine("Desconto (${sale.sale.discountPercent}%)", "-${formatCents(sale.discountCents)}")
                }
                if (sale.loyaltyDiscountCents > 0) {
                    ReceiptLine("Fidelidade", "-${formatCents(sale.loyaltyDiscountCents)}")
                }

                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TOTAL",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatCents(sale.totalCents),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                ReceiptDashedLine()

                ReceiptLine("Pagamento", methodLabel(payment.method))
                if (payment.method == PaymentMethod.CASH) {
                    ReceiptLine("Recebido", formatCents(payment.receivedCents ?: 0))
                    ReceiptLine("Troco", formatCents(payment.changeCents ?: 0))
                }

                ReceiptDashedLine()

                Text(
                    "Obrigado pela preferencia!",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvOutlinedButton(
            onClick = onPrint,
            enabled = !printUiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Imprimir")
        }
        printUiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvOutlinedButton(
            onClick = { showChannelPicker = true },
            enabled = !sendUiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Enviar")
        }
        sendUiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvButton(
            onClick = onBottomAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(bottomActionLabel)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }

    if (showChannelPicker) {
        AlertDialog(
            onDismissRequest = { showChannelPicker = false },
            title = { Text("Enviar comprovante") },
            text = {
                Column {
                    ReceiptChannel.entries.forEach { channel ->
                        PdvTextButton(
                            onClick = {
                                showChannelPicker = false
                                if (channel == ReceiptChannel.EMAIL) {
                                    showEmailInput = true
                                } else {
                                    onSendDigitally(channel)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(channelLabel(channel))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                PdvTextButton(onClick = { showChannelPicker = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEmailInput) {
        AlertDialog(
            onDismissRequest = { showEmailInput = false },
            title = { Text("Enviar por e-mail") },
            text = {
                Column {
                    Text(
                        "Informe o e-mail do cliente. Vamos abrir seu app de e-mail com o recibo pronto para enviar.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                PdvButton(
                    onClick = {
                        val email = emailText.trim()
                        val body = buildReceiptText(sale, payment)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                            putExtra(Intent.EXTRA_SUBJECT, "Comprovante - Venda #${sale.sale.id}")
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Enviar recibo"))
                            onSendDigitally(ReceiptChannel.EMAIL)
                            showEmailInput = false
                            emailText = ""
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Nenhum app de e-mail encontrado neste aparelho.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = emailText.isNotBlank()
                ) { Text("Enviar") }
            },
            dismissButton = {
                PdvTextButton(onClick = { showEmailInput = false }) { Text("Cancelar") }
            }
        )
    }
}

private val PaperColor = Color(0xFFFFFFFF)
private val PaperInk = Color(0xFF1B1B1B)

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ReceiptDashedLine() {
    Text(
        "-".repeat(60),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

private fun buildReceiptText(sale: CartOverview, payment: Payment): String {
    val sb = StringBuilder()
    sb.appendLine(sale.sale.storeNameSnapshot ?: "MERCADO")
    sale.sale.terminalNameSnapshot?.let { sb.appendLine("Terminal: $it") }
    sb.appendLine("CUPOM NAO FISCAL")
    sb.appendLine("--------------------------------")
    sb.appendLine("Venda #${sale.sale.id}")
    sb.appendLine("Data: ${dateFormat.format(Date(sale.sale.createdAt))}")
    sb.appendLine("Operador: ${sale.sale.operatorUsername}")
    sb.appendLine("--------------------------------")
    sale.items.forEach { item ->
        sb.appendLine(item.productName)
        sb.appendLine("  ${item.quantity} x ${formatCents(item.unitPriceCents)}   ${formatCents(item.unitPriceCents * item.quantity)}")
    }
    sb.appendLine("--------------------------------")
    val subtotal = sale.items.sumOf { it.unitPriceCents * it.quantity }
    sb.appendLine("Subtotal: ${formatCents(subtotal)}")
    if (sale.sale.discountPercent > 0) {
        sb.appendLine("Desconto (${sale.sale.discountPercent}%): -${formatCents(sale.discountCents)}")
    }
    if (sale.loyaltyDiscountCents > 0) {
        sb.appendLine("Fidelidade: -${formatCents(sale.loyaltyDiscountCents)}")
    }
    sb.appendLine("TOTAL: ${formatCents(sale.totalCents)}")
    sb.appendLine("--------------------------------")
    sb.appendLine("Pagamento: ${methodLabel(payment.method)}")
    if (payment.method == PaymentMethod.CASH) {
        sb.appendLine("Recebido: ${formatCents(payment.receivedCents ?: 0)}")
        sb.appendLine("Troco: ${formatCents(payment.changeCents ?: 0)}")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("Obrigado pela preferencia!")
    return sb.toString()
}
