package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.UserDao
import com.example.pdvmaquineta.data.local.database.entity.UserEntity
import com.example.pdvmaquineta.data.security.PasswordHasher
import com.example.pdvmaquineta.data.security.PinVerifier
import com.example.pdvmaquineta.domain.model.CredentialResult
import com.example.pdvmaquineta.domain.model.ThemeTone
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import com.example.pdvmaquineta.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun validateCredentials(username: String, password: String): CredentialResult {
        val entity = userDao.findByUsername(username) ?: return CredentialResult.InvalidCredentials
        if (!entity.active) return CredentialResult.UserInactive

        val matches = PasswordHasher.matches(password, entity.passwordSalt, entity.passwordHash)
        return if (matches) {
            CredentialResult.Success(entity.toDomain())
        } else {
            CredentialResult.InvalidCredentials
        }
    }

    override suspend fun validatePin(username: String, pin: String): CredentialResult {
        val entity = userDao.findByUsername(username) ?: return CredentialResult.InvalidCredentials
        if (!entity.active) return CredentialResult.UserInactive

        val hash = entity.pinHash ?: return CredentialResult.InvalidCredentials

        // Suporta PIN local (PBKDF2) e PIN vindo do SaaS (bcrypt) — ver PinVerifier.
        val matches = PinVerifier.verify(pin, hash, entity.pinSalt)
        return if (matches) {
            CredentialResult.Success(entity.toDomain())
        } else {
            CredentialResult.InvalidCredentials
        }
    }

    override suspend fun hasPinSet(username: String): Boolean =
        userDao.findByUsername(username)?.pinHash != null

    override suspend fun existsByUsername(username: String): Boolean =
        userDao.findByUsername(username) != null

    override suspend fun findById(id: Long): User? = userDao.findById(id)?.toDomain()

    override fun observeAll(): Flow<List<User>> =
        userDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createUser(
        username: String,
        displayName: String,
        role: UserRole,
        createdByUserId: Long?
    ): User {
        // passwordHash/passwordSalt continuam NOT NULL no schema, mas usuários
        // criados pela administração (Fase 5a) logam só por PIN — geramos uma
        // senha aleatória, nunca exposta nem utilizável, só pra satisfazer a
        // coluna sem mudar o schema além do que a migração 4->5 já prevê.
        val throwaway = PasswordHasher.hash(UUID.randomUUID().toString())
        val now = System.currentTimeMillis()
        val entity = UserEntity(
            username = username,
            displayName = displayName,
            passwordHash = throwaway.hash,
            passwordSalt = throwaway.salt,
            role = role.name,
            active = true,
            mustChangePin = false,
            createdByUserId = createdByUserId,
            createdAt = now,
            updatedAt = now
        )
        val id = userDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun updateUser(id: Long, displayName: String, role: UserRole): User {
        val existing = userDao.findById(id) ?: error("Usuário $id não encontrado")
        val updated = existing.copy(
            displayName = displayName,
            role = role.name,
            updatedAt = System.currentTimeMillis()
        )
        userDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        val existing = userDao.findById(id) ?: return
        userDao.update(existing.copy(active = active, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setMustChangePin(id: Long, mustChange: Boolean) {
        val existing = userDao.findById(id) ?: return
        userDao.update(existing.copy(mustChangePin = mustChange, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setPin(id: Long, pin: String) {
        val existing = userDao.findById(id) ?: return
        val hashed = PasswordHasher.hash(pin)
        userDao.update(
            existing.copy(
                pinHash = hashed.hash,
                pinSalt = hashed.salt,
                mustChangePin = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setThemeTone(id: Long, tone: ThemeTone) {
        val existing = userDao.findById(id) ?: return
        userDao.update(existing.copy(themeTone = tone.name, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun countActiveByRole(role: UserRole): Int =
        userDao.countActiveByRole(role.name)

    private fun UserEntity.toDomain() = User(
        id = id,
        username = username,
        displayName = displayName,
        role = UserRole.valueOf(role),
        active = active,
        mustChangePin = mustChangePin,
        themeTone = ThemeTone.valueOf(themeTone)
    )
}
