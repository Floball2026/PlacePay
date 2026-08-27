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

sealed class CreateUserResult {
    data class Success(val user: User) : CreateUserResult()
    data object NotAuthorized : CreateUserResult()
    data object UsernameTaken : CreateUserResult()
    data object InvalidData : CreateUserResult()
}

class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val setPinUseCase: SetPinUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        displayName: String,
        username: String,
        role: UserRole,
        pin: String
    ): CreateUserResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return CreateUserResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_USERS)) {
            return CreateUserResult.NotAuthorized
        }
        if (displayName.isBlank() || username.isBlank() || pin.length !in 4..10 || !pin.all { it.isDigit() }) {
            return CreateUserResult.InvalidData
        }
        if (userRepository.existsByUsername(username)) {
            return CreateUserResult.UsernameTaken
        }

        val user = userRepository.createUser(username, displayName, role, actor.id)
        setPinUseCase(user.id, pin)

        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.USER_CREATED,
                detail = "Usuário criado: $username; Perfil: $role",
                success = true
            )
        )
        return CreateUserResult.Success(user)
    }
}
