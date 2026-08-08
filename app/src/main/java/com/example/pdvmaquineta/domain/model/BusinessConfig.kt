package com.example.pdvmaquineta.domain.model

// Configuracao do negocio, editada LOCALMENTE no terminal por admin/supervisor
// (nao vem da retaguarda). Defaults = comportamento atual, entao terminal sem
// nada configurado funciona igual.
data class BusinessConfig(
    val enableCashback: Boolean = true,
    val requireCustomerCpf: Boolean = false,       // apenas lembra, nao obriga
    val maxDiscountPercent: Int = 100,
    // Formas de pagamento aceitas
    val payCash: Boolean = true,
    val payCredit: Boolean = true,
    val payDebit: Boolean = true,
    val payPix: Boolean = true,
    // Exigir supervisor
    val requireSupervisorPriceChange: Boolean = false,
    val requireSupervisorCancel: Boolean = false,
    // Cadencia (fixa por ora, nao exposta na tela)
    val syncIntervalSeconds: Int = 300,
    val heartbeatIntervalSeconds: Int = 120
)
