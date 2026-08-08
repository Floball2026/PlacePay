package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import javax.inject.Inject

class GetCustomerUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(id: Long): Customer? = customerRepository.findById(id)
}
