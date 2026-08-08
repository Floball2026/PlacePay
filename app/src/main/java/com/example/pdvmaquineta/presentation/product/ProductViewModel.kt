package com.example.pdvmaquineta.presentation.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.usecase.CreateProductUseCase
import com.example.pdvmaquineta.domain.usecase.ObserveProductsUseCase
import com.example.pdvmaquineta.domain.usecase.SaveProductResult
import com.example.pdvmaquineta.domain.usecase.SetProductActiveUseCase
import com.example.pdvmaquineta.domain.usecase.UpdateProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProductRoute {
    data object List : ProductRoute()
    data class Form(val product: Product?) : ProductRoute()
}

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    observeProductsUseCase: ObserveProductsUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val setProductActiveUseCase: SetProductActiveUseCase
) : ViewModel() {

    val products: StateFlow<List<Product>> = observeProductsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var route by mutableStateOf<ProductRoute>(ProductRoute.List)
        private set

    var formUiState by mutableStateOf(ProductFormUiState())
        private set

    fun openNewProductForm() {
        formUiState = ProductFormUiState()
        route = ProductRoute.Form(null)
    }

    fun openEditProductForm(product: Product) {
        formUiState = ProductFormUiState()
        route = ProductRoute.Form(product)
    }

    fun backToList() {
        route = ProductRoute.List
    }

    fun saveProduct(
        id: Long?,
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ) {
        viewModelScope.launch {
            formUiState = formUiState.copy(isLoading = true, errorMessage = null)
            val result = if (id == null) {
                createProductUseCase(
                    name, priceCents, category, stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath
                )
            } else {
                updateProductUseCase(
                    id, name, priceCents, category, stockQuantity, minStockAlert, allowSaleWithoutStock, barcode, imagePath
                )
            }
            when (result) {
                is SaveProductResult.Success -> {
                    formUiState = ProductFormUiState()
                    route = ProductRoute.List
                }
                SaveProductResult.NotAuthorized ->
                    formUiState = formUiState.copy(isLoading = false, errorMessage = "Sem permissão para essa ação")
                SaveProductResult.InvalidData ->
                    formUiState = formUiState.copy(isLoading = false, errorMessage = "Dados inválidos")
                is SaveProductResult.DuplicateBarcode ->
                    formUiState = formUiState.copy(
                        isLoading = false,
                        errorMessage = "Código de barras já cadastrado no produto \"${result.existingProductName}\""
                    )
            }
        }
    }

    fun setActive(product: Product, active: Boolean) {
        viewModelScope.launch { setProductActiveUseCase(product.id, active) }
    }
}
