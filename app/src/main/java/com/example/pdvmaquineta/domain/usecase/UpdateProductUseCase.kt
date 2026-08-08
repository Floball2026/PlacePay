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

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        id: Long,
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
                if (it.id != id) return SaveProductResult.DuplicateBarcode(it.name)
            }
        }

        val before = productRepository.getProduct(id) ?: return SaveProductResult.InvalidData
        val updated = productRepository.updateProduct(
            id, name, priceCents, category, stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath
        )

        if (before.priceCents != updated.priceCents) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.PRODUCT_PRICE_CHANGED,
                    detail = "Produto: ${updated.name}; De ${before.priceCents} para " +
                        "${updated.priceCents} centavos",
                    success = true
                )
            )
        }
        // Ajuste manual de estoque pela tela de edição — distinto da baixa
        // automática por venda, que não gera esta auditoria (já coberta pela
        // auditoria da venda/pagamento).
        if (before.stockQuantity != updated.stockQuantity) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.STOCK_ADJUSTED,
                    detail = "Produto: ${updated.name}; De ${before.stockQuantity} para " +
                        "${updated.stockQuantity} unidades",
                    success = true
                )
            )
        }
        return SaveProductResult.Success(updated)
    }
}
