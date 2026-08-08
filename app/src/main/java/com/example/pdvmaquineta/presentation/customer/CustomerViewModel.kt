package com.example.pdvmaquineta.presentation.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.domain.model.LoyaltyTransaction
import com.example.pdvmaquineta.domain.usecase.FindOrCreateCustomerUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveLoyaltyTransactionsUseCase
import com.example.pdvmaquineta.domain.usecase.SearchCustomersUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateCustomerResult
import com.example.pdvmaquineta.domain.usecase.UpdateCustomerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CustomerRoute {
    data object List : CustomerRoute()
    data class Form(val customer: Customer?) : CustomerRoute()
    data class History(val customer: Customer) : CustomerRoute()
}

data class CustomerFormUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val searchCustomersUseCase: SearchCustomersUseCase,
    private val findOrCreateCustomerUseCase: FindOrCreateCustomerUseCase,
    private val updateCustomerUseCase: UpdateCustomerUseCase,
    private val observeLoyaltyTransactionsUseCase: ObserveLoyaltyTransactionsUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val customers: StateFlow<List<Customer>> = searchQuery
        .flatMapLatest { query -> searchCustomersUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var route by mutableStateOf<CustomerRoute>(CustomerRoute.List)
        private set

    var formUiState by mutableStateOf(CustomerFormUiState())
        private set

    private val _history = MutableStateFlow<List<LoyaltyTransaction>>(emptyList())
    val history: StateFlow<List<LoyaltyTransaction>> = _history.asStateFlow()

    private var historyJob: Job? = null

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun openNewCustomerForm() {
        formUiState = CustomerFormUiState()
        route = CustomerRoute.Form(null)
    }

    fun openEditCustomerForm(customer: Customer) {
        formUiState = CustomerFormUiState()
        route = CustomerRoute.Form(customer)
    }

    fun openHistory(customer: Customer) {
        route = CustomerRoute.History(customer)
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            observeLoyaltyTransactionsUseCase(customer.id).collect { _history.value = it }
        }
    }

    fun backToList() {
        historyJob?.cancel()
        _history.value = emptyList()
        route = CustomerRoute.List
    }

    fun saveCustomer(id: Long?, name: String, phone: String, document: String?) {
        viewModelScope.launch {
            formUiState = formUiState.copy(isLoading = true, errorMessage = null)
            if (id == null) {
                findOrCreateCustomerUseCase(name, phone, document)
                formUiState = CustomerFormUiState()
                route = CustomerRoute.List
            } else {
                when (updateCustomerUseCase(id, name, phone, document)) {
                    is UpdateCustomerResult.Success -> {
                        formUiState = CustomerFormUiState()
                        route = CustomerRoute.List
                    }
                    UpdateCustomerResult.InvalidData ->
                        formUiState = formUiState.copy(isLoading = false, errorMessage = "Informe nome e telefone")
                    UpdateCustomerResult.PhoneTaken ->
                        formUiState = formUiState.copy(
                            isLoading = false,
                            errorMessage = "Já existe outro cliente com esse telefone"
                        )
                }
            }
        }
    }
}
