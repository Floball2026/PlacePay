package com.example.pdvmaquineta.presentation.reports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.usecase.ExportReportResult
import com.example.pdvmaquineta.domain.usecase.ExportReportUseCase
import com.example.pdvmaquineta.domain.usecase.GetReportResult
import com.example.pdvmaquineta.domain.usecase.GetReportUseCase
import com.example.pdvmaquineta.domain.usecase.ReportData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class ReportsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ExportUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shareUri: String? = null,
    val mimeType: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getReportUseCase: GetReportUseCase,
    private val exportReportUseCase: ExportReportUseCase
) : ViewModel() {

    var uiState by mutableStateOf(ReportsUiState())
        private set

    var reportData by mutableStateOf<ReportData?>(null)
        private set

    var exportUiState by mutableStateOf(ExportUiState())
        private set

    private var requestedFromMillis: Long? = null
    private var requestedToMillis: Long? = null

    init {
        load()
    }

    // fromMillis/toMillis nulos = sem filtro pedido (o use case ainda impõe o
    // limite de 30 dias por conta própria se o perfil só tiver
    // VIEW_OPERATIONAL_REPORTS).
    fun setDateFilter(fromMillis: Long?, toMillis: Long?) {
        requestedFromMillis = fromMillis
        requestedToMillis = toMillis
        load()
    }

    private fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (val result = getReportUseCase(requestedFromMillis, requestedToMillis)) {
                is GetReportResult.Success -> {
                    reportData = result.data
                    uiState = ReportsUiState()
                }
                GetReportResult.NotAuthorized ->
                    uiState = ReportsUiState(errorMessage = "Sem permissão para ver relatórios")
            }
        }
    }

    fun exportReport(format: ExportFormat) {
        val data = reportData ?: return
        viewModelScope.launch {
            exportUiState = ExportUiState(isLoading = true)
            exportUiState = when (val result = exportReportUseCase(data, format)) {
                is ExportReportResult.Success -> ExportUiState(
                    successMessage = "Salvo em: ${result.locationDescription}",
                    shareUri = result.shareUri,
                    mimeType = result.mimeType
                )
                ExportReportResult.NotAuthorized ->
                    ExportUiState(errorMessage = "Sem permissão para exportar")
                is ExportReportResult.Failure ->
                    ExportUiState(errorMessage = result.reason)
            }
        }
    }

    fun dismissExportResult() {
        exportUiState = ExportUiState()
    }
}
