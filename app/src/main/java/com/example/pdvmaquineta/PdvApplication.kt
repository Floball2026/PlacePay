package com.example.pdvmaquineta

import android.app.Application
import com.example.pdvmaquineta.data.di.ApplicationScope
import com.example.pdvmaquineta.data.sync.HeartbeatManager
import com.example.pdvmaquineta.data.sync.PeriodicSyncManager
import com.example.pdvmaquineta.data.sync.SyncRepository
import com.example.pdvmaquineta.data.sync.SyncSettings
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PdvApplication : Application() {

    // Hilt nao injeta a propria Application por construtor; usamos um EntryPoint
    // para pegar a fila e o escopo de app e drenar vendas pendentes no boot.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncBootstrap {
        fun saleSyncQueue(): SaleSyncQueue
        fun heartbeatManager(): HeartbeatManager
        fun periodicSyncManager(): PeriodicSyncManager
        fun syncRepository(): SyncRepository
        fun syncSettings(): SyncSettings
        @ApplicationScope fun appScope(): CoroutineScope
    }

    override fun onCreate() {
        super.onCreate()
        val entry = EntryPointAccessors.fromApplication(this, SyncBootstrap::class.java)
        entry.appScope().launch {
            runCatching { entry.saleSyncQueue().reconcileAndFlush() }
            // Atualiza operadores no boot pra a tela de login refletir a web.
            if (entry.syncSettings().isActivated) {
                runCatching { entry.syncRepository().pullOperators() }
                runCatching { entry.syncRepository().pullCustomers() }
            }
        }
        // Inicia o batimento periodico (sinal de vida do terminal).
        entry.heartbeatManager().start()
        // Inicia a sincronizacao periodica (intervalo vem da config remota).
        entry.periodicSyncManager().start()
    }
}
