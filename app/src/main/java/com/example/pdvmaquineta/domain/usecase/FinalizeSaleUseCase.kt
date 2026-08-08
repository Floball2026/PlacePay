package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

// Fase 4 ainda não existe: aqui a venda só migra pra AWAITING_PAYMENT.
// A confirmação de pagamento (e o status COMPLETED) vêm com seu próprio
// evento de auditoria quando o gateway mock/real for implementado.
class FinalizeSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val getActiveTerminalConfigUseCase: GetActiveTerminalConfigUseCase,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(sale: Sale, totalCents: Long): Sale {
        val config = getActiveTerminalConfigUseCase()
        if (config != null) {
            saleRepository.setTerminalSnapshot(sale.id, config.terminalName, config.storeName)
        }
        val finalized = saleRepository.finalizeSale(sale.id)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.SALE_FINALIZED,
                    detail = "Total: $totalCents centavos",
                    success = true
                )
            )
        }
        return finalized
    }
}
