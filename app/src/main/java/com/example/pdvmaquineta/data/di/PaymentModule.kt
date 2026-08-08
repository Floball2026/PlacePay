package com.example.pdvmaquineta.data.di

import com.example.pdvmaquineta.data.payment.MockPaymentGateway
import com.example.pdvmaquineta.domain.payment.PaymentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Separado do RepositoryModule de propósito: PaymentGateway é uma integração
// externa (a futura Plasyplay), não acesso a dado local — trocar a
// implementação aqui é o único lugar que muda quando o SDK real chegar.
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentGateway(impl: MockPaymentGateway): PaymentGateway
}
