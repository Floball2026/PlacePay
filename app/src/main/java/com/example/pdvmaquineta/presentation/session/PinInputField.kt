package com.example.pdvmaquineta.presentation.session

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

// Campo de PIN (4 a 10 dígitos) reutilizado no login, no desbloqueio e na troca
// de PIN — teclado numérico, mascarado, sem deixar digitar mais que 6.
@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= PinPolicy.MAX && new.all { it.isDigit() }) onValueChange(new)
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier
    )
}
