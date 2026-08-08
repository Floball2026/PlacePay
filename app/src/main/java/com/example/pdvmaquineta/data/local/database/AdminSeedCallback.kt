package com.example.pdvmaquineta.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pdvmaquineta.data.security.PasswordHasher
import java.util.UUID

private const val SEED_ADMIN_USERNAME = "admin"
private const val SEED_ADMIN_DISPLAY_NAME = "Administrador"
private const val SEED_ADMIN_TEMP_PIN = "000000"

// Roda em onOpen (não só onCreate) — cobre tanto instalação nova (banco
// criado direto na versão atual) quanto upgrade de um banco já existente
// (depois das migrações), porque Room chama onOpen nos dois casos. Garante
// que sempre existe pelo menos um admin ativo pra acessar a tela de
// Usuários; sem isso, remover os botões de debug pode deixar o app sem
// nenhum caminho de acesso administrativo. username/PIN fixos e conhecidos,
// com mustChangePin=true pra forçar trocar o PIN no primeiro uso.
// createdByUserId fica null de propósito, deixando explícito que foi o
// sistema que criou, não um humano.
class AdminSeedCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        if (countActiveAdmins(db) > 0) return

        val now = System.currentTimeMillis()
        val pin = PasswordHasher.hash(SEED_ADMIN_TEMP_PIN)
        val existingId = findUserIdByUsername(db, SEED_ADMIN_USERNAME)

        if (existingId != null) {
            // Já existe um 'admin' (provavelmente desativado em algum
            // momento) e não há nenhum outro admin ativo — restaura o acesso
            // em vez de inserir de novo (duplicar quebraria o índice único
            // de username).
            db.execSQL(
                "UPDATE users SET role = 'ADMIN', active = 1, mustChangePin = 1, " +
                    "pinHash = ?, pinSalt = ?, updatedAt = ? WHERE id = ?",
                arrayOf<Any>(pin.hash, pin.salt, now, existingId)
            )
        } else {
            val password = PasswordHasher.hash(UUID.randomUUID().toString())
            db.execSQL(
                "INSERT INTO users (username, displayName, passwordHash, passwordSalt, " +
                    "pinHash, pinSalt, role, active, mustChangePin, createdByUserId, themeTone, " +
                    "createdAt, updatedAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'ADMIN', 1, 1, NULL, 'BRAND_LIGHT', ?, ?)",
                arrayOf<Any>(
                    SEED_ADMIN_USERNAME, SEED_ADMIN_DISPLAY_NAME,
                    password.hash, password.salt, pin.hash, pin.salt,
                    now, now
                )
            )
        }

        db.execSQL(
            "INSERT INTO audit_log (timestamp, userId, username, action, detail, success) " +
                "VALUES (?, NULL, 'system', 'USER_CREATED', ?, 1)",
            arrayOf<Any>(
                now,
                "Admin '$SEED_ADMIN_USERNAME' criado/restaurado automaticamente: nenhum admin ativo no banco"
            )
        )
    }

    private fun countActiveAdmins(db: SupportSQLiteDatabase): Int {
        db.query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = 1").use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun findUserIdByUsername(db: SupportSQLiteDatabase, username: String): Long? {
        db.query("SELECT id FROM users WHERE username = ? LIMIT 1", arrayOf(username)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }
}
