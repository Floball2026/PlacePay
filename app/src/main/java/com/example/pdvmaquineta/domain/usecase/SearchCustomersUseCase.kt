package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SearchCustomersUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(query: String): Flow<List<Customer>> = customerRepository.search(query)
}
