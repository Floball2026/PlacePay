package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        val user = when (val current = sessionManager.state.value) {
            is SessionState.Active -> current.user
            is SessionState.Locked -> current.user
            SessionState.LoggedOut -> null
        }

        sessionManager.clear()

        if (user != null) {
            auditRepository.log(
                AuditEntry(
                    userId = user.id,
                    username = user.username,
                    action = AuditAction.LOGOUT,
                    success = true
                )
            )
        }
    }
}
