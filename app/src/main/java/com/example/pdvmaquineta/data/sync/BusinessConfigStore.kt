package com.example.pdvmaquineta.data.sync

import android.content.Context
import com.example.pdvmaquineta.domain.model.BusinessConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Guarda a configuracao do negocio LOCALMENTE (SharedPreferences "pdv_config").
// Leitura sincrona (o fluxo de venda consulta na hora) e sem migracao de banco.
// Editado pela tela de Configuracoes do negocio (admin/supervisor).
@Singleton
class BusinessConfigStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("pdv_config", Context.MODE_PRIVATE)

    fun get(): BusinessConfig {
        val d = BusinessConfig()
        return BusinessConfig(
            enableCashback = prefs.getBoolean(K_CASHBACK, d.enableCashback),
            requireCustomerCpf = prefs.getBoolean(K_REQ_CPF, d.requireCustomerCpf),
            maxDiscountPercent = prefs.getInt(K_MAX_DISCOUNT, d.maxDiscountPercent),
            payCash = prefs.getBoolean(K_PAY_CASH, d.payCash),
            payCredit = prefs.getBoolean(K_PAY_CREDIT, d.payCredit),
            payDebit = prefs.getBoolean(K_PAY_DEBIT, d.payDebit),
            payPix = prefs.getBoolean(K_PAY_PIX, d.payPix),
            requireSupervisorPriceChange = prefs.getBoolean(K_SUP_PRICE, d.requireSupervisorPriceChange),
            requireSupervisorCancel = prefs.getBoolean(K_SUP_CANCEL, d.requireSupervisorCancel)
            // intervalos ficam nos defaults do BusinessConfig
        )
    }

    fun save(config: BusinessConfig) {
        prefs.edit()
            .putBoolean(K_CASHBACK, config.enableCashback)
            .putBoolean(K_REQ_CPF, config.requireCustomerCpf)
            .putInt(K_MAX_DISCOUNT, config.maxDiscountPercent.coerceIn(0, 100))
            .putBoolean(K_PAY_CASH, config.payCash)
            .putBoolean(K_PAY_CREDIT, config.payCredit)
            .putBoolean(K_PAY_DEBIT, config.payDebit)
            .putBoolean(K_PAY_PIX, config.payPix)
            .putBoolean(K_SUP_PRICE, config.requireSupervisorPriceChange)
            .putBoolean(K_SUP_CANCEL, config.requireSupervisorCancel)
            .apply()
    }

    private companion object {
        const val K_CASHBACK = "enable_cashback"
        const val K_REQ_CPF = "require_customer_cpf"
        const val K_MAX_DISCOUNT = "max_discount_percent"
        const val K_PAY_CASH = "pay_cash"
        const val K_PAY_CREDIT = "pay_credit"
        const val K_PAY_DEBIT = "pay_debit"
        const val K_PAY_PIX = "pay_pix"
        const val K_SUP_PRICE = "req_sup_price_change"
        const val K_SUP_CANCEL = "req_sup_cancel"
    }
}
