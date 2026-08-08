package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(username: String, password: String): CredentialResult {
        val result = userRepository.validateCredentials(username, password)

        when (result) {
            is CredentialResult.Success -> {
                sessionManager.start(result.user)
                auditRepository.log(
                    AuditEntry(
                        userId = result.user.id,
                        username = result.user.username,
                        action = AuditAction.LOGIN_SUCCESS,
                        success = true
                    )
                )
            }
            CredentialResult.InvalidCredentials -> auditRepository.log(
                AuditEntry(
                    userId = null,
                    username = username,
                    action = AuditAction.LOGIN_FAILURE,
                    detail = "Credenciais inválidas",
                    success = false
                )
            )
            CredentialResult.UserInactive -> auditRepository.log(
                AuditEntry(
                    userId = null,
                    username = username,
                    action = AuditAction.LOGIN_FAILURE,
                    detail = "Usuário inativo",
                    success = false
                )
            )
        }

        return result
    }
}
