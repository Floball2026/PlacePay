package com.example.pdvmaquineta.presentation.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.loyalty.LoyaltyRedeemable
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.Payment
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.receipt.ReceiptChannel
import com.example.pdvmaquineta.domain.receipt.ReceiptPrintResult
import com.example.pdvmaquineta.domain.receipt.ReceiptSendResult
import com.example.pdvmaquineta.domain.usecase.AddProductToCartUseCase
import com.example.pdvmaquineta.domain.usecase.ApplyDiscountResult
import com.example.pdvmaquineta.domain.usecase.ApplyDiscountUseCase
import com.example.pdvmaquineta.domain.usecase.AuthorizeWithSupervisorUseCase
import com.example.pdvmaquineta.domain.usecase.CancelSaleResult
import com.example.pdvmaquineta.domain.usecase.CancelSaleUseCase
import com.example.pdvmaquineta.domain.usecase.CartOverview
import com.example.pdvmaquineta.domain.usecase.ChangeCartItemQuantityUseCase
import com.example.pdvmaquineta.domain.usecase.FindOrCreateCustomerUseCase
import com.example.pdvmaquineta.domain.usecase.FindProductByBarcodeUseCase
import com.example.pdvmaquineta.domain.usecase.FinalizeSaleUseCase
import com.example.pdvmaquineta.domain.usecase.GetCustomerUseCase
import com.example.pdvmaquineta.domain.usecase.GetLoyaltyStatusUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveActiveProductsUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveCartUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveOpenCashSessionUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveSuspendedSalesUseCase
import com.example.pdvmaquineta.domain.usecase.PrintReceiptUseCase
import com.example.pdvmaquineta.domain.usecase.ProcessPaymentResult
import com.example.pdvmaquineta.domain.usecase.ProcessPaymentUseCase
import com.example.pdvmaquineta.domain.usecase.RedeemLoyaltyResult
import com.example.pdvmaquineta.domain.usecase.RedeemLoyaltyUseCase
import com.example.pdvmaquineta.domain.usecase.RemoveCartItemUseCase
import com.example.pdvmaquineta.domain.usecase.CorrectItemPriceUseCase
import com.example.pdvmaquineta.domain.usecase.RemoveCustomerFromSaleUseCase
import com.example.pdvmaquineta.domain.usecase.ReopenSaleUseCase
import com.example.pdvmaquineta.domain.usecase.ResumeSaleResult
import com.example.pdvmaquineta.domain.usecase.ResumeSaleUseCase
import com.example.pdvmaquineta.domain.usecase.SearchCustomersUseCase
import com.example.pdvmaquineta.domain.usecase.SelectCustomerForSaleUseCase
import com.example.pdvmaquineta.domain.usecase.SendReceiptDigitallyUseCase
import com.example.pdvmaquineta.domain.usecase.SupervisorAuthorization
import com.example.pdvmaquineta.domain.usecase.SupervisorAuthorizationResult
import com.example.pdvmaquineta.domain.usecase.SuspendSaleUseCase
import com.example.pdvmaquineta.domain.usecase.UndoLoyaltyRedemptionResult
import com.example.pdvmaquineta.domain.usecase.UndoLoyaltyRedemptionUseCase
import com.example.pdvmaquineta.domain.usecase.ValidateCartStockResult
import com.example.pdvmaquineta.domain.usecase.ValidateCartStockUseCase
import com.example.pdvmaquineta.data.sync.BusinessConfigStore
import com.example.pdvmaquineta.data.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SaleRoute {
    data object Cart : SaleRoute()
    data object PaymentMethod : SaleRoute()
    data object Processing : SaleRoute()
    data object Receipt : SaleRoute()
}

data class DiscountUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authorizationRequired: Boolean = false,
    val authorization: SupervisorAuthorization? = null
)

data class CancelSaleUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class PaymentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CustomerPickerUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RedeemUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
data class CargaPluUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SaleViewModel @Inject constructor(
    observeOpenCashSessionUseCase: ObserveOpenCashSessionUseCase,
    private val observeCartUseCase: ObserveCartUseCase,
    private val observeSuspendedSalesUseCase: ObserveSuspendedSalesUseCase,
    private val observeActiveProductsUseCase: ObserveActiveProductsUseCase,
    private val addProductToCartUseCase: AddProductToCartUseCase,
    private val findProductByBarcodeUseCase: FindProductByBarcodeUseCase,
    private val changeCartItemQuantityUseCase: ChangeCartItemQuantityUseCase,
    private val removeCartItemUseCase: RemoveCartItemUseCase,
    private val correctItemPriceUseCase: CorrectItemPriceUseCase,
    private val applyDiscountUseCase: ApplyDiscountUseCase,
    private val suspendSaleUseCase: SuspendSaleUseCase,
    private val resumeSaleUseCase: ResumeSaleUseCase,
    private val cancelSaleUseCase: CancelSaleUseCase,
    private val finalizeSaleUseCase: FinalizeSaleUseCase,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val reopenSaleUseCase: ReopenSaleUseCase,
    private val authorizeWithSupervisorUseCase: AuthorizeWithSupervisorUseCase,
    private val searchCustomersUseCase: SearchCustomersUseCase,
    private val findOrCreateCustomerUseCase: FindOrCreateCustomerUseCase,
    private val selectCustomerForSaleUseCase: SelectCustomerForSaleUseCase,
    private val removeCustomerFromSaleUseCase: RemoveCustomerFromSaleUseCase,
    private val getCustomerUseCase: GetCustomerUseCase,
    private val getLoyaltyStatusUseCase: GetLoyaltyStatusUseCase,
    private val redeemLoyaltyUseCase: RedeemLoyaltyUseCase,
    private val undoLoyaltyRedemptionUseCase: UndoLoyaltyRedemptionUseCase,
    private val printReceiptUseCase: PrintReceiptUseCase,
    private val sendReceiptDigitallyUseCase: SendReceiptDigitallyUseCase,
    private val validateCartStockUseCase: ValidateCartStockUseCase,
    private val syncRepository: SyncRepository,
    private val remoteConfig: BusinessConfigStore
) : ViewModel() {

    var cargaPluState by mutableStateOf(CargaPluUiState())
        private set

    fun cargaPlu() {
        viewModelScope.launch {
            cargaPluState = CargaPluUiState(isLoading = true)
            val products = syncRepository.pullProducts()
            val operators = syncRepository.pullOperators()
            val customers = syncRepository.pullCustomers()
            val prodErr = products.exceptionOrNull()
            if (prodErr != null) {
                cargaPluState = CargaPluUiState(error = prodErr.message ?: "Falha na carga")
            } else {
                val p = products.getOrDefault(0)
                val o = operators.getOrDefault(0)
                val c = customers.getOrDefault(0)
                val base = "$p produto(s), $o operador(es) e $c cliente(s) sincronizado(s)"
                val failures = buildList {
                    if (operators.isFailure) add("operadores")
                    if (customers.isFailure) add("clientes")
                }
                cargaPluState = if (failures.isEmpty()) {
                    CargaPluUiState(message = base)
                } else {
                    CargaPluUiState(message = "$base. Falha em: ${failures.joinToString()}")
                }
            }
        }
    }

    private val cashSessionId: StateFlow<Long?> = observeOpenCashSessionUseCase()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val cart: StateFlow<CartOverview?> = cashSessionId
        .flatMapLatest { id -> if (id != null) observeCartUseCase(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val suspendedSales: StateFlow<List<Sale>> = cashSessionId
        .flatMapLatest { id -> if (id != null) observeSuspendedSalesUseCase(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val searchQuery = MutableStateFlow("")

    val products: StateFlow<List<Product>> = searchQuery
        .flatMapLatest { query -> observeActiveProductsUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val customerSearchQuery = MutableStateFlow("")

    val customerSearchResults: StateFlow<List<Customer>> = customerSearchQuery
        .flatMapLatest { query -> if (query.isBlank()) flowOf(emptyList()) else searchCustomersUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var route by mutableStateOf<SaleRoute>(SaleRoute.Cart)
        private set

    var discountUiState by mutableStateOf(DiscountUiState())
        private set

    var cancelUiState by mutableStateOf(CancelSaleUiState())
        private set

    var resumeErrorMessage by mutableStateOf<String?>(null)
        private set

    // Snapshot congelado no instante da finalização: a partir daí a venda sai
    // de OPEN e o `cart` reativo para de enxergá-la, mas o total/itens não
    // mudam mais durante o fluxo de pagamento, então não precisa continuar
    // reativo.
    var pendingSale by mutableStateOf<CartOverview?>(null)
        private set

    var selectedPaymentMethod by mutableStateOf<PaymentMethod?>(null)
        private set

    var paymentUiState by mutableStateOf(PaymentUiState())
        private set

    var lastPayment by mutableStateOf<Payment?>(null)
        private set

    var customerPickerVisible by mutableStateOf(false)
        private set

    // Lembrete (nao obrigatorio) de CPF antes do pagamento
    var cpfReminderVisible by mutableStateOf(false)
        private set

    var customerPickerUiState by mutableStateOf(CustomerPickerUiState())
        private set

    var selectedCustomer by mutableStateOf<Customer?>(null)
        private set

    var loyaltyStatus by mutableStateOf<LoyaltyRedeemable?>(null)
        private set

    var redeemUiState by mutableStateOf(RedeemUiState())
        private set

    var undoRedeemUiState by mutableStateOf(RedeemUiState())
        private set

    var printUiState by mutableStateOf(ReceiptActionUiState())
        private set

    var sendUiState by mutableStateOf(ReceiptActionUiState())
        private set

    var stockErrorMessage by mutableStateOf<String?>(null)
        private set

    // Retorno da busca por código de barras (Enter do leitor físico ou
    // câmera) quando nenhum produto ativo corresponde ao código exato.
    var scanErrorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Reage a mudanças no carrinho (cliente selecionado, item adicionado)
        // pra manter cliente/status de fidelidade sempre atualizados sem
        // precisar de uma cadeia de flatMapLatest aninhada.
        viewModelScope.launch {
            cart.collect { overview ->
                val customerId = overview?.sale?.customerId
                if (customerId == null) {
                    selectedCustomer = null
                    loyaltyStatus = null
                } else {
                    if (selectedCustomer?.id != customerId) {
                        selectedCustomer = getCustomerUseCase(customerId)
                    }
                    loyaltyStatus = getLoyaltyStatusUseCase(customerId, overview.netBeforeLoyaltyCents)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun addToCart(product: Product) {
        val sessionId = cashSessionId.value ?: return
        viewModelScope.launch { addProductToCartUseCase(sessionId, product) }
    }

    // Busca exata por código de barras — usada tanto pelo Enter do leitor
    // físico USB/Bluetooth (que age como teclado) quanto pelo retorno da
    // câmera (RF-010/011). Ao encontrar produto ativo, adiciona direto ao
    // carrinho sem exigir toque adicional do operador.
    fun submitBarcode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val locked = (cart.value?.loyaltyDiscountCents ?: 0) > 0
            if (locked) {
                scanErrorMessage = "Carrinho travado enquanto o resgate estiver aplicado"
                return@launch
            }
            val product = findProductByBarcodeUseCase(trimmed)
            if (product != null && product.active) {
                addToCart(product)
                scanErrorMessage = null
                searchQuery.value = ""
            } else {
                scanErrorMessage = "Nenhum produto encontrado para o código \"$trimmed\""
            }
        }
    }

    fun clearScanError() {
        scanErrorMessage = null
    }

    fun incrementItem(productId: Long) {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { changeCartItemQuantityUseCase(saleId, productId, 1) }
    }

    fun decrementItem(productId: Long) {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { changeCartItemQuantityUseCase(saleId, productId, -1) }
    }

    fun removeItem(productId: Long) {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { removeCartItemUseCase(saleId, productId) }
    }

    fun correctItemPrice(productId: Long, newPriceCents: Long) {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { correctItemPriceUseCase(saleId, productId, newPriceCents) }
    }

    fun applyDiscount(percent: Int) {
        val sale = cart.value?.sale ?: return
        // Teto de desconto vem da config remota (max_discount_percent).
        val maxPercent = remoteConfig.get().maxDiscountPercent
        if (percent > maxPercent) {
            discountUiState = discountUiState.copy(
                isLoading = false,
                errorMessage = "Desconto máximo permitido: $maxPercent%"
            )
            return
        }
        viewModelScope.launch {
            discountUiState = discountUiState.copy(isLoading = true, errorMessage = null)
            when (applyDiscountUseCase(sale, percent, discountUiState.authorization)) {
                ApplyDiscountResult.Success -> discountUiState = DiscountUiState()
                ApplyDiscountResult.AuthorizationRequired ->
                    discountUiState = discountUiState.copy(isLoading = false, authorizationRequired = true)
                ApplyDiscountResult.InvalidPercent ->
                    discountUiState = discountUiState.copy(isLoading = false, errorMessage = "Percentual inválido")
            }
        }
    }

    fun authorizeDiscount(username: String, password: String) {
        viewModelScope.launch {
            when (val result = authorizeWithSupervisorUseCase(Permission.AUTHORIZE_DISCOUNT, username, password)) {
                is SupervisorAuthorizationResult.Authorized ->
                    discountUiState = discountUiState.copy(
                        authorizationRequired = false,
                        authorization = result.authorization,
                        errorMessage = null
                    )
                SupervisorAuthorizationResult.InvalidCredentials ->
                    discountUiState = discountUiState.copy(errorMessage = "Credenciais de supervisor inválidas")
                SupervisorAuthorizationResult.InsufficientPermission ->
                    discountUiState = discountUiState.copy(errorMessage = "Esse usuário não tem permissão para autorizar")
            }
        }
    }

    fun dismissDiscountAuthorization() {
        discountUiState = discountUiState.copy(authorizationRequired = false, errorMessage = null)
    }

    fun resetDiscountPanel() {
        discountUiState = DiscountUiState()
    }

    fun suspendSale() {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { suspendSaleUseCase(saleId) }
    }

    fun resumeSale(sale: Sale) {
        val sessionId = cashSessionId.value ?: return
        viewModelScope.launch {
            resumeErrorMessage = when (resumeSaleUseCase(sessionId, sale.id)) {
                ResumeSaleResult.Success -> null
                ResumeSaleResult.CurrentCartNotEmpty ->
                    "Finalize ou suspenda a venda atual antes de retomar outra"
            }
        }
    }

    fun cancelSale(reason: String) {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch {
            cancelUiState = cancelUiState.copy(isLoading = true, errorMessage = null)
            when (cancelSaleUseCase(saleId, reason)) {
                CancelSaleResult.Success -> cancelUiState = CancelSaleUiState()
                CancelSaleResult.ReasonRequired ->
                    cancelUiState = cancelUiState.copy(
                        isLoading = false,
                        errorMessage = "Informe o motivo do cancelamento"
                    )
            }
        }
    }

    fun resetCancelPanel() {
        cancelUiState = CancelSaleUiState()
    }

    fun showCustomerPicker() {
        customerPickerVisible = true
        customerPickerUiState = CustomerPickerUiState()
        customerSearchQuery.value = ""
    }

    fun hideCustomerPicker() {
        customerPickerVisible = false
        customerSearchQuery.value = ""
    }

    fun onCustomerSearchQueryChange(query: String) {
        customerSearchQuery.value = query
    }

    fun selectCustomer(customer: Customer) {
        val sessionId = cashSessionId.value ?: return
        viewModelScope.launch {
            selectCustomerForSaleUseCase(sessionId, customer.id)
            hideCustomerPicker()
        }
    }

    fun createAndSelectCustomer(name: String, phone: String, document: String) {
        val sessionId = cashSessionId.value ?: return
        if (name.isBlank() || phone.isBlank()) {
            customerPickerUiState = customerPickerUiState.copy(errorMessage = "Informe nome e telefone")
            return
        }
        viewModelScope.launch {
            customerPickerUiState = customerPickerUiState.copy(isLoading = true, errorMessage = null)
            val customer = findOrCreateCustomerUseCase(name, phone, document.ifBlank { null })
            selectCustomerForSaleUseCase(sessionId, customer.id)
            hideCustomerPicker()
        }
    }

    fun removeCustomer() {
        val saleId = cart.value?.sale?.id ?: return
        viewModelScope.launch { removeCustomerFromSaleUseCase(saleId) }
    }

    fun redeemLoyalty() {
        val overview = cart.value ?: return
        val customerId = overview.sale.customerId ?: return
        viewModelScope.launch {
            redeemUiState = redeemUiState.copy(isLoading = true, errorMessage = null)
            when (val result = redeemLoyaltyUseCase(overview.sale, customerId, overview.netBeforeLoyaltyCents)) {
                is RedeemLoyaltyResult.Success -> redeemUiState = RedeemUiState()
                RedeemLoyaltyResult.NoActiveConfig ->
                    redeemUiState = redeemUiState.copy(isLoading = false, errorMessage = "Nenhum programa de fidelidade ativo")
                RedeemLoyaltyResult.NothingToRedeem ->
                    redeemUiState = redeemUiState.copy(isLoading = false, errorMessage = "Nada disponível para resgatar")
            }
        }
    }

    fun undoLoyaltyRedemption() {
        val sale = cart.value?.sale ?: return
        viewModelScope.launch {
            undoRedeemUiState = undoRedeemUiState.copy(isLoading = true, errorMessage = null)
            when (undoLoyaltyRedemptionUseCase(sale)) {
                UndoLoyaltyRedemptionResult.Success -> undoRedeemUiState = RedeemUiState()
                UndoLoyaltyRedemptionResult.NothingToUndo ->
                    undoRedeemUiState = undoRedeemUiState.copy(isLoading = false, errorMessage = "Nada para desfazer")
            }
        }
    }

    // Aviso (nao obrigatorio) de CPF: se o gestor ligou "pedir CPF" e a venda
    // ainda nao tem cliente com documento, mostra um lembrete antes do pagamento.
    fun finalizeSale() {
        if (remoteConfig.get().requireCustomerCpf && selectedCustomer?.document.isNullOrBlank()) {
            cpfReminderVisible = true
            return
        }
        proceedFinalize()
    }

    fun continueSaleWithoutCpf() {
        cpfReminderVisible = false
        proceedFinalize()
    }

    fun informCpf() {
        cpfReminderVisible = false
        showCustomerPicker()
    }

    private fun proceedFinalize() {
        val overview = cart.value ?: return
        viewModelScope.launch {
            stockErrorMessage = null
            when (val stockCheck = validateCartStockUseCase(overview.items)) {
                is ValidateCartStockResult.InsufficientStock -> {
                    stockErrorMessage = "Estoque insuficiente: ${stockCheck.productNames.joinToString(", ")}"
                    return@launch
                }
                ValidateCartStockResult.Ok -> {}
            }
            // Usa a venda retornada (não `overview.sale`, capturado antes da
            // chamada): é nela que o snapshot de terminal/loja gravado pelo
            // FinalizeSaleUseCase efetivamente aparece.
            val finalizedSale = finalizeSaleUseCase(overview.sale, overview.totalCents)
            pendingSale = overview.copy(sale = finalizedSale)
            selectedPaymentMethod = null
            paymentUiState = PaymentUiState()
            route = SaleRoute.PaymentMethod
        }
    }

    // Formas de pagamento aceitas (config local do negocio). Se o gerente
    // desligar todas (config invalida), nao trava a venda: aceita todas.
    fun acceptedPaymentMethods(): List<PaymentMethod> {
        val c = remoteConfig.get()
        val list = buildList {
            if (c.payCash) add(PaymentMethod.CASH)
            if (c.payCredit) add(PaymentMethod.CREDIT_CARD)
            if (c.payDebit) add(PaymentMethod.DEBIT_CARD)
            if (c.payPix) add(PaymentMethod.PIX)
        }
        return if (list.isEmpty()) PaymentMethod.entries.toList() else list
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        selectedPaymentMethod = method
        paymentUiState = PaymentUiState()
    }

    fun confirmPayment(receivedCents: Long?) {
        val sale = pendingSale?.sale ?: return
        val method = selectedPaymentMethod ?: return
        val total = pendingSale?.totalCents ?: return
        viewModelScope.launch {
            route = SaleRoute.Processing
            when (val result = processPaymentUseCase(sale, method, total, receivedCents)) {
                is ProcessPaymentResult.Approved -> {
                    lastPayment = result.payment
                    route = SaleRoute.Receipt
                }
                is ProcessPaymentResult.Declined -> {
                    paymentUiState = PaymentUiState(errorMessage = result.reason)
                    route = SaleRoute.PaymentMethod
                }
                ProcessPaymentResult.Timeout -> {
                    paymentUiState = PaymentUiState(errorMessage = "Tempo esgotado, tente novamente")
                    route = SaleRoute.PaymentMethod
                }
            }
        }
    }

    fun reopenCart() {
        val saleId = pendingSale?.sale?.id ?: return
        viewModelScope.launch {
            reopenSaleUseCase(saleId)
            pendingSale = null
            selectedPaymentMethod = null
            paymentUiState = PaymentUiState()
            route = SaleRoute.Cart
        }
    }

    fun startNewSale() {
        pendingSale = null
        selectedPaymentMethod = null
        lastPayment = null
        paymentUiState = PaymentUiState()
        printUiState = ReceiptActionUiState()
        sendUiState = ReceiptActionUiState()
        route = SaleRoute.Cart
    }

    fun printReceipt() {
        val overview = pendingSale ?: return
        val payment = lastPayment ?: return
        viewModelScope.launch {
            printUiState = printUiState.copy(isLoading = true, errorMessage = null)
            printUiState = when (val result = printReceiptUseCase(overview, payment)) {
                ReceiptPrintResult.Success -> ReceiptActionUiState()
                is ReceiptPrintResult.Failure -> ReceiptActionUiState(errorMessage = result.reason)
            }
        }
    }

    fun sendReceiptDigitally(channel: ReceiptChannel) {
        val overview = pendingSale ?: return
        val payment = lastPayment ?: return
        val destination = if (channel == ReceiptChannel.EMAIL) null else selectedCustomer?.phone
        viewModelScope.launch {
            sendUiState = sendUiState.copy(isLoading = true, errorMessage = null)
            sendUiState = when (
                val result = sendReceiptDigitallyUseCase(overview, payment, channel, destination)
            ) {
                ReceiptSendResult.Success -> ReceiptActionUiState()
                is ReceiptSendResult.Failure -> ReceiptActionUiState(errorMessage = result.reason)
            }
        }
    }
}
