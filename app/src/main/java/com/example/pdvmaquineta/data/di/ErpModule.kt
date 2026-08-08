package com.example.pdvmaquineta.data.di

import com.example.pdvmaquineta.data.erp.MockErpIntegrationGateway
import com.example.pdvmaquineta.domain.erp.ErpIntegrationGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Separado do RepositoryModule de propósito: ErpIntegrationGateway é uma
// integração externa (RF-042, ERP ainda não escolhido pelo cliente final),
// não acesso a dado local — trocar a implementação aqui é o único lugar que
// muda quando o ERP real for definido. Mesmo padrão de PaymentModule/
// ReceiptModule.
@Module
@InstallIn(SingletonComponent::class)
abstract class ErpModule {

    @Binds
    @Singleton
    abstract fun bindErpIntegrationGateway(impl: MockErpIntegrationGateway): ErpIntegrationGateway
}
