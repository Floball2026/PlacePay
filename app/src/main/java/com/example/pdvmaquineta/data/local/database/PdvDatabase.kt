package com.example.pdvmaquineta.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.example.pdvmaquineta.data.local.database.entity.AuditLogEntity
import com.example.pdvmaquineta.data.local.database.entity.CashMovementEntity
import com.example.pdvmaquineta.data.local.database.entity.CashSessionEntity
import com.example.pdvmaquineta.data.local.database.entity.CustomerEntity
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyConfigEntity
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyTransactionEntity
import com.example.pdvmaquineta.data.local.database.entity.PaymentEntity
import com.example.pdvmaquineta.data.local.database.entity.ProductEntity
import com.example.pdvmaquineta.data.local.database.entity.SaleEntity
import com.example.pdvmaquineta.data.local.database.entity.SaleItemEntity
import com.example.pdvmaquineta.data.local.database.entity.SaleOutboxEntity
import com.example.pdvmaquineta.data.local.database.entity.TerminalConfigEntity
import com.example.pdvmaquineta.data.local.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AuditLogEntity::class,
        CashSessionEntity::class,
        CashMovementEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SaleOutboxEntity::class,
        PaymentEntity::class,
        CustomerEntity::class,
        LoyaltyConfigEntity::class,
        LoyaltyTransactionEntity::class,
        TerminalConfigEntity::class
    ],
    version = 15,
    exportSchema = true
)
abstract class PdvDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun saleOutboxDao(): SaleOutboxDao
    abstract fun paymentDao(): PaymentDao
    abstract fun customerDao(): CustomerDao
    abstract fun loyaltyConfigDao(): LoyaltyConfigDao
    abstract fun loyaltyTransactionDao(): LoyaltyTransactionDao
    abstract fun terminalConfigDao(): TerminalConfigDao
}
