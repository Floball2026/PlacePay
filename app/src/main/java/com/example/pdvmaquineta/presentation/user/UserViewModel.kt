package com.example.pdvmaquineta.presentation.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import com.example.pdvmaquineta.domain.usecase.CreateUserResult
import com.example.pdvmaquineta.domain.usecase.CreateUserUseCase
import com.example.pdvmaquineta.domain.usecase.ForceChangePinResult
import com.example.pdvmaquineta.domain.usecase.ForceChangePinUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveUsersUseCase
import com.example.pdvmaquineta.domain.usecase.SetUserActiveResult
import com.example.pdvmaquineta.domain.usecase.SetUserActiveUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateUserResult
import com.example.pdvmaquineta.domain.usecase.UpdateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UserRoute {
    data object List : UserRoute()
    data class Form(val user: User?) : UserRoute()
}

sealed class UserListDialog {
    data class ConfirmToggleActive(val user: User) : UserListDialog()
    data class ConfirmForcePinReset(val user: User) : UserListDialog()
}

data class UserFormUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    observeUsersUseCase: ObserveUsersUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val setUserActiveUseCase: SetUserActiveUseCase,
    private val forceChangePinUseCase: ForceChangePinUseCase
) : ViewModel() {

    val users: StateFlow<List<User>> = observeUsersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var route by mutableStateOf<UserRoute>(UserRoute.List)
        private set

    var formUiState by mutableStateOf(UserFormUiState())
        private set

    var pendingDialog by mutableStateOf<UserListDialog?>(null)
        private set

    var dialogErrorMessage by mutableStateOf<String?>(null)
        private set

    fun openNewUserForm() {
        formUiState = UserFormUiState()
        route = UserRoute.Form(null)
    }

    fun openEditUserForm(user: User) {
        formUiState = UserFormUiState()
        route = UserRoute.Form(user)
    }

    fun backToList() {
        route = UserRoute.List
    }

    fun saveUser(id: Long?, displayName: String, username: String, role: UserRole, pin: String) {
        viewModelScope.launch {
            formUiState = formUiState.copy(isLoading = true, errorMessage = null)
            if (id == null) {
                when (createUserUseCase(displayName, username, role, pin)) {
                    is CreateUserResult.Success -> {
                        formUiState = UserFormUiState()
                        route = UserRoute.List
                    }
                    CreateUserResult.NotAuthorized ->
                        formUiState = formUiState.copy(isLoading = false, errorMessage = "Sem permissão para essa ação")
                    CreateUserResult.UsernameTaken ->
                        formUiState = formUiState.copy(isLoading = false, errorMessage = "Já existe um usuário com esse nome de usuário")
                    CreateUserResult.InvalidData ->
                        formUiState = formUiState.copy(
                            isLoading = false,
                            errorMessage = "Dados inválidos: confira nome, usuário e PIN (4 a 10 dígitos numéricos)"
                        )
                }
            } else {
                when (updateUserUseCase(id, displayName, role)) {
                    is UpdateUserResult.Success -> {
                        formUiState = UserFormUiState()
                        route = UserRoute.List
                    }
                    UpdateUserResult.NotAuthorized ->
                        formUiState = formUiState.copy(isLoading = false, errorMessage = "Sem permissão para essa ação")
                    UpdateUserResult.InvalidData ->
                        formUiState = formUiState.copy(isLoading = false, errorMessage = "Dados inválidos")
                    UpdateUserResult.WouldRemoveLastAdmin ->
                        formUiState = formUiState.copy(
                            isLoading = false,
                            errorMessage = "Essa mudança deixaria o sistema sem nenhum administrador ativo"
                        )
                }
            }
        }
    }

    fun requestToggleActive(user: User) {
        pendingDialog = UserListDialog.ConfirmToggleActive(user)
        dialogErrorMessage = null
    }

    fun requestForcePinReset(user: User) {
        pendingDialog = UserListDialog.ConfirmForcePinReset(user)
        dialogErrorMessage = null
    }

    fun dismissDialog() {
        pendingDialog = null
        dialogErrorMessage = null
    }

    fun confirmToggleActive() {
        val dialog = pendingDialog as? UserListDialog.ConfirmToggleActive ?: return
        viewModelScope.launch {
            when (setUserActiveUseCase(dialog.user.id, !dialog.user.active)) {
                is SetUserActiveResult.Success -> {
                    pendingDialog = null
                    dialogErrorMessage = null
                }
                SetUserActiveResult.NotAuthorized ->
                    dialogErrorMessage = "Sem permissão para essa ação"
                SetUserActiveResult.WouldRemoveLastAdmin ->
                    dialogErrorMessage = "Não é possível desativar: é o último administrador ativo"
            }
        }
    }

    fun confirmForcePinReset() {
        val dialog = pendingDialog as? UserListDialog.ConfirmForcePinReset ?: return
        viewModelScope.launch {
            when (forceChangePinUseCase(dialog.user.id)) {
                ForceChangePinResult.Success -> {
                    pendingDialog = null
                    dialogErrorMessage = null
                }
                ForceChangePinResult.NotAuthorized ->
                    dialogErrorMessage = "Sem permissão para essa ação"
            }
        }
    }
}
