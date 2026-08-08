package com.example.pdvmaquineta.presentation.customer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun CustomerApp(
    viewModel: CustomerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val history by viewModel.history.collectAsState()

    when (val route = viewModel.route) {
        CustomerRoute.List -> CustomerListScreen(
            customers = customers,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onAddCustomer = viewModel::openNewCustomerForm,
            onEditCustomer = viewModel::openEditCustomerForm,
            onViewHistory = viewModel::openHistory,
            onBack = onBack,
            modifier = modifier
        )

        is CustomerRoute.Form -> CustomerFormScreen(
            existingCustomer = route.customer,
            uiState = viewModel.formUiState,
            onSave = { name, phone, document ->
                viewModel.saveCustomer(route.customer?.id, name, phone, document)
            },
            onCancel = viewModel::backToList,
            modifier = modifier
        )

        is CustomerRoute.History -> CustomerHistoryScreen(
            customer = route.customer,
            transactions = history,
            onBack = viewModel::backToList,
            modifier = modifier
        )
    }
}
