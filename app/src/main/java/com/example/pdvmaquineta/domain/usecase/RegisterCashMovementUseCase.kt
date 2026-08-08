package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CashMovement
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class RegisterCashMovementResult {
    data class Success(val movement: CashMovement) : RegisterCashMovementResult()
    data object AuthorizationRequired : RegisterCashMovementResult()
    data object NotLoggedIn : RegisterCashMovementResult()
}

class RegisterCashMovementUseCase @Inject constructor(
    private val cashSessionRepository: CashSessionRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        session: CashSession,
        type: CashMovementType,
        amountCents: Long,
        reason: String,
        supervisorAuthorization: SupervisorAuthorization?
    ): RegisterCashMovementResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return RegisterCashMovementResult.NotLoggedIn

        val requiredPermission = when (type) {
            CashMovementType.WITHDRAWAL -> Permission.AUTHORIZE_CASH_WITHDRAWAL
            CashMovementType.SUPPLY -> Permission.AUTHORIZE_CASH_SUPPLY
        }

        val authorizedByUsername: String? = when {
            AuthorizationPolicy.hasPermission(actor.role, requiredPermission) -> null
            supervisorAuthorization != null && supervisorAuthorization.permission == requiredPermission ->
                supervisorAuthorization.authorizedByUsername
            else -> return RegisterCashMovementResult.AuthorizationRequired
        }

        val movement = cashSessionRepository.addMovement(
            sessionId = session.id,
            type = type,
            amountCents = amountCents,
            reason = reason,
            operator = actor,
            authorizedByUsername = authorizedByUsername
        )

        val action = if (type == CashMovementType.WITHDRAWAL) {
            AuditAction.CASH_WITHDRAWAL
        } else {
            AuditAction.CASH_SUPPLY
        }
        val detail = buildString {
            append("Motivo: $reason")
            if (authorizedByUsername != null) append("; Autorizado por: $authorizedByUsername")
        }
        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = action,
                detail = detail,
                success = true
            )
        )

        return RegisterCashMovementResult.Success(movement)
    }
}
