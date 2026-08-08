package com.example.pdvmaquineta.presentation.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun SaleHistoryApp(
    viewModel: SaleHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()

    when (viewModel.route) {
        SaleHistoryRoute.List -> SaleHistoryScreen(
            sales = sales,
            onDateFilterChange = viewModel::setDateFilter,
            onOpenSale = viewModel::openReceipt,
            onBack = onBack,
            modifier = modifier
        )

        SaleHistoryRoute.Receipt -> {
            val data = viewModel.receiptData
            if (data != null) {
                ReceiptScreen(
                    sale = data.cart,
                    payment = data.payment,
                    printUiState = viewModel.printUiState,
                    sendUiState = viewModel.sendUiState,
                    onPrint = viewModel::printReceipt,
                    onSendDigitally = viewModel::sendReceiptDigitally,
                    bottomActionLabel = "Voltar ao histórico",
                    onBottomAction = viewModel::backToList,
                    modifier = modifier
                )
            }
        }
    }
}
