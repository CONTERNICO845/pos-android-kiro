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
}
