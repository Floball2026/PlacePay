package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class CloseCashSessionUseCase @Inject constructor(
    private val cashSessionRepository: CashSessionRepository,
    private val getCashOverviewUseCase: GetCashOverviewUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(session: CashSession, informedCashCents: Long): CashSession {
        val overview = getCashOverviewUseCase(session)
        val divergenceCents = informedCashCents - overview.expectedCashCents

        val closed = cashSessionRepository.closeSession(
            sessionId = session.id,
            expectedCashCents = overview.expectedCashCents,
            informedCashCents = informedCashCents,
            divergenceCents = divergenceCents
        )

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.CASH_CLOSED,
                    detail = "Esperado: ${overview.expectedCashCents}; Informado: $informedCashCents; " +
                        "Divergência: $divergenceCents (centavos)",
                    success = true
                )
            )
        }

        return closed
    }
}
