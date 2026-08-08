package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class ForceChangePinResult {
    data object Success : ForceChangePinResult()
    data object NotAuthorized : ForceChangePinResult()
}

class ForceChangePinUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: Long): ForceChangePinResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return ForceChangePinResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_USERS)) {
            return ForceChangePinResult.NotAuthorized
        }

        val user = userRepository.findById(userId) ?: return ForceChangePinResult.NotAuthorized
        userRepository.setMustChangePin(userId, true)

        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.USER_PIN_RESET_FORCED,
                detail = "Usuário: ${user.username}",
                success = true
            )
        )
        return ForceChangePinResult.Success
    }
}
