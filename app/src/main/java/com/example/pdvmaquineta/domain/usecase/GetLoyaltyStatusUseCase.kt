package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.loyalty.LoyaltyEngineFactory
import com.example.pdvmaquineta.domain.loyalty.LoyaltyRedeemable
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import javax.inject.Inject

// Status atual do cliente pro modo ativo — usado pra exibir saldo/progresso
// na tela de venda antes de decidir resgatar.
class GetLoyaltyStatusUseCase @Inject constructor(
    private val loyaltyConfigRepository: LoyaltyConfigRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository
) {
    // saleNetCents é o que falta pagar depois do desconto manual (subtotal -
    // discountCents), não o subtotal bruto.
    suspend operator fun invoke(customerId: Long, saleNetCents: Long): LoyaltyRedeemable? {
        val config = loyaltyConfigRepository.getActive() ?: return null
        val engine = LoyaltyEngineFactory.create(config)
        val transactions = loyaltyTransactionRepository.findForCustomerSince(customerId, config.activatedAt)
        return engine.calculateRedeemable(transactions, saleNetCents)
    }
}
