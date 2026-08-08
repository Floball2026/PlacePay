package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import javax.inject.Inject

sealed class UpdateCustomerResult {
    data class Success(val customer: Customer) : UpdateCustomerResult()
    data object InvalidData : UpdateCustomerResult()
    data object PhoneTaken : UpdateCustomerResult()
}

class UpdateCustomerUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(id: Long, name: String, phone: String, document: String?): UpdateCustomerResult {
        if (name.isBlank() || phone.isBlank()) return UpdateCustomerResult.InvalidData

        val existingWithPhone = customerRepository.findByPhone(phone)
        if (existingWithPhone != null && existingWithPhone.id != id) return UpdateCustomerResult.PhoneTaken

        val updated = customerRepository.updateCustomer(id, name, phone, document)
        return UpdateCustomerResult.Success(updated)
    }
}
