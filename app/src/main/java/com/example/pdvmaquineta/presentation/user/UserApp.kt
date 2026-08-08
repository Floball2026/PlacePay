package com.example.pdvmaquineta.presentation.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun UserApp(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()

    when (val route = viewModel.route) {
        UserRoute.List -> UserListScreen(
            users = users,
            pendingDialog = viewModel.pendingDialog,
            dialogErrorMessage = viewModel.dialogErrorMessage,
            onAddUser = viewModel::openNewUserForm,
            onEditUser = viewModel::openEditUserForm,
            onRequestToggleActive = viewModel::requestToggleActive,
            onRequestForcePinReset = viewModel::requestForcePinReset,
            onConfirmToggleActive = viewModel::confirmToggleActive,
            onConfirmForcePinReset = viewModel::confirmForcePinReset,
            onDismissDialog = viewModel::dismissDialog,
            onBack = onBack,
            modifier = modifier
        )

        is UserRoute.Form -> UserFormScreen(
            existingUser = route.user,
            uiState = viewModel.formUiState,
            onSave = { displayName, username, role, pin ->
                viewModel.saveUser(route.user?.id, displayName, username, role, pin)
            },
            onCancel = viewModel::backToList,
            modifier = modifier
        )
    }
}
