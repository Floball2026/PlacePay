package com.example.pdvmaquineta.data.sync

import com.example.pdvmaquineta.data.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

// Sincronizacao periodica em segundo plano (produtos + operadores + clientes),
// no intervalo definido pela config remota (sync_interval_seconds). Tudo
// incremental (?since=), entao normalmente e leve/vazio. So roda se ativado.
// Faz mudancas feitas na retaguarda chegarem sozinhas, mesmo com o app aberto.
@Singleton
class PeriodicSyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val settings: SyncSettings,
    private val config: BusinessConfigStore,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            while (true) {
                if (settings.isActivated) {
                    runCatching { syncRepository.pullProducts() }
                    runCatching { syncRepository.pullOperators() }
                    runCatching { syncRepository.pullCustomers() }
                }
                val seconds = config.get().syncIntervalSeconds.coerceIn(30, 3600)
                delay(seconds * 1000L)
            }
        }
    }
}
