package com.example.pdvmaquineta.data.sync

import com.example.pdvmaquineta.data.sync.dto.ActivateRequest
import com.example.pdvmaquineta.data.local.database.dao.UserDao
import com.example.pdvmaquineta.data.local.database.dao.CustomerDao
import com.example.pdvmaquineta.data.local.database.entity.CustomerEntity
import com.example.pdvmaquineta.data.local.database.entity.UserEntity
import com.example.pdvmaquineta.data.security.PasswordHasher
import java.util.UUID
import com.example.pdvmaquineta.data.local.database.dao.ProductDao
import com.example.pdvmaquineta.data.local.database.entity.ProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: PosApiService,
    private val settings: SyncSettings,
    private val deviceInfo: DeviceInfoProvider,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val customerDao: CustomerDao
) {
    // Ativa o terminal no SaaS: salva a URL base (para o interceptor usar já
    // nesta chamada), envia o código + dados do aparelho e guarda o token + ids.
    suspend fun activate(baseUrl: String, activationCode: String): Result<String> = runCatching {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        settings.baseUrl = cleanUrl
        val resp = api.activate(ActivateRequest(activationCode.trim(), deviceInfo.build()))
        settings.saveActivation(
            baseUrl = cleanUrl,
            token = resp.token,
            companyId = resp.companyId.orEmpty(),
            storeId = resp.storeId.orEmpty(),
            terminalId = resp.terminalId.orEmpty(),
            deviceId = resp.deviceId.orEmpty()
        )
        resp.terminalId.orEmpty()
    }

    // Carga PLU: baixa os produtos do SaaS e faz upsert local (por remoteId, com
    // fallback por codigo de barras). Guarda o maior updated_at para o proximo
    // sync incremental (?since=).
    suspend fun pullProducts(): Result<Int> = runCatching {
        if (!settings.isActivated) error("Terminal não ativado. Ative em Configurações antes de sincronizar.")
        val since = settings.productsSince.ifBlank { null }
        val list = api.getProducts(since)
        val now = System.currentTimeMillis()
        var maxUpdated = settings.productsSince
        for (dto in list) {
            val existing = productDao.findByRemoteId(dto.id)
                ?: dto.barcode?.let { productDao.findByBarcode(it) }
            if (existing != null) {
                productDao.update(
                    existing.copy(
                        remoteId = dto.id,
                        name = dto.name,
                        priceCents = dto.priceCents,
                        category = dto.category,
                        barcode = dto.barcode,
                        stockQuantity = dto.stockQuantity,
                        minStockAlert = dto.minStockAlert,
                        allowSaleWithoutStock = dto.allowSaleWithoutStock,
                        imageUrl = dto.imageUrl,
                        active = dto.active,
                        updatedAt = now
                    )
                )
            } else {
                productDao.insert(
                    ProductEntity(
                        name = dto.name,
                        priceCents = dto.priceCents,
                        category = dto.category,
                        active = dto.active,
                        stockQuantity = dto.stockQuantity,
                        minStockAlert = dto.minStockAlert,
                        allowSaleWithoutStock = dto.allowSaleWithoutStock,
                        barcode = dto.barcode,
                        imagePath = null,
                        remoteId = dto.id,
                        imageUrl = dto.imageUrl,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            val u = dto.updatedAt
            if (u != null && u > maxUpdated) maxUpdated = u
        }
        if (maxUpdated.isNotBlank()) settings.productsSince = maxUpdated
        list.size
    }

    // Sync de operadores: baixa os operadores do SaaS e faz upsert local (por
    // remoteId, com fallback por username). O PIN do servidor e bcrypt; o login
    // do app ja aceita bcrypt (ver PinVerifier). Regra importante: nao
    // sobrescreve um PIN que o operador trocou localmente (vira PBKDF2) — so
    // adota o PIN do servidor quando o local ainda e do servidor (bcrypt) ou
    // vazio. Assim uma troca de PIN no caixa nao e desfeita pela proxima carga.
    suspend fun pullOperators(): Result<Int> = runCatching {
        if (!settings.isActivated) error("Terminal não ativado. Ative em Configurações antes de sincronizar.")
        val since = settings.operatorsSince.ifBlank { null }
        val list = api.getOperators(since)
        val now = System.currentTimeMillis()
        var maxUpdated = settings.operatorsSince
        val validRoles = setOf("OPERATOR", "SUPERVISOR", "ADMIN")
        for (dto in list) {
            val role = dto.role?.uppercase()?.takeIf { it in validRoles } ?: "OPERATOR"
            val existing = userDao.findByRemoteId(dto.id) ?: userDao.findByUsername(dto.username)
            if (existing != null) {
                // Servidor e a fonte da verdade do PIN: o operador so aparece
                // aqui quando mudou no servidor (sync incremental por ?since=),
                // entao adota o PIN do servidor — assim o gerente reseta PIN de
                // operador pela web. dto.pinHash e bcrypt (sem salt separado).
                userDao.update(
                    existing.copy(
                        remoteId = dto.id,
                        displayName = dto.displayName ?: existing.displayName,
                        role = role,
                        active = dto.active,
                        pinHash = dto.pinHash ?: existing.pinHash,
                        pinSalt = if (dto.pinHash != null) null else existing.pinSalt,
                        mustChangePin = dto.mustChangePin ?: false,
                        updatedAt = now
                    )
                )
            } else {
                // passwordHash/passwordSalt sao NOT NULL mas o operador loga so
                // por PIN — senha aleatoria descartavel (mesmo padrao de createUser).
                val throwaway = PasswordHasher.hash(UUID.randomUUID().toString())
                userDao.insert(
                    UserEntity(
                        username = dto.username,
                        displayName = dto.displayName ?: dto.username,
                        passwordHash = throwaway.hash,
                        passwordSalt = throwaway.salt,
                        pinHash = dto.pinHash,
                        pinSalt = null,
                        role = role,
                        active = dto.active,
                        mustChangePin = dto.mustChangePin ?: false,
                        remoteId = dto.id,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            val u = dto.updatedAt
            if (u != null && u > maxUpdated) maxUpdated = u
        }
        if (maxUpdated.isNotBlank()) settings.operatorsSince = maxUpdated
        list.size
    }

    // Sync de clientes: baixa os clientes do SaaS e faz upsert local. Casa por
    // remoteId; fallback por documento (CPF, campo principal) e depois telefone.
    // O telefone agora e nao-unico, entao clientes sem telefone nao colidem.
    suspend fun pullCustomers(): Result<Int> = runCatching {
        if (!settings.isActivated) error("Terminal não ativado. Ative em Configurações antes de sincronizar.")
        val since = settings.customersSince.ifBlank { null }
        val list = api.getCustomers(since)
        val now = System.currentTimeMillis()
        var maxUpdated = settings.customersSince
        for (dto in list) {
            runCatching {
                val phone = dto.phone ?: ""
                val existing = customerDao.findByRemoteId(dto.id)
                    ?: dto.document?.takeIf { it.isNotBlank() }?.let { customerDao.findByDocument(it) }
                    ?: phone.takeIf { it.isNotBlank() }?.let { customerDao.findByPhone(it) }
                if (existing != null) {
                    customerDao.update(
                        existing.copy(
                            remoteId = dto.id,
                            name = dto.name,
                            phone = phone,
                            document = dto.document,
                            active = dto.active,
                            updatedAt = now
                        )
                    )
                } else {
                    customerDao.insert(
                        CustomerEntity(
                            name = dto.name,
                            phone = phone,
                            document = dto.document,
                            remoteId = dto.id,
                            active = dto.active,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }
            val u = dto.updatedAt
            if (u != null && u > maxUpdated) maxUpdated = u
        }
        if (maxUpdated.isNotBlank()) settings.customersSince = maxUpdated
        list.size
    }
}
