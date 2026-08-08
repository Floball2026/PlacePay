package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.model.ThemeTone
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun validateCredentials(username: String, password: String): CredentialResult
    suspend fun validatePin(username: String, pin: String): CredentialResult
    suspend fun hasPinSet(username: String): Boolean
    suspend fun existsByUsername(username: String): Boolean
    suspend fun findById(id: Long): User?
    fun observeAll(): Flow<List<User>>

    suspend fun createUser(username: String, displayName: String, role: UserRole, createdByUserId: Long?): User
    suspend fun updateUser(id: Long, displayName: String, role: UserRole): User
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun setMustChangePin(id: Long, mustChange: Boolean)
    suspend fun setPin(id: Long, pin: String)
    suspend fun setThemeTone(id: Long, tone: ThemeTone)
    suspend fun countActiveByRole(role: UserRole): Int
}
