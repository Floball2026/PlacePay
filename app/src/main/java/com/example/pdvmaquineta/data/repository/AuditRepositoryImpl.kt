package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.AuditLogDao
import com.example.pdvmaquineta.data.local.database.entity.AuditLogEntity
import com.example.pdvmaquineta.domain.model.AuditEntry
import com.example.pdvmaquineta.domain.repository.AuditRepository
import javax.inject.Inject

class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao
) : AuditRepository {

    override suspend fun log(entry: AuditEntry) {
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = entry.timestamp,
                userId = entry.userId,
                username = entry.username,
                action = entry.action.name,
                detail = entry.detail,
                success = entry.success
            )
        )
    }
}
