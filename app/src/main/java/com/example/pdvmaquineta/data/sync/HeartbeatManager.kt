package com.example.pdvmaquineta.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import com.example.pdvmaquineta.data.di.ApplicationScope
import com.example.pdvmaquineta.data.local.database.dao.SaleOutboxDao
import com.example.pdvmaquineta.data.sync.dto.PosHeartbeatInput
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

// Heartbeat: o terminal manda um "sinal de vida" periodico pro SaaS (a
// retaguarda ve quais caixas estao online, versao, bateria, vendas pendentes).
// Loop no escopo de app (nao numa tela) — continua enquanto o processo vive.
// So envia se o terminal ja foi ativado; falha de rede e silenciosa (o proximo
// tick tenta de novo). Nao bloqueia nada da venda.
@Singleton
class HeartbeatManager @Inject constructor(
    private val api: PosApiService,
    private val settings: SyncSettings,
    private val deviceInfo: DeviceInfoProvider,
    private val outboxDao: SaleOutboxDao,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val config: BusinessConfigStore
) {
    private val started = AtomicBoolean(false)

    fun start() {
        // Idempotente: um unico loop mesmo se onCreate rodar mais de uma vez.
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            while (true) {
                runCatching { sendHeartbeat() }
                // Intervalo vem da config remota (heartbeat_interval_seconds),
                // com piso de 30s. Obs: o servidor marca "online" so com
                // heartbeat < 3 min, entao valores > 180s deixam o terminal
                // piscando offline entre batidas.
                val seconds = config.get().heartbeatIntervalSeconds.coerceIn(30, 3600)
                delay(seconds * 1000L)
            }
        }
    }

    suspend fun sendHeartbeat() {
        if (!settings.isActivated) return
        val device = deviceInfo.build()
        val input = PosHeartbeatInput(
            deviceTime = iso().format(Date(System.currentTimeMillis())),
            appVersion = device.appVersion,
            appVersionCode = device.appVersionCode,
            battery = batteryLevel(),
            storageFreeMb = freeStorageMb(),
            network = networkType(),
            pendingTransactions = runCatching { outboxDao.countPending() }.getOrDefault(0),
            lastSyncAt = settings.lastSyncAt.ifBlank { null },
            operatorUsername = currentOperator(),
            cashStatus = null
        )
        val result = api.heartbeat(input)
        // Guarda pro futuro (checagem de versao minima e config remota).
        settings.lastConfigVersion = result.configVersion
        settings.minAppVersionCode = result.minAppVersionCode
    }

    private fun currentOperator(): String? = when (val s = sessionManager.state.value) {
        is SessionState.Active -> s.user.username
        is SessionState.Locked -> s.user.username
        else -> null
    }

    private fun batteryLevel(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct in 0..100) pct else null
    }

    private fun freeStorageMb(): Int? = runCatching {
        val stat = StatFs(context.filesDir.absolutePath)
        (stat.availableBytes / (1024L * 1024L)).toInt()
    }.getOrNull()

    private fun networkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unknown"
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    // ISO 8601 com fuso do dispositivo (hora local do caixa).
    private fun iso() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

}
