package com.example.pdvmaquineta.data.local.database.migration

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "migration-9-10-test.db"

// Mesma abordagem das migrações anteriores: recria à mão (SQL puro) o banco
// real da Fase 7a completa (schema completo de 12 tabelas, version=9, com um
// produto já cadastrado) e abre com o PdvDatabase real (mesmo
// Room.databaseBuilder + MIGRATION_9_10 usados em produção), provando que a
// migração roda sem crash, preserva o produto existente com barcode nulo
// (RF-009 é opcional, sem valor retroativo), e que o índice único criado
// permite múltiplos produtos sem código mas barra duplicidade entre os que
// têm (RF-009).
@RunWith(AndroidJUnit4::class)
class Migration9To10Test {

    @Test
    fun migrate9To10_preservesExistingProductWithNullBarcodeAndEnforcesUniqueIndex() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        val v9 = context.openOrCreateDatabase(TEST_DB_NAME, 0, null)
        createV9Schema(v9)
        v9.execSQL(
            "INSERT INTO products (name, priceCents, category, active, stockQuantity, " +
                "minStockAlert, allowSaleWithoutStock, createdAt, updatedAt) " +
                "VALUES ('Café', 500, 'Bebidas', 1, 10, NULL, 0, 3000, 3000)"
        )
        v9.version = 9
        v9.close()

        val db = Room.databaseBuilder(context, PdvDatabase::class.java, TEST_DB_NAME)
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
            )
            .build()

        runBlocking {
            val preexisting = db.productDao().findById(1)
            assertNotNull("Produto pré-existente deveria continuar lá depois da migração", preexisting)
            assertEquals("Café", preexisting?.name)
            assertNull("Sem código de barras retroativo", preexisting?.barcode)

            val secondId = db.productDao().insert(
                preexisting!!.copy(id = 0, name = "Água", barcode = "7891234567890")
            )
            assertNotNull(db.productDao().findById(secondId))
            assertEquals("Água", db.productDao().findByBarcode("7891234567890")?.name)

            val thirdId = db.productDao().insert(
                preexisting.copy(id = 0, name = "Suco", barcode = null)
            )
            assertNotNull(
                "Dois produtos sem código de barras devem coexistir (NULL não conflita em índice único)",
                db.productDao().findById(thirdId)
            )

            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    db.productDao().insert(
                        preexisting.copy(id = 0, name = "Duplicado", barcode = "7891234567890")
                    )
                }
            }
        }

        db.close()
    }

    private fun createV9Schema(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                "`passwordSalt` TEXT NOT NULL, `pinHash` TEXT, `pinSalt` TEXT, `role` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `mustChangePin` INTEGER NOT NULL, `createdByUserId` INTEGER, " +
                "`themeTone` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `userId` INTEGER, `username` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `detail` TEXT, `success` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`openingBalanceCents` INTEGER NOT NULL, `openedAt` INTEGER NOT NULL, " +
                "`closedAt` INTEGER, `expectedCashCents` INTEGER, `informedCashCents` INTEGER, " +
                "`divergenceCents` INTEGER, `status` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`authorizedByUsername` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_movements_cashSessionId` " +
                "ON `cash_movements` (`cashSessionId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `category` TEXT, " +
                "`active` INTEGER NOT NULL, `stockQuantity` INTEGER NOT NULL, " +
                "`minStockAlert` INTEGER, `allowSaleWithoutStock` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active` ON `products` (`active`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, `status` TEXT NOT NULL, `customerId` INTEGER, " +
                "`discountPercent` INTEGER NOT NULL, `loyaltyDiscountCents` INTEGER NOT NULL, " +
                "`cancellationReason` TEXT, `terminalNameSnapshot` TEXT, `storeNameSnapshot` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_cashSessionId` ON `sales` (`cashSessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")
        db.execSQL(
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, `method` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`receivedCents` INTEGER, `changeCents` INTEGER, `status` TEXT NOT NULL, " +
                "`transactionId` TEXT, `declineReason` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_saleId` ON `payments` (`saleId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, `document` TEXT, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_configs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`mode` TEXT NOT NULL, `pointsPerCurrencyUnit` REAL, `pointValueInCents` INTEGER, " +
                "`visitsRequired` INTEGER, `discountPercentOnReward` INTEGER, " +
                "`activatedAt` INTEGER NOT NULL, `deactivatedAt` INTEGER, `changedByUserId` INTEGER)"
        )
        db.execSQL(
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
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_customerId` " +
                "ON `loyalty_transactions` (`customerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_saleId` " +
                "ON `loyalty_transactions` (`saleId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `terminal_config` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`terminalName` TEXT NOT NULL, `storeName` TEXT NOT NULL, `environment` TEXT NOT NULL, " +
                "`printerType` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `updatedByUserId` INTEGER)"
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fase-7a-placeholder')")
    }
}
