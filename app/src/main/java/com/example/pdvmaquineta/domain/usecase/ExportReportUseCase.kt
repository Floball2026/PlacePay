package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.export.ExportResult
import com.example.pdvmaquineta.domain.export.ReportExporter
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class ExportReportResult {
    data class Success(
        val fileName: String,
        val locationDescription: String,
        val shareUri: String?,
        val mimeType: String
    ) : ExportReportResult()

    data object NotAuthorized : ExportReportResult()
    data class Failure(val reason: String) : ExportReportResult()
}

private val auditDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())

// Exportação (Fase 7c) continua restrita a VIEW_FULL_REPORTS (Admin) — mesma
// regra já aplicada em GetReportUseCase pro botão de exportação em si.
class ExportReportUseCase @Inject constructor(
    private val reportExporter: ReportExporter,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(report: ReportData, format: ExportFormat): ExportReportResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return ExportReportResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.VIEW_FULL_REPORTS)) {
            return ExportReportResult.NotAuthorized
        }

        val result = reportExporter.export(report, format)

        val from = report.fromMillis?.let { auditDateFormat.format(Date(it)) } ?: "início do histórico"
        val to = report.toMillis?.let { auditDateFormat.format(Date(it)) } ?: "hoje"
        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.REPORT_EXPORTED,
                detail = "Formato: ${format.name}; Período: $from até $to",
                success = result is ExportResult.Success
            )
        )

        return when (result) {
            is ExportResult.Success ->
                ExportReportResult.Success(result.fileName, result.locationDescription, result.shareUri, result.mimeType)
            is ExportResult.Failure -> ExportReportResult.Failure(result.reason)
        }
    }
}
