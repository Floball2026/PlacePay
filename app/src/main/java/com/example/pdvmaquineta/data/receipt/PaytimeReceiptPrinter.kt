package com.example.pdvmaquineta.data.receipt

import android.content.Context
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.receipt.ReceiptPrintResult
import com.example.pdvmaquineta.domain.receipt.ReceiptPrinter
import com.example.pdvmaquineta.domain.usecase.CartOverview
import com.paytime.payossdk.PayOsSdkPrinter
import com.paytime.payossdk.external.model.printer.PayOsSdkPrinterStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Impressora real da maquineta (PAX A960) via SmartPOS SDK da PayTime
// (PayOsSdkPrinter). Substitui o MockReceiptPrinter — o resto do app nao muda,
// so a implementacao desta interface. Imprime o cupom nao-fiscal da venda.
@Singleton
class PaytimeReceiptPrinter @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptPrinter {

    private val printer = PayOsSdkPrinter.instance

    @Volatile
    private var configured = false

    private fun ensureConfigured() {
        if (!configured) {
            printer.configure(context)
            configured = true
        }
    }

    override suspend fun print(cart: CartOverview, payment: Payment): ReceiptPrintResult =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureConfigured()
                printer.init()
                printer.setGray(3)
                for (line in buildReceiptLines(cart, payment)) {
                    printer.printStr(line + "\n", "utf-8")
                }
                printer.step(120)
                val result = printer.start()
                val status = result.getOrNull()
                if (result.isSuccess && (status == null || status == PayOsSdkPrinterStatus.SUCCESS)) {
                    ReceiptPrintResult.Success
                } else {
                    val reason = status?.name ?: result.exceptionOrNull()?.message ?: "erro desconhecido"
                    ReceiptPrintResult.Failure("Impressora: $reason")
                }
            }.getOrElse { e ->
                ReceiptPrintResult.Failure(e.message ?: "Falha na impressao")
            }
        }

    private val cols = 32

    private fun buildReceiptLines(cart: CartOverview, payment: Payment): List<String> {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val lines = mutableListOf<String>()
        lines += center("PLACE PAY")
        lines += center("Cupom nao fiscal")
        lines += "-".repeat(cols)
        for (item in cart.items) {
            lines += "${item.quantity}x ${item.productName}".take(cols)
            lines += pair("", item.unitPriceCents * item.quantity)
        }
        lines += "-".repeat(cols)
        lines += pair("TOTAL", cart.totalCents)
        lines += "Pagamento: ${methodLabel(payment.method)}"
        payment.receivedCents?.let { lines += pair("Recebido", it) }
        payment.changeCents?.takeIf { it > 0 }?.let { lines += pair("Troco", it) }
        lines += "-".repeat(cols)
        lines += df.format(Date(payment.createdAt))
        lines += ""
        lines += center("Obrigado pela preferencia!")
        return lines
    }

    private fun money(cents: Long): String = "R$ %.2f".format(Locale("pt", "BR"), cents / 100.0)

    private fun methodLabel(m: PaymentMethod): String = when (m) {
        PaymentMethod.CASH -> "Dinheiro"
        PaymentMethod.CREDIT_CARD -> "Credito"
        PaymentMethod.DEBIT_CARD -> "Debito"
        PaymentMethod.PIX -> "PIX"
    }

    private fun center(s: String): String {
        if (s.length >= cols) return s.take(cols)
        val pad = (cols - s.length) / 2
        return " ".repeat(pad) + s
    }

    private fun pair(label: String, cents: Long): String {
        val v = money(cents)
        val space = (cols - label.length - v.length).coerceAtLeast(1)
        return label + " ".repeat(space) + v
    }
}
