package com.example.pdvmaquineta.data.local.database.migration

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.data.local.database.PdvDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "migration-1-2-test.db"

// Não usa MigrationTestHelper (bloqueado nesse projeto por um conflito de
// versão do kotlinx-serialization forçado pelo built-in Kotlin do AGP, entre
// o room-testing e o restante do classpath). Em vez disso, recria à mão -com
// SQL puro- exatamente o banco que existia na Fase 1 (só users/audit_log,
// version=1) e então abre o PdvDatabase real (mesmo Room.databaseBuilder +
// MIGRATION_1_2 usados em produção) para provar que a migração roda sem
// crashar e sem apagar dado nenhum.
@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @Test
    fun migrate1To2_preservesExistingDataAndAddsCashTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        // 1) Recria o schema real da Fase 1 (version=1: só users + audit_log).
        val v1 = context.openOrCreateDatabase(TEST_DB_NAME, 0, null)
        v1.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                "`passwordSalt` TEXT NOT NULL, `pinHash` TEXT, `role` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        v1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        v1.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `userId` INTEGER, `username` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `detail` TEXT, `success` INTEGER NOT NULL)"
        )
        v1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        v1.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fase-1-placeholder')")
        v1.execSQL(
            "INSERT INTO users (username, displayName, passwordHash, passwordSalt, role, active, createdAt) " +
                "VALUES ('preexistente', 'Usuario Pre-Migracao', 'hash-fake', 'salt-fake', 'OPERATOR', 1, 1000)"
        )
        v1.version = 1
        v1.close()

        // 2) Abre com o PdvDatabase real (mesma configuração de DatabaseModule.kt em produção).
        val db = Room.databaseBuilder(context, PdvDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

        runBlocking {
            // Não crashou ao abrir (checkIdentity passou) e o usuário anterior sobreviveu.
            val preexisting = db.userDao().findByUsername("preexistente")
            assertNotNull("Usuário criado antes da migração deveria continuar existindo", preexisting)
            assertEquals("Usuario Pre-Migracao", preexisting?.displayName)

            // As tabelas novas da Fase 2 existem e funcionam.
            val openSession = db.cashSessionDao().observeOpenSession().first()
            assertNull("Não deveria haver caixa aberto vindo da migração", openSession)
        }

        db.close()
    }
}
