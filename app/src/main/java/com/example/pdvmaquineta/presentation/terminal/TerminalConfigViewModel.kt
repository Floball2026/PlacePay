package com.example.pdvmaquineta.presentation.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.PrinterType
import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.model.TerminalEnvironment
import com.example.pdvmaquineta.domain.usecase.ObserveTerminalConfigUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateTerminalConfigResult
import com.example.pdvmaquineta.domain.usecase.UpdateTerminalConfigUseCase
import com.example.pdvmaquineta.data.sync.DeviceInfoProvider
import com.example.pdvmaquineta.data.sync.SyncRepository
import com.example.pdvmaquineta.data.sync.SyncSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TerminalConfigUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ActivationUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class TerminalConfigViewModel @Inject constructor(
    observeTerminalConfigUseCase: ObserveTerminalConfigUseCase,
    private val updateTerminalConfigUseCase: UpdateTerminalConfigUseCase,
    private val syncRepository: SyncRepository,
    private val syncSettings: SyncSettings,
    private val deviceInfo: DeviceInfoProvider
) : ViewModel() {

    // Checagem de versao minima: o heartbeat guarda min_app_version_code; se a
    // versao instalada for menor, o app esta desatualizado (aviso nao-bloqueante).
    private val currentVersion: Int = deviceInfo.build().appVersionCode ?: 0
    val currentVersionCode: Int get() = currentVersion
    val minAppVersionCode: Int get() = syncSettings.minAppVersionCode
    val isOutdated: Boolean get() = minAppVersionCode > 0 && currentVersion < minAppVersionCode

    val config: StateFlow<TerminalConfig?> = observeTerminalConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var uiState by mutableStateOf(TerminalConfigUiState())
        private set

    var activationState by mutableStateOf(ActivationUiState())
        private set

    var isActivated by mutableStateOf(syncSettings.isActivated)
        private set

    val currentBaseUrl: String get() = syncSettings.baseUrl
    val terminalId: String get() = syncSettings.terminalId

    fun activate(baseUrl: String, activationCode: String) {
        if (baseUrl.isBlank() || activationCode.isBlank()) {
            activationState = ActivationUiState(error = "Informe a URL do servidor e o código de ativação")
            return
        }
        viewModelScope.launch {
            activationState = ActivationUiState(isLoading = true)
            syncRepository.activate(baseUrl, activationCode)
                .onSuccess { tid ->
                    isActivated = true
                    activationState = ActivationUiState(message = "Terminal ativado! ID: $tid")
                }
                .onFailure { e ->
                    activationState = ActivationUiState(error = e.message ?: "Falha na ativação")
                }
        }
    }

    fun save(
        terminalName: String,
        storeName: String,
        environment: TerminalEnvironment,
        printerType: PrinterType
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (updateTerminalConfigUseCase(terminalName, storeName, environment, printerType)) {
                is UpdateTerminalConfigResult.Success -> uiState = TerminalConfigUiState(successMessage = "Configuração salva com sucesso")
                UpdateTerminalConfigResult.NotAuthorized ->
                    uiState = uiState.copy(isLoading = false, errorMessage = "Sem permissão para essa ação")
                UpdateTerminalConfigResult.InvalidData ->
                    uiState = uiState.copy(isLoading = false, errorMessage = "Preencha nome do terminal e da loja")
            }
        }
    }
}
