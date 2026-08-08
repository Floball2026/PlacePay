package com.example.pdvmaquineta.domain.erp

import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem

sealed class ErpSyncResult {
    data class Success(val syncedCount: Int, val externalReference: String? = null) : ErpSyncResult()
    data class Failure(val reason: String) : ErpSyncResult()
}

// RF-042: sincronização com ERP (produtos/preços/estoque, vendas concluídas,
// clientes). Hoje não existe ERP real definido pelo cliente final — esta
// interface só prepara o terreno pra plugar a integração real depois, sem
// reescrever domínio/apresentação, mesmo padrão de PaymentGateway/
// ReceiptPrinter/DigitalReceiptSender. O PDV é a fonte primária desses dados
// hoje (nenhum ERP alimenta o PDV ainda), então toda sincronização aqui é de
// envio (PDV -> ERP), não recebimento.
interface ErpIntegrationGateway {
    // Produto já carrega priceCents/stockQuantity — um único envio cobre
    // produtos, preços e estoque (RF-042), sem precisar de três métodos.
    suspend fun syncProducts(products: List<Product>): ErpSyncResult

    suspend fun sendSale(sale: Sale, items: List<SaleItem>, payment: Payment): ErpSyncResult

    suspend fun syncCustomers(customers: List<Customer>): ErpSyncResult
}
