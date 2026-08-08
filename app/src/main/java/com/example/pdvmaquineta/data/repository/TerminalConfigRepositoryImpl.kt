package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.TerminalConfigDao
import com.example.pdvmaquineta.data.local.database.entity.TerminalConfigEntity
import com.example.pdvmaquineta.domain.model.PrinterType
import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.model.TerminalEnvironment
import com.example.pdvmaquineta.domain.repository.TerminalConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TerminalConfigRepositoryImpl @Inject constructor(
    private val terminalConfigDao: TerminalConfigDao
) : TerminalConfigRepository {

    override suspend fun getConfig(): TerminalConfig? = terminalConfigDao.find()?.toDomain()

    override fun observeConfig(): Flow<TerminalConfig?> =
        terminalConfigDao.observe().map { it?.toDomain() }

    override suspend fun updateConfig(
        terminalName: String,
        storeName: String,
        environment: TerminalEnvironment,
        printerType: PrinterType,
        updatedByUserId: Long?
    ): TerminalConfig {
        val now = System.currentTimeMillis()
        val existing = terminalConfigDao.find()
        val entity = (existing ?: TerminalConfigEntity(
            terminalName = terminalName,
            storeName = storeName,
            environment = environment.name,
            printerType = printerType.name,
            updatedAt = now,
            updatedByUserId = updatedByUserId
        )).copy(
            terminalName = terminalName,
            storeName = storeName,
            environment = environment.name,
            printerType = printerType.name,
            updatedAt = now,
            updatedByUserId = updatedByUserId
        )

        val id = if (existing != null) {
            terminalConfigDao.update(entity)
            existing.id
        } else {
            terminalConfigDao.insert(entity)
        }
        return entity.copy(id = id).toDomain()
    }

    private fun TerminalConfigEntity.toDomain() = TerminalConfig(
        id = id,
        terminalName = terminalName,
        storeName = storeName,
        environment = TerminalEnvironment.valueOf(environment),
        printerType = PrinterType.valueOf(printerType),
        updatedAt = updatedAt,
        updatedByUserId = updatedByUserId
    )
}
