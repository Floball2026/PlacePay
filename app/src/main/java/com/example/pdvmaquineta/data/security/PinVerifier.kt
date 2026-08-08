package com.example.pdvmaquineta.data.security

import at.favre.lib.crypto.bcrypt.BCrypt

// Verificacao de PIN com dois esquemas de hash:
//  - Operadores vindos do SaaS trazem hash BCRYPT (bcryptjs, prefixo $2a/$2b),
//    com o salt embutido no proprio hash (sem coluna de salt separada).
//  - Usuarios criados localmente usam PBKDF2 (PasswordHasher), com salt proprio.
// A deteccao e pelo prefixo "$2": o Base64 do PBKDF2 nunca contem "$", entao
// nao ha ambiguidade. Assim o login funciona offline para os dois casos sem
// migrar as credenciais existentes.
object PinVerifier {

    fun verify(pin: String, hash: String, salt: String?): Boolean {
        return if (hash.startsWith("\$2")) {
            verifyBcrypt(pin, hash)
        } else {
            if (salt == null) false else PasswordHasher.matches(pin, salt, hash)
        }
    }

    // True quando o hash e do esquema do servidor (bcrypt). Usado no sync para
    // decidir se o PIN e "gerenciado pelo servidor" ou foi trocado localmente.
    fun isServerManaged(hash: String?): Boolean = hash != null && hash.startsWith("\$2")

    // Normaliza a letra da versao para "a" ($2b/$2y/$2x -> $2a) antes de
    // verificar. Para PIN (ASCII, poucos digitos) o algoritmo e identico entre
    // as versoes; normalizar evita rejeicao caso a lib seja estrita quanto a
    // versao. Feito por substring (sem regex) pra nao esbarrar em referencia de
    // grupo ($N) na substituicao.
    private fun verifyBcrypt(pin: String, hash: String): Boolean {
        val normalized = if (hash.length > 3 && hash.startsWith("\$2")) {
            "\$2a" + hash.substring(3)
        } else {
            hash
        }
        return runCatching {
            BCrypt.verifyer().verify(pin.toCharArray(), normalized.toCharArray()).verified
        }.getOrDefault(false)
    }
}
