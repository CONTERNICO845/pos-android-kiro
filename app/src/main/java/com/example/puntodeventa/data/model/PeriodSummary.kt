package com.example.puntodeventa.data.model

/**
 * Room query projection summarizing one time window. (Req 2.8)
 *
 * Queried twice per dashboard emission: once for the selected period and once for the previous
 * equivalent period that feeds the metric-card comparison indicators.
 */
data class PeriodSummary(
    val totalRevenue: Double,
    val orderCount: Int,
    val customerCount: Int
) {
    val averageTicket: Double
        get() = if (orderCount > 0) totalRevenue / orderCount else 0.0

    companion object {
        /** Neutral baseline used when a filter has no comparison period (e.g. "Todo"). */
        val EMPTY = PeriodSummary(totalRevenue = 0.0, orderCount = 0, customerCount = 0)
    }
}
