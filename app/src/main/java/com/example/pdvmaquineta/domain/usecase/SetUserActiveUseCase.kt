package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class SetUserActiveResult {
    data class Success(val user: User) : SetUserActiveResult()
    data object NotAuthorized : SetUserActiveResult()
    data object WouldRemoveLastAdmin : SetUserActiveResult()
}

class SetUserActiveUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: Long, active: Boolean): SetUserActiveResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return SetUserActiveResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_USERS)) {
            return SetUserActiveResult.NotAuthorized
        }

        val user = userRepository.findById(userId) ?: return SetUserActiveResult.NotAuthorized

        if (!active && user.role == UserRole.ADMIN && userRepository.countActiveByRole(UserRole.ADMIN) <= 1) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.USER_DEACTIVATION_BLOCKED,
                    detail = "Usuário: ${user.username}; Motivo: último admin ativo",
                    success = false
                )
            )
            return SetUserActiveResult.WouldRemoveLastAdmin
        }

        userRepository.setActive(userId, active)
        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = if (active) AuditAction.USER_ACTIVATED else AuditAction.USER_DEACTIVATED,
                detail = "Usuário: ${user.username}",
                success = true
            )
        )
        return SetUserActiveResult.Success(user.copy(active = active))
    }
}
