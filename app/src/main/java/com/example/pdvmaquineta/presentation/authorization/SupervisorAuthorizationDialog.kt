package com.example.pdvmaquineta.presentation.authorization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

// Reusável: qualquer ação que exija autorização de um perfil superior
// (sangria/suprimento agora, desconto/cancelamento na Fase 3) usa este mesmo
// diálogo — só muda a permissão verificada por quem chama.
@Composable
fun SupervisorAuthorizationDialog(
    errorMessage: String?,
    onAuthorize: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Autorização necessária") },
        text = {
            Column {
                Text("Esta ação precisa da senha de um supervisor ou administrador.")
                Spacer(Modifier.height(PdvDimens.SpacingMedium))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário do supervisor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { error ->
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            PdvTextButton(
                onClick = { onAuthorize(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Autorizar")
            }
        },
        dismissButton = {
            PdvTextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
