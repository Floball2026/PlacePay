package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.repository.UserRepository
import javax.inject.Inject

data class SupervisorAuthorization(
    val permission: Permission,
    val authorizedByUsername: String
)

sealed class SupervisorAuthorizationResult {
    data class Authorized(val authorization: SupervisorAuthorization) : SupervisorAuthorizationResult()
    data object InvalidCredentials : SupervisorAuthorizationResult()
    data object InsufficientPermission : SupervisorAuthorizationResult()
}

// Genérico por design: não é específico de caixa. Sangria/suprimento usam
// agora; desconto/cancelamento (Fase 3) reaproveitam o mesmo mecanismo.
class AuthorizeWithSupervisorUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        permission: Permission,
        username: String,
        password: String
    ): SupervisorAuthorizationResult {
        val result = userRepository.validateCredentials(username, password)
        if (result !is CredentialResult.Success) {
            return SupervisorAuthorizationResult.InvalidCredentials
        }

        return if (AuthorizationPolicy.hasPermission(result.user.role, permission)) {
            SupervisorAuthorizationResult.Authorized(
                SupervisorAuthorization(permission, result.user.username)
            )
        } else {
            SupervisorAuthorizationResult.InsufficientPermission
        }
    }
}
