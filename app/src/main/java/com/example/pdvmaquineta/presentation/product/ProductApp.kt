package com.example.pdvmaquineta.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun ProductApp(
    viewModel: ProductViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()

    when (val route = viewModel.route) {
        ProductRoute.List -> ProductListScreen(
            products = products,
            onAddProduct = viewModel::openNewProductForm,
            onEditProduct = viewModel::openEditProductForm,
            onToggleActive = { product, active -> viewModel.setActive(product, active) },
            onBack = onBack,
            modifier = modifier
        )

        is ProductRoute.Form -> ProductFormScreen(
            existingProduct = route.product,
            uiState = viewModel.formUiState,
            onSave = { name, priceCents, category, stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath ->
                viewModel.saveProduct(
                    route.product?.id, name, priceCents, category,
                    stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath
                )
            },
            onCancel = viewModel::backToList,
            modifier = modifier
        )
    }
}
