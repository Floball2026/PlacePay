package com.example.pdvmaquineta.presentation.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.ThemeTone
import com.example.pdvmaquineta.data.sync.SyncRepository
import com.example.pdvmaquineta.data.sync.SyncSettings
import com.example.pdvmaquineta.domain.session.SessionManager
import com.example.pdvmaquineta.domain.usecase.CheckHasPinUseCase
import com.example.pdvmaquineta.domain.usecase.LockSessionUseCase
import com.example.pdvmaquineta.domain.usecase.LoginUseCase
import com.example.pdvmaquineta.domain.usecase.LoginWithPinUseCase
import com.example.pdvmaquineta.domain.usecase.LogoutUseCase
import com.example.pdvmaquineta.domain.usecase.SetPinResult
import com.example.pdvmaquineta.domain.usecase.SetPinUseCase
import com.example.pdvmaquineta.domain.usecase.UnlockResult
import com.example.pdvmaquineta.domain.usecase.UnlockSessionUseCase
import com.example.pdvmaquineta.domain.usecase.UnlockWithPinUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateThemeToneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LoginMethod { PASSWORD, PIN }

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class UnlockUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ChangePinUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val loginUseCase: LoginUseCase,
    private val loginWithPinUseCase: LoginWithPinUseCase,
    private val checkHasPinUseCase: CheckHasPinUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val lockSessionUseCase: LockSessionUseCase,
    private val unlockSessionUseCase: UnlockSessionUseCase,
    private val unlockWithPinUseCase: UnlockWithPinUseCase,
    private val setPinUseCase: SetPinUseCase,
    private val updateThemeToneUseCase: UpdateThemeToneUseCase,
    private val syncRepository: SyncRepository,
    private val syncSettings: SyncSettings
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionManager.state

    // Login/bloqueio sempre usam o tom padrão — o tom do usuário só se aplica
    // depois que a sessão é iniciada.
    val themeTone: StateFlow<ThemeTone> = sessionState
        .map { state -> (state as? SessionState.Active)?.user?.themeTone ?: ThemeTone.BRAND_LIGHT }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeTone.BRAND_LIGHT)

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    var unlockUiState by mutableStateOf(UnlockUiState())
        private set

    var changePinUiState by mutableStateOf(ChangePinUiState())
        private set

    var loginMethod by mutableStateOf(LoginMethod.PASSWORD)
        private set

    // Derivado do usuário travado, não precisa ser digitado de novo — o
    // desbloqueio usa o mesmo método (senha/PIN) que o login desse usuário.
    val unlockMethod: StateFlow<LoginMethod> = sessionState
        .flatMapLatest { state ->
            if (state is SessionState.Locked) {
                flow { emit(if (checkHasPinUseCase(state.user.username)) LoginMethod.PIN else LoginMethod.PASSWORD) }
            } else {
                flowOf(LoginMethod.PASSWORD)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoginMethod.PASSWORD)

    // Guarda o ultimo usuario digitado para reavaliar o metodo (PIN/senha)
    // depois que a sincronizacao de operadores termina — assim, se o operador
    // ainda nao estava no terminal quando o nome foi digitado, o campo troca
    // pra PIN sozinho quando ele chega.
    private var lastUsername: String = ""

    // Sincroniza operadores e clientes assim que a tela de login aparece
    // (estado LoggedOut), se o terminal estiver ativado. Assim o operador
    // cadastrado/alterado na web ja aparece pra logar SEM precisar de Carga PLU
    // nem reabrir o app. Sync incremental (?since=), entao e leve.
    init {
        viewModelScope.launch {
            sessionState.collect { state ->
                if (state is SessionState.LoggedOut && syncSettings.isActivated) {
                    launch {
                        runCatching { syncRepository.pullOperators() }
                        runCatching { syncRepository.pullCustomers() }
                        evaluateLoginMethod(lastUsername)
                    }
                }
            }
        }
    }

    private suspend fun evaluateLoginMethod(username: String) {
        loginMethod = if (username.isNotBlank() && checkHasPinUseCase(username)) {
            LoginMethod.PIN
        } else {
            LoginMethod.PASSWORD
        }
    }

    fun onUsernameChange(username: String) {
        lastUsername = username
        viewModelScope.launch { evaluateLoginMethod(username) }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)
            loginUiState = when (loginUseCase(username, password)) {
                is CredentialResult.Success -> LoginUiState()
                CredentialResult.InvalidCredentials ->
                    loginUiState.copy(isLoading = false, errorMessage = "Usuário ou senha inválidos")
                CredentialResult.UserInactive ->
                    loginUiState.copy(isLoading = false, errorMessage = "Usuário inativo")
            }
        }
    }

    fun loginWithPin(username: String, pin: String) {
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)
            loginUiState = when (loginWithPinUseCase(username, pin)) {
                is CredentialResult.Success -> LoginUiState()
                CredentialResult.InvalidCredentials ->
                    loginUiState.copy(isLoading = false, errorMessage = "PIN inválido")
                CredentialResult.UserInactive ->
                    loginUiState.copy(isLoading = false, errorMessage = "Usuário inativo")
            }
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            unlockUiState = unlockUiState.copy(isLoading = true, errorMessage = null)
            unlockUiState = when (unlockSessionUseCase(password)) {
                UnlockResult.Success -> UnlockUiState()
                UnlockResult.InvalidPassword ->
                    unlockUiState.copy(isLoading = false, errorMessage = "Senha incorreta")
                UnlockResult.NotLocked -> UnlockUiState()
            }
        }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            unlockUiState = unlockUiState.copy(isLoading = true, errorMessage = null)
            unlockUiState = when (unlockWithPinUseCase(pin)) {
                UnlockResult.Success -> UnlockUiState()
                UnlockResult.InvalidPassword ->
                    unlockUiState.copy(isLoading = false, errorMessage = "PIN incorreto")
                UnlockResult.NotLocked -> UnlockUiState()
            }
        }
    }

    fun confirmNewPin(pin: String) {
        val userId = (sessionState.value as? SessionState.Active)?.user?.id ?: return
        viewModelScope.launch {
            changePinUiState = changePinUiState.copy(isLoading = true, errorMessage = null)
            changePinUiState = when (setPinUseCase(userId, pin)) {
                SetPinResult.Success -> ChangePinUiState()
                SetPinResult.InvalidPin ->
                    changePinUiState.copy(isLoading = false, errorMessage = "PIN inválido: use de 4 a 10 dígitos numéricos")
            }
        }
    }

    fun cycleThemeTone() {
        viewModelScope.launch { updateThemeToneUseCase() }
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }

    fun lock() {
        viewModelScope.launch { lockSessionUseCase() }
    }
}
