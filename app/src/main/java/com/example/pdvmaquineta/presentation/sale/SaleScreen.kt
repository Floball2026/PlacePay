package com.example.pdvmaquineta.presentation.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.pdvmaquineta.presentation.theme.PdvNavySurfaceVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdvmaquineta.domain.loyalty.LoyaltyRedeemable
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.model.SaleItem
import com.example.pdvmaquineta.domain.usecase.CartOverview
import com.example.pdvmaquineta.presentation.authorization.SupervisorAuthorizationDialog
import com.example.pdvmaquineta.presentation.barcode.BarcodeScannerScreen
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.product.ProductImageStore
import com.example.pdvmaquineta.presentation.format.parseToCents
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import com.example.pdvmaquineta.presentation.theme.PdvBgWarning
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvFillDanger
import com.example.pdvmaquineta.presentation.theme.PdvOnDanger
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import com.example.pdvmaquineta.presentation.theme.PdvTextWarning

@Composable
fun SaleScreen(
    cart: CartOverview?,
    suspendedSales: List<Sale>,
    products: List<Product>,
    discountUiState: DiscountUiState,
    cancelUiState: CancelSaleUiState,
    resumeErrorMessage: String?,
    selectedCustomer: Customer?,
    loyaltyStatus: LoyaltyRedeemable?,
    redeemUiState: RedeemUiState,
    undoRedeemUiState: RedeemUiState,
    customerPickerVisible: Boolean,
    customerPickerUiState: CustomerPickerUiState,
    customerSearchResults: List<Customer>,
    onSearchQueryChange: (String) -> Unit,
    onAddProduct: (Product) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onCorrectItemPrice: (Long, Long) -> Unit,
    onApplyDiscount: (Int) -> Unit,
    onAuthorizeDiscount: (String, String) -> Unit,
    onDismissDiscountAuthorization: () -> Unit,
    onResetDiscountPanel: () -> Unit,
    onSuspendSale: () -> Unit,
    onResumeSale: (Sale) -> Unit,
    onCancelSale: (String) -> Unit,
    onResetCancelPanel: () -> Unit,
    onShowCustomerPicker: () -> Unit,
    onHideCustomerPicker: () -> Unit,
    onCustomerSearchQueryChange: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onCreateCustomer: (String, String, String) -> Unit,
    onRemoveCustomer: () -> Unit,
    onRedeemLoyalty: () -> Unit,
    onUndoRedeemLoyalty: () -> Unit,
    stockErrorMessage: String?,
    scanErrorMessage: String?,
    onSubmitBarcode: (String) -> Unit,
    onFinalizeSale: () -> Unit,
    cpfReminderVisible: Boolean,
    onCpfReminderInform: () -> Unit,
    onCpfReminderContinue: () -> Unit,
    cargaPluState: CargaPluUiState,
    onCargaPluLoad: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var showDiscountPanel by remember { mutableStateOf(false) }
    var showCancelPanel by remember { mutableStateOf(false) }
    var discountPercentText by remember { mutableStateOf("0") }
    var cancelReason by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showSupportMenu by remember { mutableStateOf(false) }
    var showPricePanel by remember { mutableStateOf(false) }
    var showCargaPlu by remember { mutableStateOf(false) }

    val items = cart?.items.orEmpty()
    val awaitingDiscountAuthorization = discountUiState.authorizationRequired && discountUiState.authorization == null
    val cartLocked = (cart?.loyaltyDiscountCents ?: 0) > 0

    val categories = remember(products) {
        listOf("Todos") + products.mapNotNull { it.category }.distinct().sorted()
    }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val filteredProducts = remember(products, selectedCategoryIndex) {
        if (selectedCategoryIndex == 0) products
        else products.filter { it.category == categories[selectedCategoryIndex] }
    }

    fun submitAndClear() {
        if (searchText.isNotBlank()) {
            onSubmitBarcode(searchText)
            searchText = ""
            onSearchQueryChange("")
        }
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { code ->
                showScanner = false
                onSubmitBarcode(code)
            },
            onCancel = { showScanner = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            CartBottomPanel(
                cart = cart,
                cartLocked = cartLocked,
                onFinalize = onFinalizeSale,
                onSupport = {
                    showPricePanel = false
                    showCancelPanel = false
                    showCargaPlu = false
                    showSupportMenu = !showSupportMenu
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(PdvDimens.SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PdvTextButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(PdvDimens.SpacingSmall))
                            Text("Voltar")
                        }
                        Text("Nova Venda", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(48.dp)) // Alinhamento
                    }

                    Spacer(Modifier.height(PdvDimens.SpacingMedium))

                    CustomerSection(
                        customer = selectedCustomer,
                        loyaltyStatus = loyaltyStatus,
                        redeemUiState = redeemUiState,
                        cartLocked = cartLocked,
                        onSelectCustomer = onShowCustomerPicker,
                        onRemoveCustomer = onRemoveCustomer,
                        onRedeem = onRedeemLoyalty
                    )

                    Spacer(Modifier.height(PdvDimens.SpacingMedium))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                onSearchQueryChange(it)
                            },
                            label = { Text("Buscar produto ou código") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitAndClear() }),
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                        submitAndClear()
                                        true
                                    } else {
                                        false
                                    }
                                }
                        )
                        Spacer(Modifier.width(PdvDimens.SpacingSmall))
                        PdvOutlinedButton(onClick = { showScanner = true }, enabled = !cartLocked) {
                            Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "Escanear")
                        }
                    }

                    scanErrorMessage?.let {
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    stockErrorMessage?.let {
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(PdvDimens.SpacingMedium))

                    CategoryTabs(
                        categories = categories,
                        selectedIndex = selectedCategoryIndex,
                        onCategorySelected = { selectedCategoryIndex = it }
                    )

                    Spacer(Modifier.height(PdvDimens.SpacingMedium))
                }
            }

            items(filteredProducts) { product ->
                ProductTile(
                    product = product,
                    onAdd = { onAddProduct(product) },
                    enabled = !cartLocked
                )
            }

            if (suspendedSales.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(PdvDimens.SpacingLarge))
                        Text("Vendas suspensas", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        resumeErrorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        }
                        suspendedSales.forEach { sale ->
                            SuspendedSaleRow(sale = sale, onResume = { onResumeSale(sale) })
                            Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(Modifier.height(PdvDimens.SpacingLarge))
                    Text("Itens no Carrinho", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))

                    if (items.isEmpty()) {
                        Text(
                            "Vazio",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        items.forEach { item ->
                            CartItemRow(
                                item = item,
                                enabled = !cartLocked,
                                onIncrement = { onIncrement(item.productId) },
                                onDecrement = { onDecrement(item.productId) },
                                onRemove = { onRemove(item.productId) }
                            )
                            Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        }
                    }

                    if (cart != null && cart.sale.discountPercent > 0) {
                        Text(
                            "Desconto de ${cart.sale.discountPercent}% (-${formatCents(cart.discountCents)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (cart != null && cart.loyaltyDiscountCents > 0) {
                        Text(
                            "Fidelidade (-${formatCents(cart.loyaltyDiscountCents)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        PdvOutlinedButton(
                            onClick = onUndoRedeemLoyalty,
                            enabled = !undoRedeemUiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Desfazer resgate")
                        }
                    }

                    if (showDiscountPanel) {
                        DiscountPanel(
                            discountPercentText = discountPercentText,
                            onDiscountPercentChange = { discountPercentText = it },
                            uiState = discountUiState,
                            cartLocked = cartLocked,
                            onApply = { discountPercentText.toIntOrNull()?.let(onApplyDiscount) },
                            onCancel = {
                                showDiscountPanel = false
                                onResetDiscountPanel()
                            }
                        )
                    }

                    Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))
                }
            }
        }
    }

    if (awaitingDiscountAuthorization) {
        SupervisorAuthorizationDialog(
            errorMessage = discountUiState.errorMessage,
            onAuthorize = onAuthorizeDiscount,
            onDismiss = onDismissDiscountAuthorization
        )
    }

    if (cpfReminderVisible) {
        AlertDialog(
            onDismissRequest = onCpfReminderContinue,
            title = { Text("CPF do cliente") },
            text = { Text("Deseja informar o CPF do cliente nesta venda?") },
            confirmButton = {
                PdvTextButton(onClick = onCpfReminderInform) { Text("Informar CPF") }
            },
            dismissButton = {
                PdvTextButton(onClick = onCpfReminderContinue) { Text("Continuar sem CPF") }
            }
        )
    }

    if (customerPickerVisible) {
        CustomerPickerDialog(
            results = customerSearchResults,
            uiState = customerPickerUiState,
            onSearchQueryChange = onCustomerSearchQueryChange,
            onSelectCustomer = onSelectCustomer,
            onCreateCustomer = onCreateCustomer,
            onDismiss = onHideCustomerPicker
        )
    }

    if (showSupportMenu) {
        Dialog(onDismissRequest = { showSupportMenu = false }) {
            SupportMenuPanel(
                onCorrectPrice = {
                    showSupportMenu = false
                    showPricePanel = true
                },
                onCancelSale = {
                    showSupportMenu = false
                    showCancelPanel = true
                },
                onCargaPlu = {
                    showSupportMenu = false
                    showCargaPlu = true
                },
                onClose = { showSupportMenu = false }
            )
        }
    }

    if (showPricePanel) {
        Dialog(onDismissRequest = { showPricePanel = false }) {
            PriceCorrectionPanel(
                items = items,
                onApply = { productId, cents -> onCorrectItemPrice(productId, cents) },
                onClose = { showPricePanel = false }
            )
        }
    }

    if (showCancelPanel) {
        Dialog(onDismissRequest = {
            showCancelPanel = false
            onResetCancelPanel()
        }) {
            CancelPanel(
                reason = cancelReason,
                onReasonChange = { cancelReason = it },
                uiState = cancelUiState,
                onConfirm = { onCancelSale(cancelReason) },
                onCancel = {
                    showCancelPanel = false
                    onResetCancelPanel()
                }
            )
        }
    }

    if (showCargaPlu) {
        Dialog(onDismissRequest = { showCargaPlu = false }) {
            CargaPluPanel(
                state = cargaPluState,
                onLoad = onCargaPluLoad,
                onClose = { showCargaPlu = false }
            )
        }
    }
}

@Composable
private fun CustomerSection(
    customer: Customer?,
    loyaltyStatus: LoyaltyRedeemable?,
    redeemUiState: RedeemUiState,
    cartLocked: Boolean,
    onSelectCustomer: () -> Unit,
    onRemoveCustomer: () -> Unit,
    onRedeem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cliente: ${customer?.name ?: "não identificado"}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = PdvDimens.SpacingSmall)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PdvOutlinedButton(
                    onClick = onSelectCustomer,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(if (customer != null) "Trocar" else "Selecionar", fontSize = 14.sp)
                }
                if (customer != null) {
                    PdvTextButton(onClick = onRemoveCustomer, enabled = !cartLocked) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Remover cliente")
                    }
                }
            }
        }

        if (customer != null && cartLocked) {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(
                "Carrinho travado enquanto o resgate estiver aplicado. Desfaça o resgate " +
                    "para editar itens ou desconto.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (customer != null && loyaltyStatus != null) {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            loyaltyStatus.pointsAvailable?.let {
                Text("Pontos disponíveis: $it", style = MaterialTheme.typography.bodyLarge)
            }
            loyaltyStatus.visitsCount?.let { visits ->
                Text(
                    "Visitas: $visits/${loyaltyStatus.visitsRequired}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            redeemUiState.errorMessage?.let {
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (loyaltyStatus.redeemableCents > 0) {
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
                PdvOutlinedButton(
                    onClick = onRedeem,
                    enabled = !redeemUiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resgatar (${formatCents(loyaltyStatus.redeemableCents)})")
                }
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    results: List<Customer>,
    uiState: CustomerPickerUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onCreateCustomer: (name: String, phone: String, document: String) -> Unit,
    onDismiss: () -> Unit
) {
    var cpf by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar cliente") },
        text = {
            Column {
                OutlinedTextField(
                    value = cpf,
                    onValueChange = {
                        cpf = it
                        onSearchQueryChange(it)
                    },
                    label = { Text("CPF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(PdvDimens.SpacingSmall))

                results.forEach { customer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustomer(customer) }
                            .padding(vertical = PdvDimens.SpacingSmall)
                    ) {
                        Text("${customer.name} — ${customer.document ?: customer.phone}", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (results.isEmpty() && cpf.isNotBlank()) {
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(
                        "Nenhum cliente encontrado. Cadastrar novo:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.errorMessage?.let { error ->
                        Spacer(Modifier.height(PdvDimens.SpacingSmall))
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    PdvButton(
                        onClick = { onCreateCustomer(name, phone, cpf) },
                        enabled = !uiState.isLoading && name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cadastrar e selecionar")
                    }
                }
            }
        },
        confirmButton = {
            PdvTextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun CategoryTabs(
    categories: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onCategorySelected(index) },
                text = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun ProductTile(
    product: Product,
    onAdd: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) { onAdd() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(PdvDimens.SpacingSmall)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                val imageModel: Any? = product.imageUrl
                    ?: product.imagePath?.let { ProductImageStore.fileFor(LocalContext.current, it) }
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = product.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCents(product.priceCents),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun CartBottomPanel(
    cart: CartOverview?,
    cartLocked: Boolean,
    onFinalize: () -> Unit,
    onSupport: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PdvDimens.SpacingMedium, vertical = PdvDimens.SpacingSmall)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${cart?.itemCount ?: 0} itens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCents(cart?.totalCents ?: 0),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black
                    )
                }
                PdvButton(
                    onClick = onFinalize,
                    enabled = (cart?.itemCount ?: 0) > 0,
                    modifier = Modifier
                        .width(150.dp)
                        .height(48.dp)
                ) {
                    Text("FINALIZAR")
                }
            }

            Spacer(Modifier.height(6.dp))

            PdvOutlinedButton(
                onClick = onSupport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("Suporte Venda", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DiscountPanel(
    discountPercentText: String,
    onDiscountPercentChange: (String) -> Unit,
    uiState: DiscountUiState,
    cartLocked: Boolean,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text("Desconto no total (%)", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        OutlinedTextField(
            value = discountPercentText,
            onValueChange = onDiscountPercentChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        uiState.errorMessage?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Row(horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)) {
            PdvButton(
                onClick = onApply,
                enabled = !uiState.isLoading && !cartLocked && discountPercentText.toIntOrNull() != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Aplicar")
            }
            PdvOutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Fechar")
            }
        }
    }
}

@Composable
private fun CancelPanel(
    reason: String,
    onReasonChange: (String) -> Unit,
    uiState: CancelSaleUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text("Motivo do cancelamento", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        uiState.errorMessage?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Row(horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)) {
            PdvButton(
                onClick = onConfirm,
                enabled = !uiState.isLoading && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PdvFillDanger,
                    contentColor = PdvOnDanger
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Confirmar")
            }
            PdvOutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Voltar")
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: SaleItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.productName, style = MaterialTheme.typography.bodyLarge)
            Text(formatCents(item.unitPriceCents * item.quantity), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdvOutlinedButton(onClick = onDecrement, enabled = enabled) { Text("−") }
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("${item.quantity}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            PdvOutlinedButton(onClick = onIncrement, enabled = enabled) { Text("+") }
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            PdvTextButton(onClick = onRemove, enabled = enabled) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remover ${item.productName}")
            }
        }
    }
}

@Composable
private fun SuspendedSaleRow(sale: Sale, onResume: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PdvBgWarning, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Venda suspensa #${sale.id}", color = PdvTextWarning, style = MaterialTheme.typography.bodyLarge)
        PdvTextButton(onClick = onResume) { Text("Retomar") }
    }
}

@Composable
private fun SupportMenuPanel(
    onCorrectPrice: () -> Unit,
    onCancelSale: () -> Unit,
    onCargaPlu: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text("Suporte à venda", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvOutlinedButton(onClick = onCorrectPrice, modifier = Modifier.fillMaxWidth()) {
            Text("Correção de Preço")
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvOutlinedButton(onClick = onCancelSale, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar venda")
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvOutlinedButton(onClick = onCargaPlu, modifier = Modifier.fillMaxWidth()) {
            Text("Carga PLU")
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvTextButton(onClick = onClose) { Text("Fechar") }
    }
}

@Composable
private fun PriceCorrectionPanel(
    items: List<SaleItem>,
    onApply: (Long, Long) -> Unit,
    onClose: () -> Unit
) {
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text("Correção de preço", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        if (items.isEmpty()) {
            Text(
                "Nenhum item no carrinho.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            items.forEach { item ->
                val selected = selectedProductId == item.productId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProductId = item.productId }
                        .padding(vertical = PdvDimens.SpacingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (if (selected) "● " else "○ ") + item.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatCents(item.unitPriceCents), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Novo preço (R$)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(PdvDimens.SpacingMedium))
            Row(horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)) {
                PdvButton(
                    onClick = {
                        val pid = selectedProductId ?: return@PdvButton
                        val cents = parseToCents(priceText) ?: return@PdvButton
                        onApply(pid, cents)
                        onClose()
                    },
                    enabled = selectedProductId != null && parseToCents(priceText) != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Aplicar") }
                PdvOutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Fechar") }
            }
        }
    }
}

@Composable
private fun CargaPluPanel(
    state: CargaPluUiState,
    onLoad: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(PdvDimens.SpacingMedium)
    ) {
        Text("Carga PLU", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        Text(
            "Baixa os produtos e preços cadastrados no servidor para este terminal.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvButton(
            onClick = onLoad,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isLoading) "Carregando..." else "Carregar produtos agora")
        }
        state.message?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.tertiary)
        }
        state.error?.let {
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        PdvTextButton(onClick = onClose) { Text("Fechar") }
    }
}
