package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    suspend fun findByPhone(phone: String): Customer?
    suspend fun findById(id: Long): Customer?
    fun search(query: String): Flow<List<Customer>>
    suspend fun createCustomer(name: String, phone: String, document: String?): Customer
    suspend fun updateCustomer(id: Long, name: String, phone: String, document: String?): Customer
}
