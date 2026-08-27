package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class SetPinResult {
    data object Success : SetPinResult()
    data object InvalidPin : SetPinResult()
}

// Único ponto que grava um PIN novo — usado tanto na criação de usuário
// (CreateUserUseCase) quanto no fluxo pós-login de troca forçada. Sempre zera
// mustChangePin (é um no-op se já estava false). Se quem está trocando é o
// próprio usuário logado (troca forçada), atualiza também o snapshot da
// sessão ativa, senão a tela de troca reapareceria em loop.
class SetPinUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: Long, pin: String): SetPinResult {
        // PIN de 4 a 10 digitos numericos (alinhado a PinPolicy da UI: MIN=4, MAX=10).
        if (pin.length !in 4..10 || !pin.all { it.isDigit() }) return SetPinResult.InvalidPin

        userRepository.setPin(userId, pin)
        val updatedUser = userRepository.findById(userId)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            val isSelfService = actor.id == userId
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.USER_PIN_CHANGED,
                    detail = if (isSelfService) {
                        "PIN alterado pelo próprio usuário"
                    } else {
                        "PIN definido pelo administrador para ${updatedUser?.username}"
                    },
                    success = true
                )
            )
            if (isSelfService && updatedUser != null) {
                sessionManager.refreshActiveUser(updatedUser)
            }
        }

        return SetPinResult.Success
    }
}
