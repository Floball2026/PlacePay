package com.example.pdvmaquineta.presentation.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

// RF-003: qualquer toque na tela reinicia o timer; sem toque nenhum dentro do
// prazo, dispara onInactive() (que só bloqueia sessão se houver uma sessão
// ativa — ver LockSessionUseCase). O prazo é fixo por enquanto; virar
// configurável pelo admin é assunto de fase futura.
private const val INACTIVITY_TIMEOUT_MILLIS = 60_000L

@Composable
fun InactivityAwareContent(
    onInactive: () -> Unit,
    content: @Composable () -> Unit
) {
    var lastInteractionAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionAt) {
        delay(INACTIVITY_TIMEOUT_MILLIS)
        onInactive()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionAt = System.currentTimeMillis()
                    }
                }
            }
    ) {
        content()
    }
}
