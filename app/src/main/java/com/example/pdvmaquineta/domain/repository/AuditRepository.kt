package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.AuditEntry

interface AuditRepository {
    suspend fun log(entry: AuditEntry)
}
