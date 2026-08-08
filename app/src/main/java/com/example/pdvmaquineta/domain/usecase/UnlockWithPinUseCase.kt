package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

// Espelha UnlockSessionUseCase (senha) para usuários que logam por PIN — sem
// isso, um usuário PIN-only travado por inatividade (60s) não teria como se
// desbloquear, já que não existe senha utilizável para ele.
class UnlockWithPinUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(pin: String): UnlockResult {
        val locked = sessionManager.state.value as? SessionState.Locked ?: return UnlockResult.NotLocked

        val result = userRepository.validatePin(locked.user.username, pin)
        return if (result is CredentialResult.Success) {
            sessionManager.unlock()
            auditRepository.log(
                AuditEntry(
                    userId = locked.user.id,
                    username = locked.user.username,
                    action = AuditAction.SESSION_UNLOCKED,
                    success = true
                )
            )
            UnlockResult.Success
        } else {
            auditRepository.log(
                AuditEntry(
                    userId = locked.user.id,
                    username = locked.user.username,
                    action = AuditAction.SESSION_UNLOCKED,
                    detail = "PIN incorreto ao tentar desbloquear",
                    success = false
                )
            )
            UnlockResult.InvalidPassword
        }
    }
}
