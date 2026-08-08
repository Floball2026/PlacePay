package com.example.pdvmaquineta.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// PBKDF2 com salt aleatório por usuário; iterações no patamar recomendado
// pela OWASP (2023) para HMAC-SHA256. Sem libs externas, só javax.crypto.
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    data class HashedPassword(val hash: String, val salt: String)

    fun hash(password: String): HashedPassword {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = deriveKey(password, salt)
        return HashedPassword(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun matches(password: String, salt: String, expectedHash: String): Boolean {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val expectedBytes = Base64.decode(expectedHash, Base64.NO_WRAP)
        val actualBytes = deriveKey(password, saltBytes)
        // Comparação em tempo constante para evitar timing attack.
        return MessageDigest.isEqual(actualBytes, expectedBytes)
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
