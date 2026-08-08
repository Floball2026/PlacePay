package com.example.pdvmaquineta.presentation.product

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import com.example.pdvmaquineta.presentation.theme.PdvTextWarning

@Composable
fun ProductListScreen(
    products: List<Product>,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onToggleActive: (Product, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            "Produtos",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = onAddProduct,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text("Novo produto")
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        if (products.isEmpty()) {
            Text(
                "Nenhum produto cadastrado ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            products.forEach { product ->
                ProductManagementRow(
                    product = product,
                    onEdit = { onEditProduct(product) },
                    onToggleActive = { onToggleActive(product, !product.active) }
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
private fun ProductManagementRow(
    product: Product,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(PdvDimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val lowStock = product.minStockAlert != null && product.stockQuantity <= product.minStockAlert
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge)
                if (!product.active) {
                    Spacer(Modifier.width(PdvDimens.SpacingSmall))
                    Text("Inativo", style = MaterialTheme.typography.bodyLarge, color = PdvTextWarning)
                }
                if (lowStock) {
                    Spacer(Modifier.width(PdvDimens.SpacingSmall))
                    Text("Estoque baixo", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                formatCents(product.priceCents) + (product.category?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Estoque: ${product.stockQuantity}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            PdvTextButton(onClick = onEdit) { Text("Editar") }
            PdvTextButton(onClick = onToggleActive) {
                Text(if (product.active) "Desativar" else "Ativar")
            }
        }
    }
}
