package com.example.pdvmaquineta.data.di

import com.example.pdvmaquineta.data.receipt.MockDigitalReceiptSender
import com.example.pdvmaquineta.data.receipt.PaytimeReceiptPrinter
import com.example.pdvmaquineta.domain.receipt.DigitalReceiptSender
import com.example.pdvmaquineta.domain.receipt.ReceiptPrinter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Separado do RepositoryModule de propósito: ReceiptPrinter/DigitalReceiptSender
// são integrações externas (hardware de impressão, provedor de WhatsApp/SMS/
// e-mail), não acesso a dado local — trocar a implementação aqui é o único
// lugar que muda quando o modelo real for definido. Mesmo padrão de
// PaymentModule.
@Module
@InstallIn(SingletonComponent::class)
abstract class ReceiptModule {

    @Binds
    @Singleton
    abstract fun bindReceiptPrinter(impl: PaytimeReceiptPrinter): ReceiptPrinter

    @Binds
    @Singleton
    abstract fun bindDigitalReceiptSender(impl: MockDigitalReceiptSender): DigitalReceiptSender
}
