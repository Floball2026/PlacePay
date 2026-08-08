package com.example.pdvmaquineta.presentation.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdvmaquineta.presentation.session.LoginMethod
import com.example.pdvmaquineta.presentation.session.PinInputField
import com.example.pdvmaquineta.presentation.session.PinPolicy
import com.example.pdvmaquineta.presentation.session.UnlockUiState
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

@Composable
fun LockScreen(
    userDisplayName: String,
    unlockMethod: LoginMethod,
    uiState: UnlockUiState,
    onUnlockWithPassword: (String) -> Unit,
    onUnlockWithPin: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sessão bloqueada",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = userDisplayName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PdvDimens.SpacingSmall)
        )

        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        if (unlockMethod == LoginMethod.PIN) {
            PinInputField(
                value = pin,
                onValueChange = { pin = it },
                label = "PIN",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = {
                if (unlockMethod == LoginMethod.PIN) onUnlockWithPin(pin) else onUnlockWithPassword(password)
            },
            enabled = !uiState.isLoading &&
                if (unlockMethod == LoginMethod.PIN) PinPolicy.isValidLength(pin) else password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Verificando..." else "Desbloquear")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvTextButton(onClick = onLogout) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Sair (encerrar sessão)")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    PDVMaquinetaTheme {
        LockScreen(
            userDisplayName = "Operador de Teste",
            unlockMethod = LoginMethod.PASSWORD,
            uiState = UnlockUiState(),
            onUnlockWithPassword = {},
            onUnlockWithPin = {},
            onLogout = {}
        )
    }
}
