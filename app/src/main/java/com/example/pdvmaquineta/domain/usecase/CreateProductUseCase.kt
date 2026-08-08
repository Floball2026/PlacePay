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

class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ): SaveProductResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return SaveProductResult.NotAuthorized
        if (!AuthorizationPolicy.hasPermission(actor.role, Permission.MANAGE_PRODUCTS)) {
            return SaveProductResult.NotAuthorized
        }
        if (name.isBlank() || priceCents < 0 || stockQuantity < 0 || (minStockAlert != null && minStockAlert < 0)) {
            return SaveProductResult.InvalidData
        }
        if (barcode != null) {
            productRepository.findByBarcode(barcode)?.let {
                return SaveProductResult.DuplicateBarcode(it.name)
            }
        }

        val product = productRepository.createProduct(
            name, priceCents, category, stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath
        )
        auditRepository.log(
            AuditEntry(
                userId = actor.id,
                username = actor.username,
                action = AuditAction.PRODUCT_CREATED,
                detail = "Produto: ${product.name}; Preço: ${product.priceCents} centavos; " +
                    "Estoque inicial: ${product.stockQuantity}",
                success = true
            )
        )
        return SaveProductResult.Success(product)
    }
}
