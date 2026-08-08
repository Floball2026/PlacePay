package com.example.pdvmaquineta.data.erp

import com.example.pdvmaquineta.domain.erp.ErpIntegrationGateway
import com.example.pdvmaquineta.domain.erp.ErpSyncResult
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val SYNC_DELAY_MS = 300L

// Sem ERP real contratado ainda (cliente final não definiu qual) — só
// simula sucesso, sem nenhuma chamada de rede. Quando o ERP for escolhido,
// essa classe é trocada por uma implementação real de
// ErpIntegrationGateway, sem tocar em domain/presentation. Mesmo padrão de
// MockPaymentGateway/MockReceiptPrinter/MockDigitalReceiptSender.
class MockErpIntegrationGateway @Inject constructor() : ErpIntegrationGateway {

    override suspend fun syncProducts(products: List<Product>): ErpSyncResult {
        delay(SYNC_DELAY_MS)
        return ErpSyncResult.Success(syncedCount = products.size)
    }

    override suspend fun sendSale(sale: Sale, items: List<SaleItem>, payment: Payment): ErpSyncResult {
        delay(SYNC_DELAY_MS)
        return ErpSyncResult.Success(
            syncedCount = 1,
            externalReference = "ERP-${UUID.randomUUID().toString().take(8)}"
        )
    }

    override suspend fun syncCustomers(customers: List<Customer>): ErpSyncResult {
        delay(SYNC_DELAY_MS)
        return ErpSyncResult.Success(syncedCount = customers.size)
    }
}
