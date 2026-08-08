package com.example.pdvmaquineta.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.pdvmaquineta.domain.export.ExportResult
import com.example.pdvmaquineta.domain.export.ReportExporter
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.usecase.ReportData
import com.example.pdvmaquineta.domain.format.formatCents
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet

private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val labelDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())

private const val PDF_PAGE_WIDTH = 595
private const val PDF_PAGE_HEIGHT = 842
private const val PDF_MARGIN = 40f
private const val PDF_LINE_HEIGHT = 18f

private fun methodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Dinheiro"
    PaymentMethod.CREDIT_CARD -> "Cartão de crédito"
    PaymentMethod.DEBIT_CARD -> "Cartão de débito"
    PaymentMethod.PIX -> "Pix"
}

private fun periodLabel(report: ReportData): String {
    val from = report.fromMillis?.let { labelDateFormat.format(Date(it)) } ?: "início do histórico"
    val to = report.toMillis?.let { labelDateFormat.format(Date(it)) } ?: "hoje"
    return "$from até $to"
}

private fun ExportFormat.extension(): String = when (this) {
    ExportFormat.CSV -> "csv"
    ExportFormat.PDF -> "pdf"
    ExportFormat.EXCEL -> "xlsx"
}

private fun ExportFormat.mimeType(): String = when (this) {
    ExportFormat.CSV -> "text/csv"
    ExportFormat.PDF -> "application/pdf"
    ExportFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
}

// Implementação real (não mockada — CSV/PDF/Excel não dependem de SDK ou
// provedor externo ainda não escolhido, diferente de pagamento/impressão/
// envio digital). Arquivo salvo em Downloads via MediaStore (API 29+, sem
// precisar de permissão) pra ficar visível/encontrável fora do app; em
// versões mais antigas cai pro diretório externo próprio do app, exposto via
// FileProvider pra ainda permitir compartilhar.
class AndroidReportExporter @Inject constructor(
    @ApplicationContext private val context: Context
) : ReportExporter {

    override suspend fun export(report: ReportData, format: ExportFormat): ExportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = buildFileName(report, format)
            val bytes = when (format) {
                ExportFormat.CSV -> buildCsv(report).toByteArray(Charsets.UTF_8)
                ExportFormat.PDF -> buildPdf(report)
                ExportFormat.EXCEL -> buildExcel(report)
            }
            val (shareUri, locationDescription) = saveFile(fileName, format.mimeType(), bytes)
            ExportResult.Success(
                fileName = fileName,
                locationDescription = locationDescription,
                shareUri = shareUri,
                mimeType = format.mimeType()
            )
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "Erro desconhecido ao exportar")
        }
    }

    private fun buildFileName(report: ReportData, format: ExportFormat): String {
        val from = report.fromMillis?.let { fileDateFormat.format(Date(it)) } ?: "inicio"
        val to = report.toMillis?.let { fileDateFormat.format(Date(it)) } ?: fileDateFormat.format(Date())
        return "relatorio_${from}_$to.${format.extension()}"
    }

    private fun saveFile(fileName: String, mimeType: String, bytes: ByteArray): Pair<String?, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Não foi possível criar o arquivo em Downloads")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Não foi possível abrir o arquivo para escrita")
            return uri.toString() to "Downloads/$fileName"
        }

        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri.toString() to file.absolutePath
    }

    private fun buildCsv(report: ReportData): String = buildString {
        appendLine("Relatório PDV Maquineta")
        appendLine("Período;${periodLabel(report)}")
        appendLine()

        appendLine("VENDAS")
        appendLine("Total vendido;${formatCents(report.sales.totalSoldCents)}")
        appendLine("Vendas concluídas;${report.sales.completedSalesCount}")
        appendLine()
        appendLine("Forma de pagamento;Total")
        report.sales.totalsByMethod.forEach { (method, cents) ->
            appendLine("${methodLabel(method)};${formatCents(cents)}")
        }
        appendLine()
        appendLine("Top produtos mais vendidos;Quantidade;Faturamento")
        report.sales.topProducts.forEach { p ->
            appendLine("${p.productName};${p.totalQuantity};${formatCents(p.totalRevenueCents)}")
        }
        appendLine()

        appendLine("CAIXA")
        appendLine("Sessões fechadas;${report.cash.closedSessionsCount}")
        appendLine("Total de abertura;${formatCents(report.cash.totalOpeningCents)}")
        appendLine("Total informado no fechamento;${formatCents(report.cash.totalInformedCents)}")
        appendLine("Sangrias;${formatCents(report.cash.totalWithdrawalCents)}")
        appendLine("Suprimentos;${formatCents(report.cash.totalSupplyCents)}")
        appendLine()
        appendLine("Divergências;Caixa;Operador;Valor")
        report.cash.divergentSessions.forEach { session ->
            appendLine("Divergência;#${session.id};${session.operatorUsername};${formatCents(session.divergenceCents ?: 0)}")
        }
        appendLine()

        appendLine("PRODUTOS")
        appendLine("Estoque baixo;Produto;Estoque atual;Alerta mínimo")
        report.products.lowStockProducts.forEach { product ->
            appendLine("Estoque baixo;${product.name};${product.stockQuantity};${product.minStockAlert}")
        }
    }

    private fun buildPdf(report: ReportData): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val sectionPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        var y = PDF_MARGIN

        fun writeLine(text: String, paint: Paint) {
            if (y + PDF_LINE_HEIGHT > PDF_PAGE_HEIGHT - PDF_MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = PDF_MARGIN
            }
            canvas.drawText(text, PDF_MARGIN, y, paint)
            y += PDF_LINE_HEIGHT
        }

        writeLine("Relatório PDV Maquineta", titlePaint)
        writeLine("Período: ${periodLabel(report)}", bodyPaint)
        y += PDF_LINE_HEIGHT / 2

        writeLine("Vendas", sectionPaint)
        writeLine("Total vendido: ${formatCents(report.sales.totalSoldCents)}", bodyPaint)
        writeLine("Vendas concluídas: ${report.sales.completedSalesCount}", bodyPaint)
        report.sales.totalsByMethod.forEach { (method, cents) ->
            writeLine("${methodLabel(method)}: ${formatCents(cents)}", bodyPaint)
        }
        writeLine("Top produtos mais vendidos:", bodyPaint)
        report.sales.topProducts.forEachIndexed { index, p ->
            writeLine("  ${index + 1}. ${p.productName} — ${p.totalQuantity}x — ${formatCents(p.totalRevenueCents)}", bodyPaint)
        }
        y += PDF_LINE_HEIGHT / 2

        writeLine("Caixa", sectionPaint)
        writeLine("Sessões fechadas: ${report.cash.closedSessionsCount}", bodyPaint)
        writeLine("Total de abertura: ${formatCents(report.cash.totalOpeningCents)}", bodyPaint)
        writeLine("Total informado no fechamento: ${formatCents(report.cash.totalInformedCents)}", bodyPaint)
        writeLine("Sangrias: ${formatCents(report.cash.totalWithdrawalCents)}", bodyPaint)
        writeLine("Suprimentos: ${formatCents(report.cash.totalSupplyCents)}", bodyPaint)
        writeLine("Divergências identificadas:", bodyPaint)
        if (report.cash.divergentSessions.isEmpty()) {
            writeLine("  Nenhuma divergência no período.", bodyPaint)
        } else {
            report.cash.divergentSessions.forEach { session ->
                writeLine(
                    "  Caixa #${session.id} (${session.operatorUsername}): ${formatCents(session.divergenceCents ?: 0)}",
                    bodyPaint
                )
            }
        }
        y += PDF_LINE_HEIGHT / 2

        writeLine("Produtos", sectionPaint)
        writeLine("Estoque baixo agora:", bodyPaint)
        if (report.products.lowStockProducts.isEmpty()) {
            writeLine("  Nenhum produto com estoque baixo.", bodyPaint)
        } else {
            report.products.lowStockProducts.forEach { product ->
                writeLine("  ${product.name}: ${product.stockQuantity} (alerta: ${product.minStockAlert})", bodyPaint)
            }
        }

        document.finishPage(page)
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun buildExcel(report: ReportData): ByteArray {
        val output = ByteArrayOutputStream()
        val workbook = Workbook(output, "PDVMaquineta", "1.0")
        writeSalesSheet(workbook.newWorksheet("Vendas"), report)
        writeCashSheet(workbook.newWorksheet("Caixa"), report)
        writeProductsSheet(workbook.newWorksheet("Produtos"), report)
        workbook.finish()
        return output.toByteArray()
    }

    private fun writeSalesSheet(sheet: Worksheet, report: ReportData) {
        var row = 0
        sheet.value(row, 0, "Período"); sheet.value(row, 1, periodLabel(report)); row += 2
        sheet.value(row, 0, "Total vendido"); sheet.value(row, 1, formatCents(report.sales.totalSoldCents)); row++
        sheet.value(row, 0, "Vendas concluídas"); sheet.value(row, 1, report.sales.completedSalesCount); row += 2

        sheet.value(row, 0, "Forma de pagamento"); sheet.value(row, 1, "Total"); row++
        report.sales.totalsByMethod.forEach { (method, cents) ->
            sheet.value(row, 0, methodLabel(method)); sheet.value(row, 1, formatCents(cents)); row++
        }
        row++

        sheet.value(row, 0, "Produto"); sheet.value(row, 1, "Quantidade"); sheet.value(row, 2, "Faturamento"); row++
        writeTopProducts(sheet, report.sales.topProducts, row)
    }

    private fun writeCashSheet(sheet: Worksheet, report: ReportData) {
        var row = 0
        sheet.value(row, 0, "Sessões fechadas"); sheet.value(row, 1, report.cash.closedSessionsCount); row++
        sheet.value(row, 0, "Total de abertura"); sheet.value(row, 1, formatCents(report.cash.totalOpeningCents)); row++
        sheet.value(row, 0, "Total informado no fechamento")
        sheet.value(row, 1, formatCents(report.cash.totalInformedCents)); row++
        sheet.value(row, 0, "Sangrias"); sheet.value(row, 1, formatCents(report.cash.totalWithdrawalCents)); row++
        sheet.value(row, 0, "Suprimentos"); sheet.value(row, 1, formatCents(report.cash.totalSupplyCents)); row += 2

        sheet.value(row, 0, "Caixa"); sheet.value(row, 1, "Operador"); sheet.value(row, 2, "Divergência"); row++
        report.cash.divergentSessions.forEach { session ->
            sheet.value(row, 0, "#${session.id}")
            sheet.value(row, 1, session.operatorUsername)
            sheet.value(row, 2, formatCents(session.divergenceCents ?: 0))
            row++
        }
    }

    private fun writeProductsSheet(sheet: Worksheet, report: ReportData) {
        var row = 0
        sheet.value(row, 0, "Produto"); sheet.value(row, 1, "Estoque atual"); sheet.value(row, 2, "Alerta mínimo"); row++
        report.products.lowStockProducts.forEach { product: Product ->
            sheet.value(row, 0, product.name)
            sheet.value(row, 1, product.stockQuantity)
            sheet.value(row, 2, product.minStockAlert ?: 0)
            row++
        }
        row++

        sheet.value(row, 0, "Produto"); sheet.value(row, 1, "Quantidade"); sheet.value(row, 2, "Faturamento"); row++
        writeTopProducts(sheet, report.products.topProducts, row)
    }

    private fun writeTopProducts(sheet: Worksheet, topProducts: List<TopProduct>, startRow: Int) {
        var row = startRow
        topProducts.forEach { p ->
            sheet.value(row, 0, p.productName)
            sheet.value(row, 1, p.totalQuantity)
            sheet.value(row, 2, formatCents(p.totalRevenueCents))
            row++
        }
    }
}
