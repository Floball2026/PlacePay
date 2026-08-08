package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class UnlockResult {
    data object Success : UnlockResult()
    data object InvalidPassword : UnlockResult()
    data object NotLocked : UnlockResult()
}

class UnlockSessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(password: String): UnlockResult {
        val locked = sessionManager.state.value as? SessionState.Locked ?: return UnlockResult.NotLocked

        val result = userRepository.validateCredentials(locked.user.username, password)
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
                    detail = "Senha incorreta ao tentar desbloquear",
                    success = false
                )
            )
            UnlockResult.InvalidPassword
        }
    }
}
