package com.example.pdvmaquineta.data.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pdvmaquineta.domain.export.ExportResult
import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.model.CashSessionStatus
import com.example.pdvmaquineta.domain.model.ExportFormat
import com.example.pdvmaquineta.domain.model.Product
import com.example.pdvmaquineta.domain.model.TopProduct
import com.example.pdvmaquineta.domain.payment.PaymentMethod
import com.example.pdvmaquineta.domain.usecase.CashReportSection
import com.example.pdvmaquineta.domain.usecase.ProductsReportSection
import com.example.pdvmaquineta.domain.usecase.ReportData
import com.example.pdvmaquineta.domain.usecase.SalesReportSection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Smoke test isolado (Fase 7c): confirma que os três formatos (CSV/PDF/
// Excel) são gerados de verdade no dispositivo físico, sem crash — cobre
// android.graphics.pdf.PdfDocument, MediaStore/Downloads e
// org.dhatim:fastexcel, nenhum dos quais exercitado só pela compilação.
@RunWith(AndroidJUnit4::class)
class AndroidReportExporterTest {

    private fun sampleReport(): ReportData = ReportData(
        fromMillis = 1_000L,
        toMillis = 2_000L,
        maxRangeDays = null,
        sales = SalesReportSection(
            totalSoldCents = 15_000L,
            completedSalesCount = 3,
            totalsByMethod = mapOf(PaymentMethod.CASH to 10_000L, PaymentMethod.PIX to 5_000L),
            topProducts = listOf(
                TopProduct(productId = 1, productName = "Café", totalQuantity = 10, totalRevenueCents = 5_000L)
            )
        ),
        cash = CashReportSection(
            closedSessionsCount = 1,
            totalOpeningCents = 1_000L,
            totalInformedCents = 16_000L,
            totalWithdrawalCents = 500L,
            totalSupplyCents = 200L,
            divergentSessions = listOf(
                CashSession(
                    id = 1, operatorId = 1, operatorUsername = "admin",
                    openingBalanceCents = 1_000L, openedAt = 500L, closedAt = 1_500L,
                    expectedCashCents = 15_000L, informedCashCents = 16_000L,
                    divergenceCents = 1_000L, status = CashSessionStatus.CLOSED
                )
            )
        ),
        products = ProductsReportSection(
            lowStockProducts = listOf(
                Product(
                    id = 1, name = "Café", priceCents = 500L, category = "Bebidas", active = true,
                    stockQuantity = 2, minStockAlert = 5, allowSaleWithoutStock = false,
                    barcode = null, createdAt = 100L, updatedAt = 100L
                )
            ),
            topProducts = listOf(
                TopProduct(productId = 1, productName = "Café", totalQuantity = 10, totalRevenueCents = 5_000L)
            )
        )
    )

    @Test
    fun exportsAllThreeFormatsWithoutCrashing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val exporter = AndroidReportExporter(context)
        val report = sampleReport()

        runBlocking {
            ExportFormat.entries.forEach { format ->
                val result = exporter.export(report, format)
                assertTrue(
                    "Exportação de $format deveria ter sucesso, mas foi: $result",
                    result is ExportResult.Success
                )
                val success = result as ExportResult.Success
                assertTrue("Nome de arquivo vazio para $format", success.fileName.isNotBlank())
            }
        }
    }
}
