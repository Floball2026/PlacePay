package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Product

sealed class SaveProductResult {
    data class Success(val product: Product) : SaveProductResult()
    data object NotAuthorized : SaveProductResult()
    data object InvalidData : SaveProductResult()
    // Código de barras já usado por outro produto (índice único em `barcode`).
    data class DuplicateBarcode(val existingProductName: String) : SaveProductResult()
}
