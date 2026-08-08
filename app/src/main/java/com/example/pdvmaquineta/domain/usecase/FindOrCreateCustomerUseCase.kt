package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.AuditAction
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

// Usado tanto pelo mini-cadastro rápido na venda quanto por um cadastro
// completo pela tela de clientes — busca por telefone (índice único) e só
// cria se realmente não existir.
class FindOrCreateCustomerUseCase @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(name: String, phone: String, document: String? = null): Customer {
        val existing = customerRepository.findByPhone(phone)
        if (existing != null) return existing

        val created = customerRepository.createCustomer(name, phone, document)

        val actor = (sessionManager.state.value as? SessionState.Active)?.user
        if (actor != null) {
            auditRepository.log(
                AuditEntry(
                    userId = actor.id,
                    username = actor.username,
                    action = AuditAction.CUSTOMER_CREATED,
                    detail = "Cliente: $name; Telefone: $phone",
                    success = true
                )
            )
        }
        return created
    }
}
