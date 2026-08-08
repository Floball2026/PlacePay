package com.example.pdvmaquineta.experiment

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.dhatim.fastexcel.Workbook
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Experimento isolado da Fase 7 (não toca em nenhuma tela/lógica do app):
// confirma que a lib org.dhatim:fastexcel consegue gerar um .xlsx de verdade
// rodando no dispositivo físico, sem depender de java.awt (problema
// conhecido do Apache POI no Android). Salva em getExternalFilesDir(null)
// pra não precisar de permissão de armazenamento em nenhuma API level, e pra
// ficar acessível via `adb pull` sem root.
@RunWith(AndroidJUnit4::class)
class FastExcelExperimentTest {

    @Test
    fun generatesXlsxFileWithSampleRowsAndColumns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDir = context.getExternalFilesDir(null)!!
        val outputFile = File(outputDir, "fastexcel-experiment.xlsx")
        if (outputFile.exists()) outputFile.delete()

        FileOutputStream(outputFile).use { os ->
            val workbook = Workbook(os, "PDVMaquineta-Experimento", "1.0")
            val worksheet = workbook.newWorksheet("Vendas")

            worksheet.value(0, 0, "Produto")
            worksheet.value(0, 1, "Quantidade")
            worksheet.value(0, 2, "Total (R$)")

            worksheet.value(1, 0, "Café")
            worksheet.value(1, 1, 10)
            worksheet.value(1, 2, 50.0)

            worksheet.value(2, 0, "Pão de queijo")
            worksheet.value(2, 1, 25)
            worksheet.value(2, 2, 87.5)

            worksheet.value(3, 0, "Suco natural")
            worksheet.value(3, 1, 7)
            worksheet.value(3, 2, 42.0)

            workbook.finish()
        }

        assertTrue("Arquivo .xlsx deveria ter sido criado em ${outputFile.absolutePath}", outputFile.exists())
        assertTrue("Arquivo .xlsx não deveria estar vazio", outputFile.length() > 0)
    }
}
