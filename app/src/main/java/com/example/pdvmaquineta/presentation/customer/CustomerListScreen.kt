package com.example.pdvmaquineta.presentation.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.Customer
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton

@Composable
fun CustomerListScreen(
    customers: List<Customer>,
    onSearchQueryChange: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onViewHistory: (Customer) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PdvDimens.SpacingLarge),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text(
            "Clientes",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = onAddCustomer,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Novo cliente")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearchQueryChange(it)
            },
            label = { Text("Buscar por nome ou telefone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (customers.isEmpty()) {
            Text(
                "Nenhum cliente encontrado.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            customers.forEach { customer ->
                CustomerRow(
                    customer = customer,
                    onEdit = { onEditCustomer(customer) },
                    onHistory = { onViewHistory(customer) }
                )
                Spacer(Modifier.height(PdvDimens.SpacingSmall))
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
        PdvTextButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Voltar ao caixa")
        }
        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}

@Composable
private fun CustomerRow(
    customer: Customer,
    onEdit: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall)
    ) {
        Text(customer.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            customer.phone,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            PdvTextButton(onClick = onEdit) { Text("Editar") }
            PdvTextButton(onClick = onHistory) { Text("Histórico de fidelidade") }
        }
    }
}
