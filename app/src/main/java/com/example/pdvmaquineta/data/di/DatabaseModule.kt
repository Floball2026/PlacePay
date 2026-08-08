package com.example.pdvmaquineta.data.di

import android.content.Context
import androidx.room.Room
import com.example.pdvmaquineta.data.local.database.AdminSeedCallback
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import com.example.pdvmaquineta.data.local.database.dao.AuditLogDao
import com.example.pdvmaquineta.data.local.database.dao.CashMovementDao
import com.example.pdvmaquineta.data.local.database.dao.CashSessionDao
import com.example.pdvmaquineta.data.local.database.dao.CustomerDao
import com.example.pdvmaquineta.data.local.database.dao.LoyaltyConfigDao
import com.example.pdvmaquineta.data.local.database.dao.LoyaltyTransactionDao
import com.example.pdvmaquineta.data.local.database.dao.PaymentDao
import com.example.pdvmaquineta.data.local.database.dao.ProductDao
import com.example.pdvmaquineta.data.local.database.dao.SaleDao
import com.example.pdvmaquineta.data.local.database.dao.SaleItemDao
import com.example.pdvmaquineta.data.local.database.dao.SaleOutboxDao
import com.example.pdvmaquineta.data.local.database.dao.TerminalConfigDao
import com.example.pdvmaquineta.data.local.database.dao.UserDao
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_1_2
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_2_3
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_3_4
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_4_5
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_5_6
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_6_7
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_7_8
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_8_9
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_9_10
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_10_11
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_11_12
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_12_13
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_13_14
import com.example.pdvmaquineta.data.local.database.migration.MIGRATION_14_15
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "pdv.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePdvDatabase(@ApplicationContext context: Context): PdvDatabase =
        Room.databaseBuilder(context, PdvDatabase::class.java, DATABASE_NAME)
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15
            )
            .addCallback(AdminSeedCallback())
            .build()

    @Provides
    fun provideUserDao(database: PdvDatabase): UserDao = database.userDao()

    @Provides
    fun provideAuditLogDao(database: PdvDatabase): AuditLogDao = database.auditLogDao()

    @Provides
    fun provideCashSessionDao(database: PdvDatabase): CashSessionDao = database.cashSessionDao()

    @Provides
    fun provideCashMovementDao(database: PdvDatabase): CashMovementDao = database.cashMovementDao()

    @Provides
    fun provideProductDao(database: PdvDatabase): ProductDao = database.productDao()

    @Provides
    fun provideSaleDao(database: PdvDatabase): SaleDao = database.saleDao()

    @Provides
    fun provideSaleItemDao(database: PdvDatabase): SaleItemDao = database.saleItemDao()

    @Provides
    fun provideSaleOutboxDao(database: PdvDatabase): SaleOutboxDao = database.saleOutboxDao()

    @Provides
    fun providePaymentDao(database: PdvDatabase): PaymentDao = database.paymentDao()

    @Provides
    fun provideCustomerDao(database: PdvDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideLoyaltyConfigDao(database: PdvDatabase): LoyaltyConfigDao = database.loyaltyConfigDao()

    @Provides
    fun provideLoyaltyTransactionDao(database: PdvDatabase): LoyaltyTransactionDao = database.loyaltyTransactionDao()

    @Provides
    fun provideTerminalConfigDao(database: PdvDatabase): TerminalConfigDao = database.terminalConfigDao()
}
