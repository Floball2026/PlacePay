package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.ProductRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

class SetProductActiveUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(id: Long, active: Boolean): SaveProductResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return SaveProductResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_PRODUCTS)) {
            return SaveProductResult.NotAuthorized
        }

        val product = productRepository.getProduct(id) ?: return SaveProductResult.InvalidData
        productRepository.setActive(id, active)

        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.PRODUCT_STATUS_CHANGED,
                detail = "Produto: ${product.name}; Status: ${if (active) "ativado" else "desativado"}",
                success = true
            )
        )
        return SaveProductResult.Success(product.copy(active = active))
    }
}
