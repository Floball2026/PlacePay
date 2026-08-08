package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.CustomerDao
import com.example.pdvmaquineta.data.local.database.entity.CustomerEntity
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override suspend fun findByPhone(phone: String): Customer? = customerDao.findByPhone(phone)?.toDomain()

    override suspend fun findById(id: Long): Customer? = customerDao.findById(id)?.toDomain()

    override fun search(query: String): Flow<List<Customer>> =
        customerDao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun createCustomer(name: String, phone: String, document: String?): Customer {
        val now = System.currentTimeMillis()
        val entity = CustomerEntity(
            name = name,
            phone = phone,
            document = document,
            createdAt = now,
            updatedAt = now
        )
        val id = customerDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun updateCustomer(id: Long, name: String, phone: String, document: String?): Customer {
        val existing = customerDao.findById(id) ?: error("Cliente $id não encontrado")
        val updated = existing.copy(
            name = name,
            phone = phone,
            document = document,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.update(updated)
        return updated.toDomain()
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        phone = phone,
        document = document,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
