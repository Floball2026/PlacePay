package com.example.pdvmaquineta.presentation.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.Sale
import com.example.pdvmaquineta.domain.receipt.ReceiptChannel
import com.example.pdvmaquineta.domain.receipt.ReceiptPrintResult
import com.example.pdvmaquineta.domain.receipt.ReceiptSendResult
import com.example.pdvmaquineta.domain.usecase.GetCustomerUseCase
import com.example.pdvmaquineta.domain.usecase.GetSaleReceiptDataUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveCompletedSalesUseCase
import com.example.pdvmaquineta.domain.usecase.PrintReceiptUseCase
import com.example.pdvmaquineta.domain.usecase.ReceiptData
import com.example.pdvmaquineta.domain.usecase.SendReceiptDigitallyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SaleHistoryRoute {
    data object List : SaleHistoryRoute()
    data object Receipt : SaleHistoryRoute()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SaleHistoryViewModel @Inject constructor(
    observeCompletedSalesUseCase: ObserveCompletedSalesUseCase,
    private val getSaleReceiptDataUseCase: GetSaleReceiptDataUseCase,
    private val getCustomerUseCase: GetCustomerUseCase,
    private val printReceiptUseCase: PrintReceiptUseCase,
    private val sendReceiptDigitallyUseCase: SendReceiptDigitallyUseCase
) : ViewModel() {

    private val fromMillis = MutableStateFlow<Long?>(null)
    private val toMillis = MutableStateFlow<Long?>(null)

    val sales: StateFlow<List<Sale>> = combine(fromMillis, toMillis) { from, to -> from to to }
        .flatMapLatest { (from, to) -> observeCompletedSalesUseCase(from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var route by mutableStateOf<SaleHistoryRoute>(SaleHistoryRoute.List)
        private set

    var receiptData by mutableStateOf<ReceiptData?>(null)
        private set

    var printUiState by mutableStateOf(ReceiptActionUiState())
        private set

    var sendUiState by mutableStateOf(ReceiptActionUiState())
        private set

    fun setDateFilter(fromMillis: Long?, toMillis: Long?) {
        this.fromMillis.value = fromMillis
        this.toMillis.value = toMillis
    }

    fun openReceipt(saleId: Long) {
        viewModelScope.launch {
            receiptData = getSaleReceiptDataUseCase(saleId)
            printUiState = ReceiptActionUiState()
            sendUiState = ReceiptActionUiState()
            route = SaleHistoryRoute.Receipt
        }
    }

    fun backToList() {
        receiptData = null
        route = SaleHistoryRoute.List
    }

    fun printReceipt() {
        val data = receiptData ?: return
        viewModelScope.launch {
            printUiState = printUiState.copy(isLoading = true, errorMessage = null)
            printUiState = when (val result = printReceiptUseCase(data.cart, data.payment)) {
                ReceiptPrintResult.Success -> ReceiptActionUiState()
                is ReceiptPrintResult.Failure -> ReceiptActionUiState(errorMessage = result.reason)
            }
        }
    }

    fun sendReceiptDigitally(channel: ReceiptChannel) {
        val data = receiptData ?: return
        viewModelScope.launch {
            sendUiState = sendUiState.copy(isLoading = true, errorMessage = null)
            val customerId = data.cart.sale.customerId
            val destination = if (channel == ReceiptChannel.EMAIL || customerId == null) {
                null
            } else {
                getCustomerUseCase(customerId)?.phone
            }
            sendUiState = when (
                val result = sendReceiptDigitallyUseCase(data.cart, data.payment, channel, destination)
            ) {
                ReceiptSendResult.Success -> ReceiptActionUiState()
                is ReceiptSendResult.Failure -> ReceiptActionUiState(errorMessage = result.reason)
            }
        }
    }
}
