package com.example.pdvmaquineta.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Guarda o Codigo de Ativacao do terminal na PayTime (usado no init() do SDK).
// SharedPreferences "paytime_config". Padrao = codigo do sandbox (EC PLACE WORK),
// trocavel via setActivationCode (futura tela de config do terminal).
@Singleton
class PaytimeConfigStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("paytime_config", Context.MODE_PRIVATE)

    fun getActivationCode(): String =
        prefs.getString(K_ACTIVATION, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_ACTIVATION

    fun setActivationCode(code: String) {
        prefs.edit().putString(K_ACTIVATION, code.trim()).apply()
    }

    private companion object {
        const val K_ACTIVATION = "paytime_activation_code"
        const val DEFAULT_ACTIVATION = "PLA69V9M"
    }
}
