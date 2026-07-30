package com.example.puntodeventa.ui.stats

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility object for formatting statistics values displayed on the Stats Dashboard.
 */
object StatsFormatters {

    private val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    private val countFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US).apply {
        isGroupingUsed = true
    }

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Formats a monetary amount with "$" prefix, comma thousands separator,
     * and exactly 2 decimal places.
     * Examples: "$1,234.56", "$0.00"
     */
    fun formatCurrency(amount: Double): String {
        return "$${currencyFormat.format(amount)}"
    }

    /**
     * Formats an integer count with locale-aware thousand separators, no decimal.
     * Examples: "1,234", "0"
     */
    fun formatCount(count: Int): String {
        return countFormat.format(count)
    }

    /**
     * Formats a timestamp as "HH:mm" in the device's local timezone.
     */
    fun formatOrderTime(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return timeFormatter.format(localTime)
    }

    /**
     * Formats a quantity sold as "{quantity} vendidos".
     */
    fun formatQuantitySold(quantity: Int): String {
        return "$quantity vendidos"
    }

    /**
     * Returns "Cliente anónimo" if customerName is null or blank,
     * otherwise returns the original name.
     */
    fun displayCustomerName(customerName: String?): String {
        return if (customerName.isNullOrBlank()) "Cliente anónimo" else customerName
    }

    // ── Enterprise dashboard (v2) ─────────────────────────────────────────────

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /**
     * Compact currency for chart axis labels, where horizontal space is scarce. (Req 8.6)
     * Examples: "$0", "$950", "$1.2k", "$3.4M"
     */
    fun formatCompactCurrency(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs < 1_000.0 -> "$sign$${String.format(Locale.US, "%.0f", abs)}"
            abs < 1_000_000.0 -> "$sign$${String.format(Locale.US, "%.1f", abs / 1_000.0)}k"
            else -> "$sign$${String.format(Locale.US, "%.1f", abs / 1_000_000.0)}M"
        }
    }

    /**
     * Signed percentage change with one decimal place. (Req 13.5, 13.6, 13.7)
     * A value that rounds to zero renders as the neutral "0.0%" without a sign.
     * Examples: "+5.0%", "-2.3%", "0.0%"
     */
    fun formatPercent(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return when {
            rounded > 0.0 -> String.format(Locale.US, "+%.1f%%", rounded)
            rounded < 0.0 -> String.format(Locale.US, "%.1f%%", rounded)
            else -> "0.0%"
        }
    }

    /**
     * Unsigned share of a whole, one decimal place. (Req 14.6)
     * Example: "42.5%"
     */
    fun formatShare(value: Double): String =
        String.format(Locale.US, "%.1f%%", value)

    /** "dd/MM/yyyy" in the device time zone. */
    fun formatDate(timestamp: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

    /** "dd/MM/yyyy HH:mm" in the device time zone — used by the CSV header. */
    fun formatDateTime(timestamp: Long): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

    /** Human-readable range caption, e.g. "01/07/2026 00:00 – 30/07/2026 23:59". (Req 15.5) */
    fun formatRangeLabel(startMillis: Long, endMillis: Long): String =
        "${formatDateTime(startMillis)} – ${formatDateTime(endMillis)}"
}
