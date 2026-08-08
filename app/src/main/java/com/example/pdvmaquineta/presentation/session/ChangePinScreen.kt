package com.example.pdvmaquineta.presentation.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

// Intercepta o app logo após um login por PIN quando mustChangePin = true —
// o usuário não consegue prosseguir sem definir um PIN novo (só "Sair" como
// válvula de escape, pra outro usuário poder logar no dispositivo).
@Composable
fun ChangePinScreen(
    uiState: ChangePinUiState,
    onConfirm: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val pinsMismatch = confirmPin.isNotEmpty() && newPin != confirmPin
    val canConfirm = PinPolicy.isValidLength(newPin) && newPin == confirmPin

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Defina seu novo PIN",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Por segurança, defina um novo PIN de 4 a 10 dígitos antes de continuar.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PdvDimens.SpacingSmall)
        )

        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        PinInputField(
            value = newPin,
            onValueChange = { newPin = it },
            label = "Novo PIN",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PinInputField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = "Confirme o novo PIN",
            modifier = Modifier.fillMaxWidth()
        )

        if (pinsMismatch) {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = "Os PINs não coincidem", color = MaterialTheme.colorScheme.error)
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = { onConfirm(newPin) },
            enabled = !uiState.isLoading && canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Salvando..." else "Confirmar")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        PdvTextButton(onClick = onLogout) {
            Text("Sair (encerrar sessão)")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePinScreenPreview() {
    PDVMaquinetaTheme {
        ChangePinScreen(
            uiState = ChangePinUiState(),
            onConfirm = {},
            onLogout = {}
        )
    }
}
