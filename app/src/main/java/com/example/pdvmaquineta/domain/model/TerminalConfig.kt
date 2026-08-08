package com.example.pdvmaquineta.domain.model

enum class TerminalEnvironment {
    HOMOLOGACAO,
    PRODUCAO
}

enum class PrinterType {
    NONE,
    INTEGRATED,
    BLUETOOTH
}

data class TerminalConfig(
    val id: Long,
    val terminalName: String,
    val storeName: String,
    val environment: TerminalEnvironment,
    val printerType: PrinterType,
    val updatedAt: Long,
    val updatedByUserId: Long?
)
