package com.example.pdvmaquineta.domain.model

data class Customer(
    val id: Long,
    val name: String,
    val phone: String,
    val document: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
