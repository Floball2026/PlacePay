package com.example.pdvmaquineta.data.sync.dto

import com.google.gson.annotations.SerializedName

// Contrato do backend SaaS (/v1/pos). Nomes em snake_case via @SerializedName.

data class DeviceInfoDto(
    @SerializedName("android_id") val androidId: String,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("android_version") val androidVersion: String?,
    @SerializedName("app_version") val appVersion: String?,
    @SerializedName("app_version_code") val appVersionCode: Int?
)

data class ActivateRequest(
    @SerializedName("activation_code") val activationCode: String,
    @SerializedName("device") val device: DeviceInfoDto
)

data class ActivateResponse(
    @SerializedName("token") val token: String,
    @SerializedName("company_id") val companyId: String?,
    @SerializedName("store_id") val storeId: String?,
    @SerializedName("terminal_id") val terminalId: String?,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("config_version") val configVersion: Int?
)

data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price_cents") val priceCents: Long,
    @SerializedName("category") val category: String?,
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("stock_quantity") val stockQuantity: Int,
    @SerializedName("min_stock_alert") val minStockAlert: Int?,
    @SerializedName("allow_sale_without_stock") val allowSaleWithoutStock: Boolean,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("updated_at") val updatedAt: String?
)

// ---- Envio de venda: POST /v1/pos/transactions ----

data class TransactionCustomerDto(
    @SerializedName("document") val document: String?,
    @SerializedName("name") val name: String?
)

data class TransactionTotalsDto(
    @SerializedName("gross_cents") val grossCents: Long?,
    @SerializedName("discount_cents") val discountCents: Long?,
    @SerializedName("loyalty_discount_cents") val loyaltyDiscountCents: Long?,
    @SerializedName("net_cents") val netCents: Long,
    @SerializedName("item_count") val itemCount: Int
)

data class TransactionItemDto(
    @SerializedName("product_ref_id") val productRefId: String?,
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price_cents") val unitPriceCents: Long,
    @SerializedName("total_cents") val totalCents: Long
)

data class TransactionPaymentDto(
    @SerializedName("method") val method: String,
    @SerializedName("amount_cents") val amountCents: Long,
    @SerializedName("received_cents") val receivedCents: Long?,
    @SerializedName("change_cents") val changeCents: Long?,
    @SerializedName("authorization_code") val authorizationCode: String?,
    @SerializedName("is_offline") val isOffline: Boolean?
)

data class PosTransactionInput(
    @SerializedName("transaction_uuid") val transactionUuid: String,
    @SerializedName("local_transaction_number") val localTransactionNumber: Long?,
    @SerializedName("operator_username") val operatorUsername: String?,
    @SerializedName("customer") val customer: TransactionCustomerDto?,
    @SerializedName("origin") val origin: String?,
    @SerializedName("app_version") val appVersion: String?,
    @SerializedName("schema_version") val schemaVersion: Int?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("completed_at") val completedAt: String,
    @SerializedName("totals") val totals: TransactionTotalsDto,
    @SerializedName("items") val items: List<TransactionItemDto>,
    @SerializedName("payments") val payments: List<TransactionPaymentDto>
)

data class PosTransactionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("transaction_uuid") val transactionUuid: String?,
    @SerializedName("server_transaction_id") val serverTransactionId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("received_at") val receivedAt: String?,
    @SerializedName("already_processed") val alreadyProcessed: Boolean?
)

// ---- Heartbeat: POST /v1/pos/heartbeat ----

data class PosHeartbeatInput(
    @SerializedName("device_time") val deviceTime: String?,
    @SerializedName("app_version") val appVersion: String?,
    @SerializedName("app_version_code") val appVersionCode: Int?,
    @SerializedName("battery") val battery: Int?,
    @SerializedName("storage_free_mb") val storageFreeMb: Int?,
    @SerializedName("network") val network: String?,
    @SerializedName("pending_transactions") val pendingTransactions: Int?,
    @SerializedName("last_sync_at") val lastSyncAt: String?,
    @SerializedName("operator_username") val operatorUsername: String?,
    @SerializedName("cash_status") val cashStatus: String?
)

data class PosHeartbeatCommand(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("payload") val payload: Map<String, Any?>?
)

data class PosHeartbeatResult(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("config_version") val configVersion: Int,
    @SerializedName("min_app_version_code") val minAppVersionCode: Int,
    @SerializedName("commands") val commands: List<PosHeartbeatCommand>?
)

// ---- Sync de operadores: GET /v1/pos/catalog/operators ----

data class OperatorDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("pin_hash") val pinHash: String?,
    @SerializedName("must_change_pin") val mustChangePin: Boolean?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("updated_at") val updatedAt: String?
)

// ---- Sync de clientes: GET /v1/pos/catalog/customers ----

data class CustomerDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("document") val document: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("updated_at") val updatedAt: String?
)

// ---- Config remota: GET /v1/pos/config , POST /v1/pos/config/ack ----

data class PosConfigResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("payload") val payload: com.google.gson.JsonElement?
)

data class ConfigAckRequest(
    @SerializedName("config_version") val configVersion: Int
)

// ---- Config remota: chaves do payload de /v1/pos/config ----

data class RemoteConfigDto(
    @SerializedName("enable_cashback") val enableCashback: Boolean?,
    @SerializedName("enable_promotions") val enablePromotions: Boolean?,
    @SerializedName("offline_mode") val offlineMode: Boolean?,
    @SerializedName("require_customer_cpf") val requireCustomerCpf: Boolean?,
    @SerializedName("sync_interval_seconds") val syncIntervalSeconds: Int?,
    @SerializedName("heartbeat_interval_seconds") val heartbeatIntervalSeconds: Int?,
    @SerializedName("max_discount_percent") val maxDiscountPercent: Int?
)
