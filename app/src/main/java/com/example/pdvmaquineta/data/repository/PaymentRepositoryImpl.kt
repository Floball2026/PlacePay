package com.example.pdvmaquineta.data.repository

import com.example.pdvmaquineta.data.local.database.dao.PaymentDao
import com.example.pdvmaquineta.data.local.database.entity.PaymentEntity
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.PaymentStatus
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao
) : PaymentRepository {

    override suspend fun recordPayment(
        saleId: Long,
        method: PaymentMethod,
        amountCents: Long,
        receivedCents: Long?,
        changeCents: Long?,
        status: PaymentStatus,
        transactionId: String?,
        declineReason: String?
    ): Payment {
        val entity = PaymentEntity(
            saleId = saleId,
            method = method.name,
            amountCents = amountCents,
            receivedCents = receivedCents,
            changeCents = changeCents,
            status = status.name,
            transactionId = transactionId,
            declineReason = declineReason,
            createdAt = System.currentTimeMillis()
        )
        val id = paymentDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun getApprovedTotalsByMethod(cashSessionId: Long): Map<PaymentMethod, Long> =
        paymentDao.sumApprovedByMethod(cashSessionId).associate {
            PaymentMethod.valueOf(it.method) to it.totalCents
        }

    override suspend fun findApprovedForSale(saleId: Long): Payment? =
        paymentDao.findApprovedForSale(saleId)?.toDomain()

    override suspend fun getApprovedTotalsByMethodInRange(fromMillis: Long?, toMillis: Long?): Map<PaymentMethod, Long> =
        paymentDao.sumApprovedByMethodInRange(fromMillis, toMillis).associate {
            PaymentMethod.valueOf(it.method) to it.totalCents
        }

    private fun PaymentEntity.toDomain() = Payment(
        id = id,
        saleId = saleId,
        method = PaymentMethod.valueOf(method),
        amountCents = amountCents,
        receivedCents = receivedCents,
        changeCents = changeCents,
        status = PaymentStatus.valueOf(status),
        transactionId = transactionId,
        declineReason = declineReason,
        createdAt = createdAt
    )
}
