package com.example.pdvmaquineta.presentation.user

import com.example.pdvmaquineta.presentation.session.PinPolicy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.pdvmaquineta.presentation.session.PinInputField
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.OPERATOR -> "Operador de Caixa"
    UserRole.SUPERVISOR -> "Supervisor"
    UserRole.ADMIN -> "Administrador"
}

@Composable
fun UserFormScreen(
    existingUser: User?,
    uiState: UserFormUiState,
    onSave: (displayName: String, username: String, role: UserRole, pin: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCreating = existingUser == null
    var displayName by remember { mutableStateOf(existingUser?.displayName.orEmpty()) }
    var username by remember { mutableStateOf(existingUser?.username.orEmpty()) }
    var role by remember { mutableStateOf(existingUser?.role ?: UserRole.OPERATOR) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val pinsMismatch = isCreating && confirmPin.isNotEmpty() && pin != confirmPin
    val canSave = displayName.isNotBlank() && username.isNotBlank() &&
        (!isCreating || (PinPolicy.isValidLength(pin) && pin == confirmPin))

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
            text = if (isCreating) "Novo usuário" else "Editar usuário",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuário") },
            singleLine = true,
            enabled = isCreating,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        RoleSelector(selected = role, onSelect = { role = it })

        if (isCreating) {
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            PinInputField(
                value = pin,
                onValueChange = { pin = it },
                label = "PIN (4 a 10 dígitos)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            PinInputField(
                value = confirmPin,
                onValueChange = { confirmPin = it },
                label = "Confirme o PIN",
                modifier = Modifier.fillMaxWidth()
            )
            if (pinsMismatch) {
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text("Os PINs não coincidem", color = MaterialTheme.colorScheme.error)
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { onSave(displayName, username, role, pin) },
            enabled = !uiState.isLoading && canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Salvando..." else "Salvar")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onCancel) { Text("Cancelar") }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun RoleSelector(selected: UserRole, onSelect: (UserRole) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Perfil", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        UserRole.entries.forEach { role ->
            val modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
            if (role == selected) {
                PdvButton(onClick = { onSelect(role) }, modifier = modifier) {
                    Text(roleLabel(role))
                }
            } else {
                PdvOutlinedButton(onClick = { onSelect(role) }, modifier = modifier) {
                    Text(roleLabel(role))
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }
    }
}
