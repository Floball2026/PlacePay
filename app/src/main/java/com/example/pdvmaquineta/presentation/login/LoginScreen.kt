package com.example.pdvmaquineta.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import com.example.pdvmaquineta.presentation.session.LoginUiState
import com.example.pdvmaquineta.presentation.session.PinInputField
import com.example.pdvmaquineta.presentation.session.PinPolicy
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    loginMethod: LoginMethod,
    onUsernameChange: (String) -> Unit,
    onLoginWithPassword: (String, String) -> Unit,
    onLoginWithPin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val canSubmit = username.isNotBlank() &&
        if (loginMethod == LoginMethod.PIN) PinPolicy.isValidLength(pin) else password.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PDV Maquineta",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                onUsernameChange(it)
            },
            label = { Text("Usuário") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        if (loginMethod == LoginMethod.PIN) {
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
                if (loginMethod == LoginMethod.PIN) {
                    onLoginWithPin(username, pin)
                } else {
                    onLoginWithPassword(username, password)
                }
            },
            enabled = !uiState.isLoading && canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Entrando..." else "Entrar")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvOutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Entrar com biometria (em breve)")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PDVMaquinetaTheme {
        LoginScreen(
            uiState = LoginUiState(),
            loginMethod = LoginMethod.PASSWORD,
            onUsernameChange = {},
            onLoginWithPassword = { _, _ -> },
            onLoginWithPin = { _, _ -> }
        )
    }
}
