package com.example.pdvmaquineta.data.local.database.migration

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "migration-2-3-test.db"

// Mesma abordagem do Migration1To2Test: recria à mão (SQL puro) o banco real
// da Fase 2 (users/audit_log/cash_sessions/cash_movements, version=2) e abre
// com o PdvDatabase real (mesmo Room.databaseBuilder + MIGRATION_2_3 usados em
// produção), provando que a migração roda sem crash e preserva o caixa aberto
// existente enquanto cria as tabelas novas de produto/venda.
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    @Test
    fun migrate2To3_preservesExistingDataAndAddsSaleTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        // 1) Recria o schema real da Fase 2 (version=2).
        val v2 = context.openOrCreateDatabase(TEST_DB_NAME, 0, null)
        v2.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                "`passwordSalt` TEXT NOT NULL, `pinHash` TEXT, `role` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        v2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        v2.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `userId` INTEGER, `username` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `detail` TEXT, `success` INTEGER NOT NULL)"
        )
        v2.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`openingBalanceCents` INTEGER NOT NULL, `openedAt` INTEGER NOT NULL, " +
                "`closedAt` INTEGER, `expectedCashCents` INTEGER, `informedCashCents` INTEGER, " +
                "`divergenceCents` INTEGER, `status` TEXT NOT NULL)"
        )
        v2.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, `operatorUsername` TEXT NOT NULL, " +
                "`authorizedByUsername` TEXT, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        v2.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_movements_cashSessionId` " +
                "ON `cash_movements` (`cashSessionId`)"
        )
        v2.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        v2.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fase-2-placeholder')")
        v2.execSQL(
            "INSERT INTO cash_sessions (operatorId, operatorUsername, openingBalanceCents, openedAt, status) " +
                "VALUES (1, 'preexistente', 10000, 1000, 'OPEN')"
        )
        v2.version = 2
        v2.close()

        // 2) Abre com o PdvDatabase real (mesma configuração de DatabaseModule.kt em produção).
        val db = Room.databaseBuilder(context, PdvDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

        runBlocking {
            // Não crashou ao abrir e o caixa aberto da Fase 2 sobreviveu.
            val openSession = db.cashSessionDao().observeOpenSession().first()
            assertNotNull("Caixa aberto antes da migração deveria continuar existindo", openSession)
            assertEquals("preexistente", openSession?.operatorUsername)

            // As tabelas novas da Fase 3 existem e funcionam.
            val productId = db.productDao().insert(
                com.example.pdvmaquineta.data.local.database.entity.ProductEntity(
                    name = "Produto Teste",
                    priceCents = 500,
                    category = null,
                    createdAt = 0,
                    updatedAt = 0
                )
            )
            assertTrue("Produto novo deveria ser inserível após a migração", productId > 0)
        }

        db.close()
    }
}
