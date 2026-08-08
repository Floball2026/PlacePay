package com.example.pdvmaquineta.data.di

import com.example.pdvmaquineta.data.repository.AuditRepositoryImpl
import com.example.pdvmaquineta.data.repository.CashSessionRepositoryImpl
import com.example.pdvmaquineta.data.repository.CustomerRepositoryImpl
import com.example.pdvmaquineta.data.repository.LoyaltyConfigRepositoryImpl
import com.example.pdvmaquineta.data.repository.LoyaltyTransactionRepositoryImpl
import com.example.pdvmaquineta.data.repository.PaymentRepositoryImpl
import com.example.pdvmaquineta.data.repository.ProductRepositoryImpl
import com.example.pdvmaquineta.data.repository.SaleRepositoryImpl
import com.example.pdvmaquineta.data.repository.TerminalConfigRepositoryImpl
import com.example.pdvmaquineta.data.repository.UserRepositoryImpl
import com.example.pdvmaquineta.data.session.InMemorySessionManager
import com.example.pdvmaquineta.data.sync.SaleOutboxRepository
import com.example.pdvmaquineta.domain.repository.AuditRepository
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import com.example.pdvmaquineta.domain.repository.CustomerRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyConfigRepository
import com.example.pdvmaquineta.domain.repository.LoyaltyTransactionRepository
import com.example.pdvmaquineta.domain.repository.PaymentRepository
import com.example.pdvmaquineta.domain.repository.ProductRepository
import com.example.pdvmaquineta.domain.repository.SaleRepository
import com.example.pdvmaquineta.domain.repository.TerminalConfigRepository
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import com.example.pdvmaquineta.domain.sync.SaleSyncQueue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSaleSyncQueue(impl: SaleOutboxRepository): SaleSyncQueue

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: InMemorySessionManager): SessionManager

    @Binds
    @Singleton
    abstract fun bindCashSessionRepository(impl: CashSessionRepositoryImpl): CashSessionRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindLoyaltyConfigRepository(impl: LoyaltyConfigRepositoryImpl): LoyaltyConfigRepository

    @Binds
    @Singleton
    abstract fun bindLoyaltyTransactionRepository(impl: LoyaltyTransactionRepositoryImpl): LoyaltyTransactionRepository

    @Binds
    @Singleton
    abstract fun bindTerminalConfigRepository(impl: TerminalConfigRepositoryImpl): TerminalConfigRepository
}
