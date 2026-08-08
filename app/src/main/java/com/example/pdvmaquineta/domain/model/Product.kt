package com.example.pdvmaquineta.domain.model

data class Product(
    val id: Long,
    val name: String,
    val priceCents: Long,
    val category: String?,
    val active: Boolean,
    val stockQuantity: Int,
    // Nulo = sem alerta configurado pra esse produto.
    val minStockAlert: Int?,
    // Configurável por produto (decisão de negócio): permite finalizar venda
    // mesmo com estoque insuficiente/zerado — nesse caso stockQuantity pode
    // ficar negativo, refletindo estoque "a repor".
    val allowSaleWithoutStock: Boolean,
    // Opcional (decisão de negócio) — nulo quando o produto não tem código
    // de barras cadastrado.
    val barcode: String?,
    val createdAt: Long,
    val updatedAt: Long,
    // Nome do arquivo de imagem no armazenamento interno (pasta product_images).
    // Nulo = sem foto. Preenchido pelo seletor de galeria hoje e, no futuro,
    // pela interface externa de sincronizacao (mesmo campo/pasta).
    val imagePath: String? = null,
    // Id do produto no servidor (SaaS) e URL da imagem remota — preenchidos na
    // sincronizacao (Carga PLU). Nulos para produtos criados so localmente.
    val remoteId: String? = null,
    val imageUrl: String? = null
)
