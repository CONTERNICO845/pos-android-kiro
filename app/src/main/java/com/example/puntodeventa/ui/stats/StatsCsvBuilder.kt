package com.example.puntodeventa.ui.stats

import java.util.Locale

/**
 * Serializes the current dashboard into an RFC 4180 CSV document. (Req 15.4 – 15.7)
 *
 * Pure Kotlin: no Android types and no I/O, so the document layout, the quoting and the number
 * formatting are all covered by JVM property tests. The ViewModel only writes the returned string.
 */
object StatsCsvBuilder {

    /**
     * UTF-8 byte-order mark. Prepended so spreadsheet applications detect the encoding and render
     * accented Spanish text correctly. (Req 15.4)
     */
    const val BOM = "\uFEFF"

    private const val SEPARATOR = ","

    /** Builds the report for [state] as of [generatedAtMillis], BOM included. */
    fun build(state: StatsUiState, generatedAtMillis: Long): String {
        val sb = StringBuilder()
        sb.append(BOM)

        // ── Header ────────────────────────────────────────────────────────────
        sb.appendRow("Reporte de ventas")
        sb.appendRow("Periodo", state.selectedFilter.label)
        sb.appendRow("Rango", StatsFormatters.formatRangeLabel(state.rangeStartMillis, state.rangeEndMillis))
        sb.appendRow("Generado", StatsFormatters.formatDateTime(generatedAtMillis))
        sb.appendLine()

        // ── Summary with period-over-period comparison ────────────────────────
        sb.appendRow("RESUMEN")
        sb.appendRow("Metrica", "Actual", "Periodo anterior", "Cambio %")
        sb.appendRow(
            "Ingresos",
            money(state.totalRevenue),
            comparisonCell(state.hasComparison, money(state.previousRevenue)),
            percentCell(state.revenueDelta)
        )
        sb.appendRow(
            "Ordenes",
            state.orderCount.toString(),
            comparisonCell(state.hasComparison, state.previousOrderCount.toString()),
            percentCell(state.orderCountDelta)
        )
        sb.appendRow(
            "Ticket promedio",
            money(state.averageTicket),
            comparisonCell(state.hasComparison, money(state.previousAverageTicket)),
            percentCell(state.averageTicketDelta)
        )
        sb.appendRow(
            "Clientes",
            state.customerCount.toString(),
            comparisonCell(state.hasComparison, state.previousCustomerCount.toString()),
            percentCell(state.customerCountDelta)
        )
        sb.appendLine()

        // ── Revenue by tender type ────────────────────────────────────────────
        sb.appendRow("VENTAS POR METODO DE PAGO")
        sb.appendRow("Metodo", "Ingresos", "Ordenes", "Participacion %")
        state.paymentBreakdown.forEach { slice ->
            sb.appendRow(
                slice.method.displayName,
                money(slice.revenue),
                slice.orderCount.toString(),
                percent(slice.share * 100.0)
            )
        }
        sb.appendLine()

        // ── Trend series ──────────────────────────────────────────────────────
        sb.appendRow("TENDENCIA DE VENTAS")
        sb.appendRow("Granularidad", "Periodo", "Ingresos")
        state.trendSeries.forEach { point ->
            sb.appendRow(
                state.trendGranularity.csvLabel,
                point.fullLabel,
                money(point.revenue)
            )
        }
        sb.appendLine()

        // ── Top products ──────────────────────────────────────────────────────
        sb.appendRow("PRODUCTOS MAS VENDIDOS")
        sb.appendRow("Producto", "Cantidad", "Ingresos")
        state.topProducts.forEach { product ->
            sb.appendRow(
                product.productName,
                product.totalQuantity.toString(),
                money(product.totalRevenue)
            )
        }

        return sb.toString()
    }

    /**
     * RFC 4180 field quoting: wraps the value in double quotes and doubles inner quotes when it
     * contains a separator, a quote or a line break. Everything else is returned unchanged.
     * (Req 15.6)
     */
    fun escape(field: String): String {
        val needsQuoting = field.contains(SEPARATOR) ||
            field.contains('"') ||
            field.contains('\n') ||
            field.contains('\r')
        return if (needsQuoting) "\"${field.replace("\"", "\"\"")}\"" else field
    }

    /** Money as a plain decimal: no currency symbol, no grouping, always a period. (Req 15.7) */
    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

    /** Percentage as a plain decimal with one place and no sign decoration. (Req 15.7) */
    private fun percent(value: Double): String = String.format(Locale.US, "%.1f", value)

    /** Blank cell when the selected period has no baseline, instead of a misleading zero. */
    private fun comparisonCell(hasComparison: Boolean, value: String): String =
        if (hasComparison) value else ""

    /** Blank cell for an unavailable or undefined change, never "null" or "Infinity". */
    private fun percentCell(delta: MetricDelta): String =
        if (!delta.available || delta.percent == null) "" else percent(delta.percent)

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(SEPARATOR) { escape(it) })
        append("\r\n")
    }

    private fun StringBuilder.appendLine() {
        append("\r\n")
    }
}
