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

sealed class UpdateUserResult {
    data class Success(val user: User) : UpdateUserResult()
    data object NotAuthorized : UpdateUserResult()
    data object InvalidData : UpdateUserResult()
    data object WouldRemoveLastAdmin : UpdateUserResult()
}

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: Long, displayName: String, role: UserRole): UpdateUserResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return UpdateUserResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_USERS)) {
            return UpdateUserResult.NotAuthorized
        }
        if (displayName.isBlank()) return UpdateUserResult.InvalidData

        val before = userRepository.findById(userId) ?: return UpdateUserResult.InvalidData

        // Tirar o papel de ADMIN de alguém não pode zerar os admins ativos.
        if (before.role == UserRole.ADMIN && role != UserRole.ADMIN && before.active) {
            if (userRepository.countActiveByRole(UserRole.ADMIN) <= 1) {
                return UpdateUserResult.WouldRemoveLastAdmin
            }
        }

        val updated = userRepository.updateUser(userId, displayName, role)

        if (before.role != updated.role) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.USER_ROLE_CHANGED,
                    detail = "Usuário: ${updated.username}; De ${before.role} para ${updated.role}",
                    success = true
                )
            )
        }
        return UpdateUserResult.Success(updated)
    }
}
