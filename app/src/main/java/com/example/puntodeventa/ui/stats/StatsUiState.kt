package com.example.puntodeventa.ui.stats

import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.model.ProductSaleSummary

/**
 * UI state holder for the Statistics Dashboard screen.
 * Emitted by StatsViewModel and observed by StatsScreen.
 *
 * The four `...Delta` values are derived rather than stored, so the state can never hold a comparison
 * indicator that disagrees with its own metrics. (Req 12.1, 13.4)
 */
data class StatsUiState(
    val selectedFilter: TimeFilter = TimeFilter.TODAY,

    // ── Resolved query range ─────────────────────────────────────────────────
    val rangeStartMillis: Long = 0L,
    val rangeEndMillis: Long = 0L,

    // ── Selected period ──────────────────────────────────────────────────────
    val totalRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val averageTicket: Double = 0.0,
    val customerCount: Int = 0,

    // ── Previous equivalent period (absent for the "Todo" filter) ────────────
    val hasComparison: Boolean = false,
    val previousRevenue: Double = 0.0,
    val previousOrderCount: Int = 0,
    val previousAverageTicket: Double = 0.0,
    val previousCustomerCount: Int = 0,

    // ── Sales trend ──────────────────────────────────────────────────────────
    val trendSeries: List<SalesTrendPoint> = emptyList(),
    val trendGranularity: TrendGranularity = TrendGranularity.HOURLY,
    val chartMode: ChartMode = ChartMode.BAR,

    // ── Payment breakdown ────────────────────────────────────────────────────
    val paymentBreakdown: List<PaymentSlice> = emptyList(),

    // ── Lists ────────────────────────────────────────────────────────────────
    val topProducts: List<ProductSaleSummary> = emptyList(),
    val recentOrders: List<OrderEntity> = emptyList(),

    // ── Flags and transient messages ─────────────────────────────────────────
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,

    // ── Custom range picker ──────────────────────────────────────────────────
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val showDateRangePicker: Boolean = false
) {
    val revenueDelta: MetricDelta
        get() = MetricDelta.of(totalRevenue, previousRevenue, hasComparison)

    val orderCountDelta: MetricDelta
        get() = MetricDelta.of(orderCount, previousOrderCount, hasComparison)

    val averageTicketDelta: MetricDelta
        get() = MetricDelta.of(averageTicket, previousAverageTicket, hasComparison)

    val customerCountDelta: MetricDelta
        get() = MetricDelta.of(customerCount, previousCustomerCount, hasComparison)
}
