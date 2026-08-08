package com.example.pdvmaquineta.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Guarda a configuração de integração com o SaaS (URL, token do terminal e ids
// recebidos na ativação). SharedPreferences: leitura síncrona (o interceptor de
// rede precisa disso) e sem migração de banco.
@Singleton
class SyncSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("pdv_sync", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "") ?: ""
        set(v) { prefs.edit().putString("base_url", v).apply() }

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(v) { prefs.edit().putString("token", v).apply() }

    var companyId: String
        get() = prefs.getString("company_id", "") ?: ""
        set(v) { prefs.edit().putString("company_id", v).apply() }

    var storeId: String
        get() = prefs.getString("store_id", "") ?: ""
        set(v) { prefs.edit().putString("store_id", v).apply() }

    var terminalId: String
        get() = prefs.getString("terminal_id", "") ?: ""
        set(v) { prefs.edit().putString("terminal_id", v).apply() }

    var deviceId: String
        get() = prefs.getString("device_id", "") ?: ""
        set(v) { prefs.edit().putString("device_id", v).apply() }

    // Marca da última carga de produtos (para o sync incremental ?since=).
    var productsSince: String
        get() = prefs.getString("products_since", "") ?: ""
        set(v) { prefs.edit().putString("products_since", v).apply() }

    // Marca da ultima carga de operadores (sync incremental ?since=).
    var operatorsSince: String
        get() = prefs.getString("operators_since", "") ?: ""
        set(v) { prefs.edit().putString("operators_since", v).apply() }

    // Marca da ultima carga de clientes (sync incremental ?since=).
    var customersSince: String
        get() = prefs.getString("customers_since", "") ?: ""
        set(v) { prefs.edit().putString("customers_since", v).apply() }

    // Marca do ultimo sync bem-sucedido (venda enviada) — reportado no heartbeat.
    var lastSyncAt: String
        get() = prefs.getString("last_sync_at", "") ?: ""
        set(v) { prefs.edit().putString("last_sync_at", v).apply() }

    // Recebidos no heartbeat: versao de config aplicavel e menor versao de app
    // aceita. Guardados pra checagem de versao/config remota (fatias futuras).
    var lastConfigVersion: Int
        get() = prefs.getInt("last_config_version", 0)
        set(v) { prefs.edit().putInt("last_config_version", v).apply() }

    var minAppVersionCode: Int
        get() = prefs.getInt("min_app_version_code", 0)
        set(v) { prefs.edit().putInt("min_app_version_code", v).apply() }

    // Ultima versao de config ja aplicada e confirmada (ack) ao servidor.
    var ackedConfigVersion: Int
        get() = prefs.getInt("acked_config_version", 0)
        set(v) { prefs.edit().putInt("acked_config_version", v).apply() }

    // Payload da config remota (JSON serializado). Guardado para uso futuro
    // (chaves de configuracao ainda nao definidas).
    var configPayload: String
        get() = prefs.getString("config_payload", "") ?: ""
        set(v) { prefs.edit().putString("config_payload", v).apply() }

    val isActivated: Boolean get() = token.isNotBlank()

    fun saveActivation(
        baseUrl: String,
        token: String,
        companyId: String,
        storeId: String,
        terminalId: String,
        deviceId: String
    ) {
        prefs.edit()
            .putString("base_url", baseUrl)
            .putString("token", token)
            .putString("company_id", companyId)
            .putString("store_id", storeId)
            .putString("terminal_id", terminalId)
            .putString("device_id", deviceId)
            .apply()
    }
}
