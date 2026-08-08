package com.example.pdvmaquineta.presentation.product

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Armazenamento local (offline) das fotos de produto.
 *
 * As imagens ficam em filesDir/product_images/ e o banco guarda apenas o NOME
 * do arquivo (campo Product.imagePath). Esse mesmo esquema serve para a futura
 * interface externa de sincronizacao: basta gravar o arquivo nesta pasta e
 * salvar o nome no campo.
 */
object ProductImageStore {
    private const val DIR = "product_images"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun fileFor(context: Context, name: String): File = File(dir(context), name)

    /** Copia a imagem escolhida (galeria) para o armazenamento interno e
     *  retorna o nome do arquivo gerado. */
    fun importFromUri(context: Context, uri: Uri): String {
        val name = "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        val target = fileFor(context, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Nao foi possivel ler a imagem selecionada")
        return name
    }
}
