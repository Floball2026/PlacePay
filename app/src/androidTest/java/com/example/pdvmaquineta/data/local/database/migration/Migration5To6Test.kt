package com.example.pdvmaquineta.data.local.database.migration

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import com.example.pdvmaquineta.data.local.database.entity.CustomerEntity
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyConfigEntity
import com.example.pdvmaquineta.data.local.database.entity.LoyaltyTransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "migration-5-6-test.db"

// Mesma abordagem das migrações anteriores: recria à mão (SQL puro) o banco
// real da Fase 5a (schema completo de 8 tabelas, version=5, com um caixa
// aberto e uma venda já registrados) e abre com o PdvDatabase real (mesmo
// Room.databaseBuilder + MIGRATION_5_6 usados em produção), provando que a
// migração roda sem crash, preserva a venda existente (agora com
// customerId=null/loyaltyDiscountCents=0 por padrão), e que as tabelas novas
// de cliente/fidelidade ficam utilizáveis.
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    @Test
    fun migrate5To6_preservesExistingSaleAndAddsCustomerLoyaltyTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        val v5 = context.openOrCreateDatabase(TEST_DB_NAME, 0, null)
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                "`passwordSalt` TEXT NOT NULL, `pinHash` TEXT, `pinSalt` TEXT, `role` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `mustChangePin` INTEGER NOT NULL, `createdByUserId` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        v5.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `userId` INTEGER, `username` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `detail` TEXT, `success` INTEGER NOT NULL)"
        )
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`openingBalanceCents` INTEGER NOT NULL, `openedAt` INTEGER NOT NULL, " +
                "`closedAt` INTEGER, `expectedCashCents` INTEGER, `informedCashCents` INTEGER, " +
                "`divergenceCents` INTEGER, `status` TEXT NOT NULL)"
        )
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`authorizedByUsername` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v5.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_movements_cashSessionId` " +
                "ON `cash_movements` (`cashSessionId`)"
        )
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `category` TEXT, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active` ON `products` (`active`)")
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`discountPercent` INTEGER NOT NULL, `cancellationReason` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_cashSessionId` ON `sales` (`cashSessionId`)")
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")
        v5.execSQL(
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
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
        v5.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, `method` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`receivedCents` INTEGER, `changeCents` INTEGER, `status` TEXT NOT NULL, " +
                "`transactionId` TEXT, `declineReason` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v5.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_saleId` ON `payments` (`saleId`)")
        v5.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        v5.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fase-5a-placeholder')")

        v5.execSQL(
            "INSERT INTO cash_sessions (operatorId, operatorUsername, openingBalanceCents, openedAt, status) " +
                "VALUES (1, 'preexistente', 10000, 1000, 'OPEN')"
        )
        v5.execSQL(
            "INSERT INTO sales (cashSessionId, operatorId, operatorUsername, status, discountPercent, createdAt, updatedAt) " +
                "VALUES (1, 1, 'preexistente', 'AWAITING_PAYMENT', 0, 2000, 2000)"
        )
        v5.version = 5
        v5.close()

        val db = Room.databaseBuilder(context, PdvDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

        runBlocking {
            val sale = db.saleDao().findById(1)
            assertNotNull("Venda pré-existente deveria continuar lá depois da migração", sale)
            assertEquals("preexistente", sale?.operatorUsername)
            assertEquals(null, sale?.customerId)
            assertEquals(0L, sale?.loyaltyDiscountCents)

            val customerId = db.customerDao().insert(
                CustomerEntity(
                    name = "Cliente Teste",
                    phone = "11999990000",
                    document = null,
                    createdAt = 3000,
                    updatedAt = 3000
                )
            )
            assertTrue("Cliente novo deveria ser inserível após a migração", customerId > 0)

            val configId = db.loyaltyConfigDao().insert(
                LoyaltyConfigEntity(
                    mode = "POINTS_PER_VALUE",
                    pointsPerCurrencyUnit = 1.0,
                    pointValueInCents = 5,
                    visitsRequired = null,
                    discountPercentOnReward = null,
                    activatedAt = 3000,
                    deactivatedAt = null,
                    changedByUserId = null
                )
            )
            assertTrue("Config de fidelidade nova deveria ser inserível", configId > 0)

            db.loyaltyTransactionDao().insert(
                LoyaltyTransactionEntity(
                    customerId = customerId,
                    saleId = 1,
                    mode = "POINTS_PER_VALUE",
                    type = "EARNED",
                    points = 50,
                    amountCents = null,
                    timestamp = 3000
                )
            )
            val transactions = db.loyaltyTransactionDao().findForCustomerSince(customerId, 0)
            assertEquals(1, transactions.size)
            assertEquals(50, transactions.first().points)
        }

        db.close()
    }
}
