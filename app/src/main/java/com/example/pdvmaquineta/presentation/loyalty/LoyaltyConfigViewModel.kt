package com.example.pdvmaquineta.presentation.loyalty

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.usecase.ObserveLoyaltyConfigUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateLoyaltyConfigResult
import com.example.pdvmaquineta.domain.usecase.UpdateLoyaltyConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoyaltyConfigUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class PendingLoyaltyChange(
    val mode: LoyaltyMode,
    val pointsPerCurrencyUnit: Double?,
    val pointValueInCents: Long?,
    val visitsRequired: Int?,
    val discountPercentOnReward: Int?
)

@HiltViewModel
class LoyaltyConfigViewModel @Inject constructor(
    observeLoyaltyConfigUseCase: ObserveLoyaltyConfigUseCase,
    private val updateLoyaltyConfigUseCase: UpdateLoyaltyConfigUseCase
) : ViewModel() {

    val activeConfig: StateFlow<LoyaltyConfig?> = observeLoyaltyConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var uiState by mutableStateOf(LoyaltyConfigUiState())
        private set

    var pendingChange by mutableStateOf<PendingLoyaltyChange?>(null)
        private set

    fun requestChange(
        mode: LoyaltyMode,
        pointsPerCurrencyUnit: Double?,
        pointValueInCents: Long?,
        visitsRequired: Int?,
        discountPercentOnReward: Int?
    ) {
        pendingChange = PendingLoyaltyChange(
            mode, pointsPerCurrencyUnit, pointValueInCents, visitsRequired, discountPercentOnReward
        )
    }

    fun dismissChangeConfirmation() {
        pendingChange = null
        uiState = LoyaltyConfigUiState()
    }

    fun confirmChange() {
        val change = pendingChange ?: return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (
                updateLoyaltyConfigUseCase(
                    change.mode,
                    change.pointsPerCurrencyUnit,
                    change.pointValueInCents,
                    change.visitsRequired,
                    change.discountPercentOnReward
                )
            ) {
                is UpdateLoyaltyConfigResult.Success -> {
                    uiState = LoyaltyConfigUiState()
                    pendingChange = null
                }
                UpdateLoyaltyConfigResult.NotAuthorized ->
                    uiState = uiState.copy(isLoading = false, errorMessage = "Sem permissão para essa ação")
                UpdateLoyaltyConfigResult.InvalidData ->
                    uiState = uiState.copy(isLoading = false, errorMessage = "Parâmetros inválidos")
            }
        }
    }
}
