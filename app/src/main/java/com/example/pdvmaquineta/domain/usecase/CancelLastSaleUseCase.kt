package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.payment.PaymentGateway
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.payment.RevertResult
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import com.example.pdvmaquineta.domain.repository.ProductRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed class CancelLastSaleResult {
    data class Success(val sale: Sale) : CancelLastSaleResult()
    /** Nao ha venda concluida nesta sessao de caixa. */
    data object NoSaleToCancel : CancelLastSaleResult()
    data object ReasonRequired : CancelLastSaleResult()
    /** A configuracao exige supervisor e nenhum autorizou. */
    data object AuthorizationRequired : CancelLastSaleResult()
    /**
     * A venda foi paga em cartao/PIX mas nao tem o identificador da transacao
     * (vendas gravadas antes da v16 do banco). Nao ha como estornar por aqui.
     */
    data object CannotRevertLegacySale : CancelLastSaleResult()
    /** A adquirente nao confirmou o estorno. A venda continua valida. */
    data class RevertFailed(val reason: String) : CancelLastSaleResult()
}

/**
 * Cancela a ultima venda concluida da sessao de caixa aberta, estornando na
 * adquirente antes de mexer em qualquer coisa.
 *
 * A ORDEM E O CORACAO DESTE CASO DE USO. O estorno vem primeiro e a venda so e
 * cancelada se a adquirente confirmar. Ao contrario, uma falha no estorno
 * deixaria a venda cancelada nos relatorios com o dinheiro cobrado do cliente —
 * um furo silencioso, que e pior do que nao ter a funcionalidade.
 *
 * Dinheiro nao passa por adquirente: cancela direto.
 */
class CancelLastSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val productRepository: ProductRepository,
    private val undoLoyaltyRedemptionUseCase: UndoLoyaltyRedemptionUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager,
    private val saleSyncQueue: SaleSyncQueue
) {
    suspend operator fun invoke(
        cashSessionId: Long,
        reason: String,
        requireSupervisor: Boolean,
        authorizedByUsername: String?
    ): CancelLastSaleResult {
        if (reason.isBlank()) return CancelLastSaleResult.ReasonRequired
        if (requireSupervisor && authorizedByUsername == null) {
            return CancelLastSaleResult.AuthorizationRequired
        }

        val sale = saleRepository.findLastCompleted(cashSessionId)
            ?: return CancelLastSaleResult.NoSaleToCancel

        // ---------- 1. estorno na adquirente ----------
        val payments = paymentRepository.approvedPaymentsForSale(sale.id)
        for (payment in payments) {
            if (payment.method == PaymentMethod.CASH) continue
            val nsuRequest = payment.nsuRequest
                ?: return CancelLastSaleResult.CannotRevertLegacySale
            when (val revert = paymentGateway.revertTransaction(nsuRequest)) {
                RevertResult.Reverted -> Unit
                is RevertResult.Failed ->
                    return CancelLastSaleResult.RevertFailed(revert.reason)
                RevertResult.NotApplicable ->
                    return CancelLastSaleResult.RevertFailed(
                        "Este terminal nao consegue estornar esta forma de pagamento"
                    )
            }
        }

        // ---------- 2. so agora a venda deixa de valer ----------
        saleRepository.cancelSale(sale.id, reason)

        // Devolve o estoque: esta venda ja tinha baixado quando foi paga.
        val items = saleRepository.observeItems(sale.id).first()
        for (item in items) {
            productRepository.incrementStock(item.productId, item.quantity)
        }

        undoLoyaltyRedemptionUseCase(sale, LoyaltyRedemptionReversalReason.SALE_CANCELLED)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user

        // Avisa a retaguarda reusando o transaction_uuid original.
        runCatching {
            saleSyncQueue.enqueueCancellation(
                saleId = sale.id,
                reason = reason,
                cancelledBy = authorizedByUsername ?: actor?.username,
                cancelledAtMillis = System.currentTimeMillis()
            )
        }

        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.SALE_CANCELLED,
                    detail = buildString {
                        append("Venda concluída #").append(sale.id)
                        append("; Motivo: ").append(reason)
                        if (authorizedByUsername != null) {
                            append("; Autorizado por: ").append(authorizedByUsername)
                        }
                        if (payments.any { it.method != PaymentMethod.CASH }) {
                            append("; Estornado na adquirente")
                        }
                    },
                    success = true
                )
            )
        }

        return CancelLastSaleResult.Success(sale)
    }
}
