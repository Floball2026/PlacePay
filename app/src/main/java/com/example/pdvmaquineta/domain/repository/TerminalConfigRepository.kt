package com.example.pdvmaquineta.domain.repository

import com.example.pdvmaquineta.domain.model.PrinterType
import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.model.TerminalEnvironment
import kotlinx.coroutines.flow.Flow

interface TerminalConfigRepository {
    suspend fun getConfig(): TerminalConfig?
    fun observeConfig(): Flow<TerminalConfig?>

    suspend fun updateConfig(
        terminalName: String,
        storeName: String,
        environment: TerminalEnvironment,
        printerType: PrinterType,
        updatedByUserId: Long?
    ): TerminalConfig
}
