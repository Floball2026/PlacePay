package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.PrinterType
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.model.TerminalEnvironment
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.TerminalConfigRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class UpdateTerminalConfigResult {
    data class Success(val config: TerminalConfig) : UpdateTerminalConfigResult()
    data object NotAuthorized : UpdateTerminalConfigResult()
    data object InvalidData : UpdateTerminalConfigResult()
}

class UpdateTerminalConfigUseCase @Inject constructor(
    private val terminalConfigRepository: TerminalConfigRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        terminalName: String,
        storeName: String,
        environment: TerminalEnvironment,
        printerType: PrinterType
    ): UpdateTerminalConfigResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return UpdateTerminalConfigResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_TERMINALS)) {
            return UpdateTerminalConfigResult.NotAuthorized
        }
        if (terminalName.isBlank() || storeName.isBlank()) {
            return UpdateTerminalConfigResult.InvalidData
        }

        val updated = terminalConfigRepository.updateConfig(
            terminalName = terminalName,
            storeName = storeName,
            environment = environment,
            printerType = printerType,
            updatedByUserId = actor.id
        )

        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.TERMINAL_CONFIG_UPDATED,
                detail = "Terminal: $terminalName; Loja: $storeName; Ambiente: ${environment.name}; " +
                    "Impressora: ${printerType.name}",
                success = true
            )
        )
        return UpdateTerminalConfigResult.Success(updated)
    }
}
