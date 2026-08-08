package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Fila durável de envio de vendas pro SaaS (outbox pattern). Cada venda
// concluída vira uma linha aqui, com o JSON já pronto (payload) e um
// transactionUuid estável — reenviar usa o mesmo UUID, então o servidor
// deduplica por (company, terminal, transaction_uuid). Sobrevive a fechar o
// app: nenhuma venda se perde mesmo sem internet. Índice único em saleId
// garante que a mesma venda nunca entra duas vezes na fila.
@Entity(
    tableName = "sale_outbox",
    indices = [
        Index(value = ["saleId"], unique = true),
        Index("status")
    ]
)
data class SaleOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val transactionUuid: String,
    val payload: String,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
    }
}
