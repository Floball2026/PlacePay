package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import javax.inject.Inject

data class CashOverview(
    val session: CashSession,
    val expectedCashCents: Long,
    val withdrawalTotalCents: Long,
    val supplyTotalCents: Long,
    val cashSalesCents: Long,
    val cardSalesCents: Long,
    val pixSalesCents: Long
)

class GetCashOverviewUseCase @Inject constructor(
    private val cashSessionRepository: CashSessionRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(session: CashSession): CashOverview {
        val movementTotals = cashSessionRepository.getMovementTotals(session.id)
        val paymentTotals = paymentRepository.getApprovedTotalsByMethod(session.id)

        val cashSalesCents = paymentTotals[PaymentMethod.CASH] ?: 0
        val cardSalesCents = (paymentTotals[PaymentMethod.CREDIT_CARD] ?: 0) +
            (paymentTotals[PaymentMethod.DEBIT_CARD] ?: 0)
        val pixSalesCents = paymentTotals[PaymentMethod.PIX] ?: 0

        val expectedCashCents = session.openingBalanceCents + movementTotals.supplyCents -
            movementTotals.withdrawalCents + cashSalesCents

        return CashOverview(
            session = session,
            expectedCashCents = expectedCashCents,
            withdrawalTotalCents = movementTotals.withdrawalCents,
            supplyTotalCents = movementTotals.supplyCents,
            cashSalesCents = cashSalesCents,
            cardSalesCents = cardSalesCents,
            pixSalesCents = pixSalesCents
        )
    }
}
