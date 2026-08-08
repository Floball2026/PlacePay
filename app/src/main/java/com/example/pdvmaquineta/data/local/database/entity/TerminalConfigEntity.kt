package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Linha única (singleton): a repository sempre lê/atualiza a primeira (e
// única) linha desta tabela, nunca mantém histórico de reconfigurações.
@Entity(tableName = "terminal_config")
data class TerminalConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val terminalName: String,
    val storeName: String,
    val environment: String,
    val printerType: String,
    val updatedAt: Long,
    val updatedByUserId: Long?
)
