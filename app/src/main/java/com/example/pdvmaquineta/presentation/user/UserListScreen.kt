package com.example.pdvmaquineta.presentation.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import com.example.pdvmaquineta.presentation.theme.PdvTextWarning

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.OPERATOR -> "Operador de Caixa"
    UserRole.SUPERVISOR -> "Supervisor"
    UserRole.ADMIN -> "Administrador"
}

@Composable
fun UserListScreen(
    users: List<User>,
    pendingDialog: UserListDialog?,
    dialogErrorMessage: String?,
    onAddUser: () -> Unit,
    onEditUser: (User) -> Unit,
    onRequestToggleActive: (User) -> Unit,
    onRequestForcePinReset: (User) -> Unit,
    onConfirmToggleActive: () -> Unit,
    onConfirmForcePinReset: () -> Unit,
    onDismissDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) {
            users
        } else {
            users.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text(
            "Usuários",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = onAddUser,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Novo usuário")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por nome ou usuário") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (filteredUsers.isEmpty()) {
            Text(
                "Nenhum usuário encontrado.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filteredUsers.forEach { user ->
                UserManagementRow(
                    user = user,
                    onEdit = { onEditUser(user) },
                    onToggleActive = { onRequestToggleActive(user) },
                    onForcePinReset = { onRequestForcePinReset(user) }
                )
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar ao caixa")
        }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }

    when (pendingDialog) {
        is UserListDialog.ConfirmToggleActive -> ConfirmActionDialog(
            title = if (pendingDialog.user.active) "Desativar usuário" else "Ativar usuário",
            message = if (pendingDialog.user.active) {
                "Desativar ${pendingDialog.user.displayName}? Ele não vai conseguir mais fazer login."
            } else {
                "Ativar ${pendingDialog.user.displayName}?"
            },
            errorMessage = dialogErrorMessage,
            onConfirm = onConfirmToggleActive,
            onDismiss = onDismissDialog
        )

        is UserListDialog.ConfirmForcePinReset -> ConfirmActionDialog(
            title = "Forçar troca de PIN",
            message = "${pendingDialog.user.displayName} vai precisar definir um novo PIN no próximo login.",
            errorMessage = dialogErrorMessage,
            onConfirm = onConfirmForcePinReset,
            onDismiss = onDismissDialog
        )

        null -> Unit
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                errorMessage?.let { error ->
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            PdvTextButton(onClick = onConfirm) { Text("Confirmar") }
        },
        dismissButton = {
            PdvTextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun UserManagementRow(
    user: User,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onForcePinReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                    if (!user.active) {
                        Spacer(Modifier.width(PdvDimens.SpacingSmall))
                        Text("Inativo", style = MaterialTheme.typography.bodyLarge, color = PdvTextWarning)
                    }
                }
                Text(
                    "${user.username} • ${roleLabel(user.role)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            PdvTextButton(onClick = onEdit) { Text("Editar") }
            PdvTextButton(onClick = onToggleActive) {
                Text(if (user.active) "Desativar" else "Ativar")
            }
            PdvTextButton(onClick = onForcePinReset) { Text("Forçar troca de PIN") }
        }
    }
}
