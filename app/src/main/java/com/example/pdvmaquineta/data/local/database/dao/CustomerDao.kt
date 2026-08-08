package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE document = :document LIMIT 1")
    suspend fun findByDocument(document: String): CustomerEntity?

    @Query(
        "SELECT * FROM customers WHERE active = 1 AND " +
            "(name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' " +
            "OR document LIKE '%' || :query || '%') ORDER BY name"
    )
    fun search(query: String): Flow<List<CustomerEntity>>

    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)
}
