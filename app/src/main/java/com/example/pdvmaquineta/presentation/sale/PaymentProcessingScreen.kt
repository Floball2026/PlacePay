package com.example.pdvmaquineta.presentation.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton

@Composable
fun PaymentProcessingScreen(
    message: String? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        // Mostra a instrucao do terminal (ex.: "Aproxime, insira ou passe o
        // cartao", "Digite a senha") quando o SDK enviar; senao, texto padrao.
        Text(
            text = message ?: "Processando pagamento...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        onCancel?.let { cancel ->
            Spacer(Modifier.height(PdvDimens.SpacingLarge))
            PdvOutlinedButton(
                onClick = cancel,
                modifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentProcessingScreenPreview() {
    PDVMaquinetaTheme {
        PaymentProcessingScreen()
    }
}
