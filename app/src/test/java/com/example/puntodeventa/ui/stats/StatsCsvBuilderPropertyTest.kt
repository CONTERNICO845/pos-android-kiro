package com.example.puntodeventa.ui.stats

import com.example.puntodeventa.data.model.PaymentMethod
import com.example.puntodeventa.data.model.ProductSaleSummary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.forAll

/**
 * Feature: statistics-dashboard
 * Property 19: CSV escaping round-trip
 * Property 20: CSV numeric locale independence
 */
class StatsCsvBuilderPropertyTest : StringSpec({

    val numericCell = Regex("""^-?\d+(\.\d+)?$""")

    /** Minimal RFC 4180 reader, enough to verify what the builder writes. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        val body = text.removePrefix(StatsCsvBuilder.BOM)

        while (index < body.length) {
            val char = body[index]
            when {
                inQuotes && char == '"' && index + 1 < body.length && body[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                !inQuotes && char == ',' -> {
                    cells.add(cell.toString())
                    cell.clear()
                }
                !inQuotes && char == '\r' -> Unit
                !inQuotes && char == '\n' -> {
                    cells.add(cell.toString())
                    cell.clear()
                    rows.add(cells)
                    cells = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || cells.isNotEmpty()) {
            cells.add(cell.toString())
            rows.add(cells)
        }
        return rows
    }

    /** Reverses [StatsCsvBuilder.escape]. */
    fun unescape(field: String): String =
        if (field.length >= 2 && field.startsWith("\"") && field.endsWith("\"")) {
            field.substring(1, field.length - 1).replace("\"\"", "\"")
        } else {
            field
        }

    fun sampleState(
        products: List<ProductSaleSummary> = emptyList(),
        revenue: Double = 1_234.56,
        previousRevenue: Double = 1_100.0,
        hasComparison: Boolean = true
    ) = StatsUiState(
        selectedFilter = TimeFilter.TODAY,
        rangeStartMillis = 1_785_000_000_000L,
        rangeEndMillis = 1_785_086_399_999L,
        totalRevenue = revenue,
        orderCount = 42,
        averageTicket = if (revenue > 0) revenue / 42 else 0.0,
        customerCount = 31,
        hasComparison = hasComparison,
        previousRevenue = previousRevenue,
        previousOrderCount = 38,
        previousAverageTicket = if (previousRevenue > 0) previousRevenue / 38 else 0.0,
        previousCustomerCount = 30,
        trendSeries = listOf(
            SalesTrendPoint(1_785_000_000_000L, "14", "14:00 – 14:59", 320.0),
            SalesTrendPoint(1_785_003_600_000L, "15", "15:00 – 15:59", 0.0)
        ),
        trendGranularity = TrendGranularity.HOURLY,
        paymentBreakdown = listOf(
            PaymentSlice(PaymentMethod.CASH, 900.0, 30, 0.729),
            PaymentSlice(PaymentMethod.CARD, 334.56, 12, 0.271)
        ),
        topProducts = products
    )

    "Property 19 — escape/unescape is a round trip for any field" {
        forAll(PropTestConfig(iterations = 300), Arb.string(0..40)) { field ->
            unescape(StatsCsvBuilder.escape(field)) == field
        }
    }

    "Property 19b — fields without separators, quotes or breaks are left untouched" {
        forAll(PropTestConfig(iterations = 200), Arb.string(0..40)) { field ->
            val risky = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
            if (risky) true else StatsCsvBuilder.escape(field) == field
        }
    }

    "Property 20 — every numeric cell is a locale-independent plain decimal" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.double(0.0..500_000.0),
            Arb.double(0.0..500_000.0),
            Arb.list(Arb.string(1..20), 0..5),
            Arb.list(Arb.int(0..500), 0..5),
            Arb.list(Arb.double(0.0..9_999.0), 0..5)
        ) { revenue, previousRevenue, names, quantities, revenues ->
            val products = names.indices.map { index ->
                ProductSaleSummary(
                    productName = names[index],
                    totalQuantity = quantities.getOrElse(index) { 0 },
                    totalRevenue = revenues.getOrElse(index) { 0.0 }
                )
            }
            val csv = StatsCsvBuilder.build(
                sampleState(products, revenue, previousRevenue),
                generatedAtMillis = 1_785_050_000_000L
            )
            val rows = parseCsv(csv)

            fun numericColumnsOf(sectionHeader: String, columns: IntRange) {
                val sectionIndex = rows.indexOfFirst { it.firstOrNull() == sectionHeader }
                (sectionIndex >= 0) shouldBe true
                // Skip the section title and the column header row.
                var cursor = sectionIndex + 2
                while (cursor < rows.size && rows[cursor].size > columns.last) {
                    columns.forEach { column ->
                        val value = rows[cursor][column]
                        // A blank cell is allowed where a baseline is missing.
                        (value.isEmpty() || numericCell.matches(value)) shouldBe true
                    }
                    cursor++
                }
            }

            numericColumnsOf("RESUMEN", 1..3)
            numericColumnsOf("VENTAS POR METODO DE PAGO", 1..3)
            numericColumnsOf("PRODUCTOS MAS VENDIDOS", 1..2)
        }
    }

    "the document carries a BOM and all four sections" {
        val csv = StatsCsvBuilder.build(sampleState(), generatedAtMillis = 1_785_050_000_000L)
        csv.startsWith(StatsCsvBuilder.BOM) shouldBe true
        csv shouldContain "RESUMEN"
        csv shouldContain "VENTAS POR METODO DE PAGO"
        csv shouldContain "TENDENCIA DE VENTAS"
        csv shouldContain "PRODUCTOS MAS VENDIDOS"
    }

    "row counts match the state" {
        val products = listOf(
            ProductSaleSummary("Taco de asada", 64, 1_280.0),
            ProductSaleSummary("Agua de horchata", 20, 300.0)
        )
        val rows = parseCsv(
            StatsCsvBuilder.build(sampleState(products), generatedAtMillis = 1_785_050_000_000L)
        )
        val trendIndex = rows.indexOfFirst { it.firstOrNull() == "TENDENCIA DE VENTAS" }
        val productsIndex = rows.indexOfFirst { it.firstOrNull() == "PRODUCTOS MAS VENDIDOS" }

        // Two trend buckets between the column header and the blank row before the next section.
        (productsIndex - (trendIndex + 2) - 1) shouldBe 2
        (rows.size - (productsIndex + 2)) shouldBe products.size
    }

    "a product name containing a comma stays a single quoted cell" {
        val rows = parseCsv(
            StatsCsvBuilder.build(
                sampleState(listOf(ProductSaleSummary("Taco, con todo", 3, 60.0))),
                generatedAtMillis = 1_785_050_000_000L
            )
        )
        val productRow = rows.last()
        productRow.size shouldBe 3
        productRow[0] shouldBe "Taco, con todo"
        productRow[1] shouldBe "3"
        productRow[2] shouldBe "60.00"
    }

    "without a comparison period the baseline and change cells are blank" {
        val rows = parseCsv(
            StatsCsvBuilder.build(
                sampleState(hasComparison = false),
                generatedAtMillis = 1_785_050_000_000L
            )
        )
        val revenueRow = rows.first { it.firstOrNull() == "Ingresos" }
        revenueRow[2] shouldBe ""
        revenueRow[3] shouldBe ""
    }
})
