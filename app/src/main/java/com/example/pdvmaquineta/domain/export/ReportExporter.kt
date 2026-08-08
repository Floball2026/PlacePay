package com.example.pdvmaquineta.domain.export

import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.usecase.ReportData

sealed class ExportResult {
    data class Success(
        val fileName: String,
        val locationDescription: String,
        // String (não android.net.Uri) de propósito — mantém o domínio livre
        // de tipos do Android; quem precisa de um Uri de verdade (pra
        // compartilhar via Intent) reconstrói com Uri.parse na apresentação.
        val shareUri: String?,
        val mimeType: String
    ) : ExportResult()

    data class Failure(val reason: String) : ExportResult()
}

// Abstração sobre a geração/gravação do arquivo de exportação. Diferente de
// PaymentGateway/ReceiptPrinter/DigitalReceiptSender, aqui não há mock: CSV,
// PDF (android.graphics.pdf.PdfDocument) e Excel (org.dhatim:fastexcel) são
// implementáveis de verdade sem depender de SDK/provedor externo ainda não
// escolhido.
interface ReportExporter {
    suspend fun export(report: ReportData, format: ExportFormat): ExportResult
}
