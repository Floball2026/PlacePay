package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class LockSessionUseCase @Inject constructor(
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        val active = sessionManager.state.value as? SessionState.Active ?: return

        sessionManager.lock()
        auditRepository.log(
            AuditEntry(
                userId = active.user.id,
                username = active.user.username,
                action = AuditAction.SESSION_LOCKED,
                success = true
            )
        )
    }
}
