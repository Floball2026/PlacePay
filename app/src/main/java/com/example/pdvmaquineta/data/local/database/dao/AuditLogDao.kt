package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.pdvmaquineta.data.local.database.entity.AuditLogEntity

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entry: AuditLogEntity)
}
