package com.example.pdvmaquineta.domain.model

enum class UserRole {
    OPERATOR,
    SUPERVISOR,
    ADMIN
}

data class User(
    val id: Long,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val active: Boolean,
    val mustChangePin: Boolean,
    val themeTone: ThemeTone
)
