package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.LoyaltyConfig
import com.example.pdvmaquineta.domain.model.LoyaltyMode
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

sealed class UpdateLoyaltyConfigResult {
    data class Success(val config: LoyaltyConfig) : UpdateLoyaltyConfigResult()
    data object NotAuthorized : UpdateLoyaltyConfigResult()
    data object InvalidData : UpdateLoyaltyConfigResult()
}

class UpdateLoyaltyConfigUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        mode: LoyaltyMode,
        pointsPerCurrencyUnit: Double?,
        pointValueInCents: Long?,
        visitsRequired: Int?,
        discountPercentOnReward: Int?
    ): UpdateLoyaltyConfigResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return UpdateLoyaltyConfigResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_LOYALTY)) {
            return UpdateLoyaltyConfigResult.NotAuthorized
        }

        val valid = when (mode) {
            LoyaltyMode.POINTS_PER_VALUE -> (pointsPerCurrencyUnit ?: 0.0) > 0 && (pointValueInCents ?: 0) > 0
            LoyaltyMode.VISIT_COUNT_DISCOUNT ->
                (visitsRequired ?: 0) > 0 && (discountPercentOnReward ?: 0) in 1..100
        }
        if (!valid) return UpdateLoyaltyConfigResult.InvalidData

        val previous = loyaltyConfigRepository.getActive()
        val updated = loyaltyConfigRepository.changeConfig(
            mode = mode,
            pointsPerCurrencyUnit = pointsPerCurrencyUnit,
            pointValueInCents = pointValueInCents,
            visitsRequired = visitsRequired,
            discountPercentOnReward = discountPercentOnReward,
            changedByUserId = actor.id
        )

        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.LOYALTY_CONFIG_CHANGED,
                detail = "De ${previous?.mode?.name ?: "nenhuma"} para ${mode.name}",
                success = true
            )
        )
        return UpdateLoyaltyConfigResult.Success(updated)
    }
}
