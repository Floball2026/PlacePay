package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pdvmaquineta.data.local.database.entity.PaymentEntity

data class PaymentMethodTotal(
    val method: String,
    val totalCents: Long
)

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Query(
        "SELECT p.method as method, COALESCE(SUM(p.amountCents), 0) as totalCents " +
            "FROM payments p INNER JOIN sales s ON s.id = p.saleId " +
            "WHERE s.cashSessionId = :cashSessionId AND p.status = 'APPROVED' " +
            "GROUP BY p.method"
    )
    suspend fun sumApprovedByMethod(cashSessionId: Long): List<PaymentMethodTotal>

    @Query(
        "SELECT * FROM payments WHERE saleId = :saleId AND status = 'APPROVED' " +
            "ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun findApprovedForSale(saleId: Long): PaymentEntity?

    // Todos os pagamentos aprovados da venda, em ordem cronologica.
    // A venda pode ter mais de um (pagamento dividido); o envio ao SaaS precisa
    // de todos, senao a soma dos pagamentos nao fecha com o total.
    @Query(
        "SELECT * FROM payments WHERE saleId = :saleId AND status = 'APPROVED' " +
            "ORDER BY createdAt ASC"
    )
    suspend fun findAllApprovedForSale(saleId: Long): List<PaymentEntity>

    // Mesma agregação de sumApprovedByMethod, por período em vez de sessão de
    // caixa — usado no relatório (Fase 7b).
    @Query(
        "SELECT method as method, COALESCE(SUM(amountCents), 0) as totalCents " +
            "FROM payments " +
            "WHERE status = 'APPROVED' " +
            "AND (:fromMillis IS NULL OR createdAt >= :fromMillis) " +
            "AND (:toMillis IS NULL OR createdAt <= :toMillis) " +
            "GROUP BY method"
    )
    suspend fun sumApprovedByMethodInRange(fromMillis: Long?, toMillis: Long?): List<PaymentMethodTotal>
}
