package com.example.pdvmaquineta.domain.payment

sealed class PaymentResult {
    data class Approved(val transactionId: String, val changeCents: Long = 0) : PaymentResult()
    data class Declined(val reason: String) : PaymentResult()
    data object Timeout : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}
