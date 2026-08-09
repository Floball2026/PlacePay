package com.example.pdvmaquineta.presentation.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun SaleApp(
    viewModel: SaleViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.cart.collectAsState()
    val suspendedSales by viewModel.suspendedSales.collectAsState()
    val products by viewModel.products.collectAsState()
    val customerSearchResults by viewModel.customerSearchResults.collectAsState()

    when (viewModel.route) {
        SaleRoute.Cart -> SaleScreen(
            cart = cart,
            suspendedSales = suspendedSales,
            products = products,
            discountUiState = viewModel.discountUiState,
            cancelUiState = viewModel.cancelUiState,
            resumeErrorMessage = viewModel.resumeErrorMessage,
            selectedCustomer = viewModel.selectedCustomer,
            loyaltyStatus = viewModel.loyaltyStatus,
            redeemUiState = viewModel.redeemUiState,
            undoRedeemUiState = viewModel.undoRedeemUiState,
            customerPickerVisible = viewModel.customerPickerVisible,
            customerPickerUiState = viewModel.customerPickerUiState,
            customerSearchResults = customerSearchResults,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onAddProduct = viewModel::addToCart,
            onIncrement = viewModel::incrementItem,
            onDecrement = viewModel::decrementItem,
            onRemove = viewModel::removeItem,
            onCorrectItemPrice = viewModel::correctItemPrice,
            onApplyDiscount = viewModel::applyDiscount,
            onAuthorizeDiscount = viewModel::authorizeDiscount,
            onDismissDiscountAuthorization = viewModel::dismissDiscountAuthorization,
            onResetDiscountPanel = viewModel::resetDiscountPanel,
            onSuspendSale = viewModel::suspendSale,
            onResumeSale = viewModel::resumeSale,
            onCancelSale = viewModel::cancelSale,
            onResetCancelPanel = viewModel::resetCancelPanel,
            onShowCustomerPicker = viewModel::showCustomerPicker,
            onHideCustomerPicker = viewModel::hideCustomerPicker,
            onCustomerSearchQueryChange = viewModel::onCustomerSearchQueryChange,
            onSelectCustomer = viewModel::selectCustomer,
            onCreateCustomer = viewModel::createAndSelectCustomer,
            onRemoveCustomer = viewModel::removeCustomer,
            onRedeemLoyalty = viewModel::redeemLoyalty,
            onUndoRedeemLoyalty = viewModel::undoLoyaltyRedemption,
            stockErrorMessage = viewModel.stockErrorMessage,
            scanErrorMessage = viewModel.scanErrorMessage,
            onSubmitBarcode = viewModel::submitBarcode,
            onFinalizeSale = viewModel::finalizeSale,
            cpfReminderVisible = viewModel.cpfReminderVisible,
            onCpfReminderInform = viewModel::informCpf,
            onCpfReminderContinue = viewModel::continueSaleWithoutCpf,
            cargaPluState = viewModel.cargaPluState,
            onCargaPluLoad = viewModel::cargaPlu,
            onBack = onBack,
            modifier = modifier
        )

        SaleRoute.PaymentMethod -> PaymentMethodScreen(
            totalCents = viewModel.pendingSale?.totalCents ?: 0,
            selectedMethod = viewModel.selectedPaymentMethod,
            acceptedMethods = viewModel.acceptedPaymentMethods(),
            uiState = viewModel.paymentUiState,
            onSelectMethod = viewModel::selectPaymentMethod,
            onConfirm = viewModel::confirmPayment,
            onBackToCart = viewModel::reopenCart,
            modifier = modifier
        )

        SaleRoute.Processing -> PaymentProcessingScreen(modifier = modifier)

        SaleRoute.Receipt -> {
            val sale = viewModel.pendingSale
            val payment = viewModel.lastPayment
            if (sale != null && payment != null) {
                ReceiptScreen(
                    sale = sale,
                    payment = payment,
                    printUiState = viewModel.printUiState,
                    sendUiState = viewModel.sendUiState,
                    onPrint = viewModel::printReceipt,
                    onSendDigitally = viewModel::sendReceiptDigitally,
                    bottomActionLabel = "Nova venda",
                    onBottomAction = viewModel::startNewSale,
                    modifier = modifier
                )
            }
        }
    }
}
