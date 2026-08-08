package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

private const val DISCOUNT_SUPERVISOR_THRESHOLD_PERCENT = 10

sealed class ApplyDiscountResult {
    data object Success : ApplyDiscountResult()
    data object AuthorizationRequired : ApplyDiscountResult()
    data object InvalidPercent : ApplyDiscountResult()
}

class ApplyDiscountUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        sale: Sale,
        percent: Int,
        supervisorAuthorization: SupervisorAuthorization?
    ): ApplyDiscountResult {
        if (percent !in 0..100) return ApplyDiscountResult.InvalidPercent
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return ApplyDiscountResult.InvalidPercent

        if (percent > DISCOUNT_SUPERVISOR_THRESHOLD_PERCENT) {
            val authorized = AuthorizationPolicy.hasPermission(actor.role, Permission.AUTHORIZE_DISCOUNT) ||
                (supervisorAuthorization != null && supervisorAuthorization.permission == Permission.AUTHORIZE_DISCOUNT)
            if (!authorized) return ApplyDiscountResult.AuthorizationRequired
        }

        saleRepository.setDiscount(sale.id, percent)

        val detail = buildString {
            append("Desconto de $percent%")
            if (supervisorAuthorization != null) append("; Autorizado por: ${supervisorAuthorization.authorizedByUsername}")
        }
        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.DISCOUNT_AUTHORIZED,
                detail = detail,
                success = true
            )
        )

        return ApplyDiscountResult.Success
    }
}
