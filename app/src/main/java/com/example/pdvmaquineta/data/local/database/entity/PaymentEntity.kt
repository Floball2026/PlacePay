package com.example.pdvmaquineta.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val method: String,
    val amountCents: Long,
    val receivedCents: Long?,
    val changeCents: Long?,
    val status: String,
    val transactionId: String?,
    // ---- Identificacao da transacao na adquirente (cartao/PIX) ----
    // `nsuRequest` e a chave IMUTAVEL da transacao dentro do SDK da PayTime
    // (primeiro parametro do construtor de PayOsSdkTransactionStore, sem
    // setter). E ele que `revertTransaction` e `getTransaction` esperam —
    // sem guarda-lo, uma venda paga no cartao nao tem como ser estornada.
    val nsuRequest: String? = null,
    // NSU do lado da adquirente e nome dela: e por estes dois que a venda se
    // concilia com o extrato.
    val nsuAcquirer: String? = null,
    val acquirerName: String? = null,
    val cardBrand: String? = null,
    val panMasked: String? = null,
    val installments: Int? = null,
    val declineReason: String?,
    val createdAt: Long
)
