package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.PaymentStatus
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.payment.PaymentGateway
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.payment.PaymentRequest
import com.example.pdvmaquineta.domain.payment.PaymentResult
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import javax.inject.Inject

sealed class ProcessPaymentResult {
    data class Approved(val payment: Payment) : ProcessPaymentResult()
    data class Declined(val reason: String) : ProcessPaymentResult()
    data object Timeout : ProcessPaymentResult()
}

class ProcessPaymentUseCase @Inject constructor(
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val saleRepository: SaleRepository,
    private val earnLoyaltyUseCase: EarnLoyaltyUseCase,
    private val decrementStockForSaleUseCase: DecrementStockForSaleUseCase,
    private val saleSyncQueue: SaleSyncQueue,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        sale: Sale,
        method: PaymentMethod,
        amountCents: Long,
        receivedCents: Long?
    ): ProcessPaymentResult {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        val result = paymentGateway.charge(PaymentRequest(method, amountCents, receivedCents))

        return when (result) {
            is PaymentResult.Approved -> {
                val payment = paymentRepository.recordPayment(
                    saleId = sale.id,
                    method = method,
                    amountCents = amountCents,
                    receivedCents = receivedCents,
                    changeCents = result.changeCents,
                    status = PaymentStatus.APPROVED,
                    transactionId = result.transactionId,
                    declineReason = null
                )
                saleRepository.completeSale(sale.id)
                // Enfileira a venda pro SaaS (grava local; envio em segundo
                // plano). Nao bloqueia a confirmacao do pagamento.
                saleSyncQueue.enqueue(sale.id)
                earnLoyaltyUseCase(sale, amountCents)
                decrementStockForSaleUseCase(sale.id)
                if (actor != null) {
                    auditRepository.log(
                        AuditEntry(
                            userId = actor.id,
                            username = actor.username,
                            action = AuditAction.PAYMENT_CONFIRMED,
                            detail = "Forma: $method; Total: $amountCents centavos; " +
                                "Transação: ${result.transactionId}",
                            success = true
                        )
                    )
                }
                ProcessPaymentResult.Approved(payment)
            }

            is PaymentResult.Declined -> {
                recordFailure(sale.id, method, amountCents, receivedCents, PaymentStatus.DECLINED, result.reason)
                logDecline(actor, method, result.reason)
                ProcessPaymentResult.Declined(result.reason)
            }

            is PaymentResult.Error -> {
                recordFailure(sale.id, method, amountCents, receivedCents, PaymentStatus.DECLINED, result.message)
                logDecline(actor, method, result.message)
                ProcessPaymentResult.Declined(result.message)
            }

            PaymentResult.Timeout -> {
                recordFailure(sale.id, method, amountCents, receivedCents, PaymentStatus.TIMEOUT, "Tempo esgotado")
                logDecline(actor, method, "Tempo esgotado")
                ProcessPaymentResult.Timeout
            }
        }
    }

    private suspend fun recordFailure(
        saleId: Long,
        method: PaymentMethod,
        amountCents: Long,
        receivedCents: Long?,
        status: PaymentStatus,
        reason: String
    ) {
        paymentRepository.recordPayment(
            saleId = saleId,
            method = method,
            amountCents = amountCents,
            receivedCents = receivedCents,
            changeCents = null,
            status = status,
            transactionId = null,
            declineReason = reason
        )
    }

    private suspend fun logDecline(actor: User?, method: PaymentMethod, reason: String) {
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.PAYMENT_DECLINED,
                    detail = "Forma: $method; Motivo: $reason",
                    success = false
                )
            )
        }
    }
}
