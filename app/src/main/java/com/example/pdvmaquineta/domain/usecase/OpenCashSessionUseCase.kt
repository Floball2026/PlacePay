package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class OpenCashSessionUseCase @Inject constructor(
    private val cashSessionRepository: CashSessionRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(openingBalanceCents: Long): CashSession? {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return null

        val session = cashSessionRepository.openSession(user, openingBalanceCents)
        auditRepository.log(
            AuditEntry(
                userId = user.id,
                username = user.username,
                action = AuditAction.CASH_OPENED,
                detail = "Valor inicial: $openingBalanceCents centavos",
                success = true
            )
        )
        return session
    }
}
