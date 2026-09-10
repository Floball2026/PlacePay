package com.example.pdvmaquineta.data.payment

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Canal simples pras mensagens do fluxo de pagamento do SDK (ex.: "Aproxime,
// insira ou passe o cartao", "Processando", "Digite a senha"). O gateway
// publica; a tela de pagamento observa e exibe. A doc da PayTime exige que
// essas mensagens do onMessage sejam mostradas ao operador/cliente.
@Singleton
class PaymentStatusBus @Inject constructor() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun update(text: String?) {
        // A doc da PayTime pede exibir a mensagem inteira, tratando quebras de
        // linha e espacos. Colapsa \n e espacos repetidos num unico espaco.
        _message.value = text
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun clear() {
        _message.value = null
    }
}
