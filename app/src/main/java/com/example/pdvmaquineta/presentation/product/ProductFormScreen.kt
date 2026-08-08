package com.example.pdvmaquineta.presentation.product

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.presentation.barcode.BarcodeScannerScreen
import com.example.pdvmaquineta.presentation.format.parseToCents
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton
import com.example.pdvmaquineta.presentation.theme.PdvTextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProductFormScreen(
    existingProduct: Product?,
    uiState: ProductFormUiState,
    onSave: (
        name: String,
        priceCents: Long,
        category: String?,
        stockQuantity: Int,
        minStockAlert: Int?,
        allowSaleWithoutStock: Boolean,
        barcode: String?,
        imagePath: String?
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(existingProduct?.name.orEmpty()) }
    var priceText by remember {
        mutableStateOf(existingProduct?.let { (it.priceCents / 100.0).toString() }.orEmpty())
    }
    var category by remember { mutableStateOf(existingProduct?.category.orEmpty()) }
    var stockQuantityText by remember {
        mutableStateOf((existingProduct?.stockQuantity ?: 0).toString())
    }
    var minStockAlertText by remember {
        mutableStateOf(existingProduct?.minStockAlert?.toString().orEmpty())
    }
    var allowSaleWithoutStock by remember {
        mutableStateOf(existingProduct?.allowSaleWithoutStock ?: false)
    }
    var barcode by remember { mutableStateOf(existingProduct?.barcode.orEmpty()) }
    var showScanner by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageName by remember { mutableStateOf(existingProduct?.imagePath) }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { ProductImageStore.importFromUri(context, uri) }
                imageName = saved
            }
        }
    }
    val priceCents = parseToCents(priceText)
    val stockQuantity = stockQuantityText.toIntOrNull()
    val minStockAlert = minStockAlertText.toIntOrNull()

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showScanner = false
            },
            onCancel = { showScanner = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

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
            text = if (existingProduct == null) "Novo produto" else "Editar produto",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PdvDimens.SpacingExtraLarge))

        // Foto do produto (opcional). Guardada localmente (offline).
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(PdvDimens.CornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val current = imageName
            if (current != null) {
                AsyncImage(
                    model = ProductImageStore.fileFor(context, current),
                    contentDescription = "Foto do produto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    "Sem foto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        Row(horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingSmall)) {
            PdvOutlinedButton(onClick = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text(if (imageName == null) "Adicionar foto" else "Trocar foto")
            }
            if (imageName != null) {
                PdvTextButton(onClick = { imageName = null }) { Text("Remover") }
            }
        }
        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Preço (R$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoria (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = stockQuantityText,
            onValueChange = { stockQuantityText = it },
            label = { Text("Estoque atual") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Código de barras (opcional)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            PdvOutlinedButton(onClick = { showScanner = true }) {
                Text("Escanear")
            }
        }
        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        OutlinedTextField(
            value = minStockAlertText,
            onValueChange = { minStockAlertText = it },
            label = { Text("Alerta de estoque mínimo (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        Text("Venda sem estoque", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        val toggleModifier = Modifier.fillMaxWidth().height(PdvDimens.ButtonHeight)
        if (!allowSaleWithoutStock) {
            PdvButton(onClick = { allowSaleWithoutStock = false }, modifier = toggleModifier) {
                Text("Bloquear venda sem estoque")
            }
        } else {
            PdvOutlinedButton(onClick = { allowSaleWithoutStock = false }, modifier = toggleModifier) {
                Text("Bloquear venda sem estoque")
            }
        }
        Spacer(Modifier.height(PdvDimens.SpacingSmall))
        if (allowSaleWithoutStock) {
            PdvButton(onClick = { allowSaleWithoutStock = true }, modifier = toggleModifier) {
                Text("Permitir venda sem estoque")
            }
        } else {
            PdvOutlinedButton(onClick = { allowSaleWithoutStock = true }, modifier = toggleModifier) {
                Text("Permitir venda sem estoque")
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))

        PdvButton(
            onClick = {
                val price = priceCents ?: return@PdvButton
                val stock = stockQuantity ?: return@PdvButton
                onSave(
                    name, price, category.ifBlank { null }, stock, minStockAlert,
                    allowSaleWithoutStock, barcode.trim().ifBlank { null }, imageName
                )
            },
            enabled = !uiState.isLoading && name.isNotBlank() && priceCents != null && stockQuantity != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.ButtonHeight)
        ) {
            Text(if (uiState.isLoading) "Salvando..." else "Salvar")
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))
        PdvTextButton(onClick = onCancel) { Text("Cancelar") }

        Spacer(Modifier.height(PdvDimens.SpacingLarge))
    }
}
