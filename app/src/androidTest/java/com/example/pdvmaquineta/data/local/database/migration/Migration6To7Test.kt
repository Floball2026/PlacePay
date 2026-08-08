package com.example.pdvmaquineta.data.local.database.migration

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "migration-6-7-test.db"

// Mesma abordagem das migrações anteriores: recria à mão (SQL puro) o banco
// real da Fase 5 (schema completo de 11 tabelas, version=6, com um usuário
// admin já cadastrado) e abre com o PdvDatabase real (mesmo
// Room.databaseBuilder + MIGRATION_6_7 usados em produção), provando que a
// migração roda sem crash, preserva o usuário existente com themeTone
// preenchido no valor padrão ('NAVY_DARK'), e que a coluna nova é utilizável
// (aceita update pra outro tom).
@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    @Test
    fun migrate6To7_preservesExistingUserWithDefaultThemeToneAndAllowsUpdate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        val v6 = context.openOrCreateDatabase(TEST_DB_NAME, 0, null)
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                "`passwordSalt` TEXT NOT NULL, `pinHash` TEXT, `pinSalt` TEXT, `role` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `mustChangePin` INTEGER NOT NULL, `createdByUserId` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        v6.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `userId` INTEGER, `username` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `detail` TEXT, `success` INTEGER NOT NULL)"
        )
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`openingBalanceCents` INTEGER NOT NULL, `openedAt` INTEGER NOT NULL, " +
                "`closedAt` INTEGER, `expectedCashCents` INTEGER, `informedCashCents` INTEGER, " +
                "`divergenceCents` INTEGER, `status` TEXT NOT NULL)"
        )
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`authorizedByUsername` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v6.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_movements_cashSessionId` " +
                "ON `cash_movements` (`cashSessionId`)"
        )
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `category` TEXT, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active` ON `products` (`active`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, `status` TEXT NOT NULL, `customerId` INTEGER, " +
                "`discountPercent` INTEGER NOT NULL, `loyaltyDiscountCents` INTEGER NOT NULL, " +
                "`cancellationReason` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_cashSessionId` ON `sales` (`cashSessionId`)")
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                "`productName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, " +
                "`quantity` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, `method` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`receivedCents` INTEGER, `changeCents` INTEGER, `status` TEXT NOT NULL, " +
                "`transactionId` TEXT, `declineReason` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v6.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_saleId` ON `payments` (`saleId`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, `document` TEXT, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        v6.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_configs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`mode` TEXT NOT NULL, `pointsPerCurrencyUnit` REAL, `pointValueInCents` INTEGER, " +
                "`visitsRequired` INTEGER, `discountPercentOnReward` INTEGER, " +
                "`activatedAt` INTEGER NOT NULL, `deactivatedAt` INTEGER, `changedByUserId` INTEGER)"
        )
        v6.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_transactions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`customerId` INTEGER NOT NULL, `saleId` INTEGER, `mode` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, `points` INTEGER, `amountCents` INTEGER, " +
                "`timestamp` INTEGER NOT NULL, " +
                "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v6.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_customerId` " +
                "ON `loyalty_transactions` (`customerId`)"
        )
        v6.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_saleId` " +
                "ON `loyalty_transactions` (`saleId`)"
        )
        v6.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        v6.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fase-5-placeholder')")

        v6.execSQL(
            "INSERT INTO users (username, displayName, passwordHash, passwordSalt, role, active, " +
                "mustChangePin, createdAt, updatedAt) " +
                "VALUES ('admin.preexistente', 'Admin Preexistente', 'hash', 'salt', 'ADMIN', 1, 0, 5000, 5000)"
        )
        v6.version = 6
        v6.close()

        val db = Room.databaseBuilder(context, PdvDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

        runBlocking {
            val preexisting = db.userDao().findByUsername("admin.preexistente")
            assertNotNull("Usuário pré-existente deveria continuar lá depois da migração", preexisting)
            assertEquals("Admin Preexistente", preexisting?.displayName)
            assertEquals("NAVY_DARK", preexisting?.themeTone)

            val updated = preexisting!!.copy(themeTone = "NAVY_LIGHT", updatedAt = 9000)
            db.userDao().update(updated)

            val reloaded = db.userDao().findByUsername("admin.preexistente")
            assertEquals("NAVY_LIGHT", reloaded?.themeTone)
        }

        db.close()
    }
}
