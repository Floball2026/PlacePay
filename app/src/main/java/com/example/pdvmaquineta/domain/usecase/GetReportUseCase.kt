package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import com.example.pdvmaquineta.domain.repository.ProductRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

private const val OPERATIONAL_REPORT_MAX_RANGE_DAYS = 30
private const val TOP_PRODUCTS_LIMIT = 5
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

data class SalesReportSection(
    val totalSoldCents: Long,
    val completedSalesCount: Int,
    val totalsByMethod: Map<PaymentMethod, Long>,
    val topProducts: List<TopProduct>
)

data class CashReportSection(
    val closedSessionsCount: Int,
    val totalOpeningCents: Long,
    val totalInformedCents: Long,
    val totalWithdrawalCents: Long,
    val totalSupplyCents: Long,
    // Só sessões cujo divergenceCents != 0.
    val divergentSessions: List<CashSession>
)

data class ProductsReportSection(
    val lowStockProducts: List<Product>,
    val topProducts: List<TopProduct>
)

data class ReportData(
    // Intervalo efetivamente aplicado (já com o limite de 30 dias imposto
    // pra Supervisor, se for o caso) — sempre reflete o que foi consultado de
    // fato, não necessariamente o que foi pedido.
    val fromMillis: Long?,
    val toMillis: Long?,
    val maxRangeDays: Int?,
    val sales: SalesReportSection,
    val cash: CashReportSection,
    val products: ProductsReportSection
)

sealed class GetReportResult {
    data class Success(val data: ReportData) : GetReportResult()
    data object NotAuthorized : GetReportResult()
}

// Painel único combinando Vendas/Caixa/Produtos (Fase 7b). VIEW_FULL_REPORTS
// (Admin) consulta sem limite de período; VIEW_OPERATIONAL_REPORTS
// (Supervisor) fica limitado aos últimos 30 dias — imposto aqui (não só na
// UI), mesmo princípio de autorização centralizada (RF-002) já usado em
// outras ações sensíveis.
class GetReportUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val cashSessionRepository: CashSessionRepository,
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(fromMillis: Long?, toMillis: Long?): GetReportResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
            ?: return GetReportResult.NotAuthorized
        val canViewFull = AuthorizationPolicy.hasPermission(actor.role, Permission.VIEW_FULL_REPORTS)
        val canViewOperational = AuthorizationPolicy.hasPermission(actor.role, Permission.VIEW_OPERATIONAL_REPORTS)
        if (!canViewFull && !canViewOperational) return GetReportResult.NotAuthorized

        val maxRangeDays = if (canViewFull) null else OPERATIONAL_REPORT_MAX_RANGE_DAYS
        val effectiveFrom = if (maxRangeDays != null) {
            val oldestAllowed = System.currentTimeMillis() - maxRangeDays * DAY_MILLIS
            if (fromMillis == null || fromMillis < oldestAllowed) oldestAllowed else fromMillis
        } else {
            fromMillis
        }

        val totalsByMethod = paymentRepository.getApprovedTotalsByMethodInRange(effectiveFrom, toMillis)
        val topProducts = saleRepository.getTopSellingProducts(effectiveFrom, toMillis, TOP_PRODUCTS_LIMIT)

        val sales = SalesReportSection(
            totalSoldCents = totalsByMethod.values.sum(),
            completedSalesCount = saleRepository.countCompletedSales(effectiveFrom, toMillis),
            totalsByMethod = totalsByMethod,
            topProducts = topProducts
        )

        val closedSessions = cashSessionRepository.getClosedSessionsInRange(effectiveFrom, toMillis)
        val movementTotals = cashSessionRepository.getMovementTotalsInRange(effectiveFrom, toMillis)
        val cash = CashReportSection(
            closedSessionsCount = closedSessions.size,
            totalOpeningCents = closedSessions.sumOf { it.openingBalanceCents },
            totalInformedCents = closedSessions.sumOf { it.informedCashCents ?: 0 },
            totalWithdrawalCents = movementTotals.withdrawalCents,
            totalSupplyCents = movementTotals.supplyCents,
            divergentSessions = closedSessions.filter { (it.divergenceCents ?: 0) != 0L }
        )

        val products = ProductsReportSection(
            lowStockProducts = productRepository.getLowStockProducts(),
            topProducts = topProducts
        )

        return GetReportResult.Success(
            ReportData(
                fromMillis = effectiveFrom,
                toMillis = toMillis,
                maxRangeDays = maxRangeDays,
                sales = sales,
                cash = cash,
                products = products
            )
        )
    }
}
