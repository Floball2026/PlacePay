package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.repository.ProductRepository
import javax.inject.Inject

// Resolve um código lido por câmera/leitor físico (RF-010/011) — usado na
// tela de venda tanto pelo botão "Escanear" quanto pelo Enter do leitor
// físico USB/Bluetooth agindo como teclado.
class FindProductByBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): Product? = productRepository.findByBarcode(barcode)
}
