package com.example.pdvmaquineta.presentation.cash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.usecase.CashOverview
import com.example.pdvmaquineta.domain.usecase.CloseCashSessionUseCase
import com.example.pdvmaquineta.domain.usecase.GetCashOverviewUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveOpenCashSessionUseCase
import com.example.pdvmaquineta.domain.usecase.OpenCashSessionUseCase
import com.example.pdvmaquineta.domain.usecase.RegisterCashMovementResult
import com.example.pdvmaquineta.domain.usecase.RegisterCashMovementUseCase
import com.example.pdvmaquineta.domain.usecase.SupervisorAuthorization
import com.example.pdvmaquineta.domain.usecase.SupervisorAuthorizationResult
import com.example.pdvmaquineta.domain.usecase.AuthorizeWithSupervisorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CashRoute {
    data object Dashboard : CashRoute()
    data class Movement(val type: CashMovementType) : CashRoute()
    data object Close : CashRoute()
    data object Sale : CashRoute()
    data object Products : CashRoute()
    data object Users : CashRoute()
    data object Customers : CashRoute()
    data object Loyalty : CashRoute()
    data object SaleHistory : CashRoute()
    data object Terminal : CashRoute()
    data object BusinessConfig : CashRoute()
    data object Reports : CashRoute()
}

data class OpenCashUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class MovementUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authorizationRequired: Boolean = false,
    val authorization: SupervisorAuthorization? = null
)

data class CloseCashUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CashViewModel @Inject constructor(
    observeOpenCashSessionUseCase: ObserveOpenCashSessionUseCase,
    private val getCashOverviewUseCase: GetCashOverviewUseCase,
    private val openCashSessionUseCase: OpenCashSessionUseCase,
    private val closeCashSessionUseCase: CloseCashSessionUseCase,
    private val registerCashMovementUseCase: RegisterCashMovementUseCase,
    private val authorizeWithSupervisorUseCase: AuthorizeWithSupervisorUseCase
) : ViewModel() {

    val openSession: StateFlow<CashSession?> = observeOpenCashSessionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var overview by mutableStateOf<CashOverview?>(null)
        private set

    var route by mutableStateOf<CashRoute>(CashRoute.Dashboard)
        private set

    var openCashUiState by mutableStateOf(OpenCashUiState())
        private set

    var movementUiState by mutableStateOf(MovementUiState())
        private set

    var closeCashUiState by mutableStateOf(CloseCashUiState())
        private set

    init {
        viewModelScope.launch {
            openSession.collect { session ->
                overview = session?.let { getCashOverviewUseCase(it) }
                if (session == null) route = CashRoute.Dashboard
            }
        }
    }

    fun navigateTo(newRoute: CashRoute) {
        route = newRoute
        movementUiState = MovementUiState()
        closeCashUiState = CloseCashUiState()
    }

    fun openCash(openingBalanceCents: Long) {
        viewModelScope.launch {
            openCashUiState = openCashUiState.copy(isLoading = true, errorMessage = null)
            val session = openCashSessionUseCase(openingBalanceCents)
            openCashUiState = if (session != null) {
                OpenCashUiState()
            } else {
                openCashUiState.copy(isLoading = false, errorMessage = "Não foi possível abrir o caixa")
            }
        }
    }

    fun authorizeSupervisor(permission: Permission, username: String, password: String) {
        viewModelScope.launch {
            when (val result = authorizeWithSupervisorUseCase(permission, username, password)) {
                is SupervisorAuthorizationResult.Authorized ->
                    movementUiState = movementUiState.copy(
                        authorizationRequired = false,
                        authorization = result.authorization,
                        errorMessage = null
                    )
                SupervisorAuthorizationResult.InvalidCredentials ->
                    movementUiState = movementUiState.copy(errorMessage = "Credenciais de supervisor inválidas")
                SupervisorAuthorizationResult.InsufficientPermission ->
                    movementUiState = movementUiState.copy(errorMessage = "Esse usuário não tem permissão para autorizar")
            }
        }
    }

    fun dismissAuthorizationPrompt() {
        movementUiState = movementUiState.copy(authorizationRequired = false, errorMessage = null)
    }

    fun registerMovement(type: CashMovementType, amountCents: Long, reason: String) {
        val session = openSession.value ?: return
        viewModelScope.launch {
            movementUiState = movementUiState.copy(isLoading = true, errorMessage = null)
            when (
                val result = registerCashMovementUseCase(
                    session, type, amountCents, reason, movementUiState.authorization
                )
            ) {
                is RegisterCashMovementResult.Success -> {
                    overview = getCashOverviewUseCase(session)
                    movementUiState = MovementUiState()
                    route = CashRoute.Dashboard
                }
                RegisterCashMovementResult.AuthorizationRequired ->
                    movementUiState = movementUiState.copy(isLoading = false, authorizationRequired = true)
                RegisterCashMovementResult.NotLoggedIn ->
                    movementUiState = movementUiState.copy(isLoading = false, errorMessage = "Sessão inválida")
            }
        }
    }

    fun closeCash(informedCashCents: Long) {
        val session = openSession.value ?: return
        viewModelScope.launch {
            closeCashUiState = closeCashUiState.copy(isLoading = true, errorMessage = null)
            closeCashSessionUseCase(session, informedCashCents)
            closeCashUiState = CloseCashUiState()
            route = CashRoute.Dashboard
        }
    }
}
