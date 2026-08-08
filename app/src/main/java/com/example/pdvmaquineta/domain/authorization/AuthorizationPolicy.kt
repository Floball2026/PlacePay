package com.example.pdvmaquineta.domain.authorization

import com.example.pdvmaquineta.domain.model.UserRole

// Única fonte de verdade sobre o que cada perfil pode fazer (RF-002).
// A UI e os casos de uso só perguntam aqui — nunca decidem sozinhos.
object AuthorizationPolicy {

    private val rolePermissions: Map<UserRole, Set<Permission>> = mapOf(
        UserRole.OPERATOR to emptySet(),
        UserRole.SUPERVISOR to setOf(
            Permission.AUTHORIZE_DISCOUNT,
            Permission.AUTHORIZE_CANCELLATION,
            Permission.AUTHORIZE_CASH_WITHDRAWAL,
            Permission.AUTHORIZE_CASH_SUPPLY,
            Permission.VIEW_OPERATIONAL_REPORTS,
            Permission.MANAGE_PRODUCTS,
            Permission.MANAGE_BUSINESS_CONFIG
        ),
        UserRole.ADMIN to Permission.entries.toSet()
    )

    fun hasPermission(role: UserRole, permission: Permission): Boolean =
        rolePermissions[role]?.contains(permission) == true
}
