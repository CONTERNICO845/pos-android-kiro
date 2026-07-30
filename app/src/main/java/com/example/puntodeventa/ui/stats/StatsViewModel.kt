package com.example.puntodeventa.ui.stats

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.model.OrderTotalPoint
import com.example.puntodeventa.data.model.PaymentMethod
import com.example.puntodeventa.data.model.PaymentMethodRevenue
import com.example.puntodeventa.data.model.PeriodSummary
import com.example.puntodeventa.data.model.ProductSaleSummary
import com.example.puntodeventa.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TimeFilter.TODAY)
    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _showDateRangePicker = MutableStateFlow(false)
    private val _chartMode = MutableStateFlow(ChartMode.BAR)
    private val _transient = MutableStateFlow(TransientState())

    /**
     * Reactive dashboard state. (Req 12.1, 12.2, 16.1)
     *
     * `flatMapLatest` swaps the whole set of Room flows whenever the selection changes, which tears
     * down the collectors of the superseded period — that is the cancellation the requirements ask
     * for, without a manual Job handle. `catch` keeps the last good state and surfaces the failure
     * instead of letting a Room error crash the pipeline. (Req 3.8, 12.4)
     */
    val uiState: StateFlow<StatsUiState> = combine(
        _selectedFilter,
        _customRange,
        _showDateRangePicker,
        _chartMode,
        _transient
    ) { filter, customRange, showPicker, chartMode, transient ->
        Selection(filter, customRange, showPicker, chartMode, transient)
    }.flatMapLatest { selection ->
        val (start, end) = resolveRange(selection)
        val previousRange = computePreviousRange(selection.filter, start, end)

        val currentPeriod = combine(
            orderRepository.getPeriodSummaryFlow(start, end),
            orderRepository.getTopProductsFlow(start, end),
            orderRepository.getRecentOrdersFlow(start, end),
            orderRepository.getPaymentMethodBreakdownFlow(start, end),
            orderRepository.getOrderTotalsFlow(start, end)
        ) { summary, products, orders, breakdown, totals ->
            CurrentPeriodData(summary, products, orders, breakdown, totals)
        }

        val previousSummary = previousRange
            ?.let { (previousStart, previousEnd) ->
                orderRepository.getPeriodSummaryFlow(previousStart, previousEnd)
            }
            ?: flowOf(PeriodSummary.EMPTY)

        combine(currentPeriod, previousSummary) { data, previous ->
            buildState(
                selection = selection,
                start = start,
                end = end,
                hasComparison = previousRange != null,
                data = data,
                previous = previous
            )
        }
    }.catch { throwable ->
        emit(uiState.value.copy(isLoading = false, errorMessage = throwable.message))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    // ── Events ────────────────────────────────────────────────────────────────

    fun onFilterChange(filter: TimeFilter) {
        if (filter == TimeFilter.CUSTOM) {
            _showDateRangePicker.value = true
        } else {
            _customRange.value = null
            _selectedFilter.value = filter
        }
    }

    fun onDateRangeSelected(startMillis: Long, endMillis: Long) {
        val zone = ZoneId.systemDefault()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val adjustedEnd = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        _customRange.value = Pair(startMillis, adjustedEnd)
        _selectedFilter.value = TimeFilter.CUSTOM
        _showDateRangePicker.value = false
    }

    fun onDateRangePickerDismissed() {
        _showDateRangePicker.value = false
    }

    /** Switches the trend chart between bars and a line. (Req 8.8) */
    fun onChartModeChange(mode: ChartMode) {
        _chartMode.value = mode
    }

    /**
     * Writes the CSV report to the Uri returned by the SAF picker. (Req 15.3, 15.8, 15.9)
     *
     * The document is built from the state currently on screen, so the export always matches what
     * the user is looking at.
     */
    fun onExportUriReceived(uri: Uri, contentResolver: ContentResolver) {
        if (_transient.value.isExporting) return
        val snapshot = uiState.value

        viewModelScope.launch {
            _transient.value = _transient.value.copy(isExporting = true, userMessage = null)
            try {
                val csv = StatsCsvBuilder.build(snapshot, System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
                }
                _transient.value = TransientState(
                    isExporting = false,
                    userMessage = "Reporte exportado correctamente"
                )
            } catch (e: Exception) {
                _transient.value = TransientState(
                    isExporting = false,
                    userMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    /** Consumes the transient message so it is shown exactly once. (Req 16.3) */
    fun clearUserMessage() {
        if (_transient.value.userMessage != null) {
            _transient.value = _transient.value.copy(userMessage = null)
        }
    }

    // ── State assembly ────────────────────────────────────────────────────────

    private fun resolveRange(selection: Selection): Pair<Long, Long> =
        if (selection.filter == TimeFilter.CUSTOM && selection.customRange != null) {
            selection.customRange
        } else {
            computeRange(selection.filter)
        }

    private fun buildState(
        selection: Selection,
        start: Long,
        end: Long,
        hasComparison: Boolean,
        data: CurrentPeriodData,
        previous: PeriodSummary
    ): StatsUiState {
        val granularity = SalesTrendCalculator.granularityFor(selection.filter, start, end)
        return StatsUiState(
            selectedFilter = selection.filter,
            rangeStartMillis = start,
            rangeEndMillis = end,
            totalRevenue = data.summary.totalRevenue,
            orderCount = data.summary.orderCount,
            averageTicket = data.summary.averageTicket,
            customerCount = data.summary.customerCount,
            hasComparison = hasComparison,
            previousRevenue = previous.totalRevenue,
            previousOrderCount = previous.orderCount,
            previousAverageTicket = previous.averageTicket,
            previousCustomerCount = previous.customerCount,
            trendSeries = SalesTrendCalculator.buildSeries(granularity, start, end, data.orderTotals),
            trendGranularity = granularity,
            chartMode = selection.chartMode,
            paymentBreakdown = toSlices(data.paymentBreakdown),
            topProducts = data.topProducts,
            recentOrders = data.recentOrders,
            isLoading = false,
            isExporting = selection.transient.isExporting,
            errorMessage = null,
            userMessage = selection.transient.userMessage,
            customStartMillis = selection.customRange?.first,
            customEndMillis = selection.customRange?.second,
            showDateRangePicker = selection.showPicker
        )
    }

    /**
     * Resolves storage tokens to [PaymentMethod] and computes each share of the period revenue.
     *
     * Rows are merged after resolution so unknown/legacy tokens fold into "Efectivo" instead of
     * appearing as separate slices or being dropped. (Req 14.9)
     */
    private fun toSlices(rows: List<PaymentMethodRevenue>): List<PaymentSlice> {
        if (rows.isEmpty()) return emptyList()
        val total = rows.sumOf { it.totalRevenue }
        if (total <= 0.0) return emptyList()

        return rows
            .groupBy { PaymentMethod.fromStorage(it.paymentMethod) }
            .map { (method, methodRows) ->
                val revenue = methodRows.sumOf { it.totalRevenue }
                PaymentSlice(
                    method = method,
                    revenue = revenue,
                    orderCount = methodRows.sumOf { it.orderCount },
                    share = revenue / total
                )
            }
            .sortedWith(compareByDescending<PaymentSlice> { it.revenue }.thenBy { it.method.ordinal })
    }

    // ── Internal carriers ─────────────────────────────────────────────────────

    private data class Selection(
        val filter: TimeFilter,
        val customRange: Pair<Long, Long>?,
        val showPicker: Boolean,
        val chartMode: ChartMode,
        val transient: TransientState
    )

    private data class CurrentPeriodData(
        val summary: PeriodSummary,
        val topProducts: List<ProductSaleSummary>,
        val recentOrders: List<OrderEntity>,
        val paymentBreakdown: List<PaymentMethodRevenue>,
        val orderTotals: List<OrderTotalPoint>
    )

    private data class TransientState(
        val isExporting: Boolean = false,
        val userMessage: String? = null
    )

    companion object {

        fun computeRange(
            filter: TimeFilter,
            now: Long = System.currentTimeMillis()
        ): Pair<Long, Long> {
            val zone = ZoneId.systemDefault()
            val nowInstant = Instant.ofEpochMilli(now)
            val today = nowInstant.atZone(zone).toLocalDate()

            return when (filter) {
                TimeFilter.TODAY -> {
                    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    Pair(start, now)
                }
                TimeFilter.YESTERDAY -> {
                    val yesterday = today.minusDays(1)
                    val start = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = yesterday.atTime(LocalTime.of(23, 59, 59, 999_000_000))
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                    Pair(start, end)
                }
                TimeFilter.THIS_MONTH -> {
                    val firstOfMonth = today.withDayOfMonth(1)
                    val start = firstOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                    Pair(start, now)
                }
                TimeFilter.ALL -> {
                    Pair(0L, now)
                }
                TimeFilter.CUSTOM -> {
                    Pair(0L, now)
                }
            }
        }

        /**
         * Previous equivalent window for the comparison indicators, or `null` when the filter has no
         * baseline. (Req 13.1, 13.2)
         *
         * Day shifts use `java.time` local-date arithmetic rather than subtracting 86,400,000 ms, so
         * a DST boundary compares the same clock window instead of sliding by an hour. The monthly
         * window is clamped so it can never overlap the current one (a 30-day March against a 28-day
         * February would otherwise reach into March).
         */
        fun computePreviousRange(
            filter: TimeFilter,
            start: Long,
            end: Long,
            zone: ZoneId = ZoneId.systemDefault()
        ): Pair<Long, Long>? {
            if (end < start) return null

            return when (filter) {
                TimeFilter.ALL -> null

                TimeFilter.TODAY, TimeFilter.YESTERDAY ->
                    Pair(shiftOneDayBack(start, zone), shiftOneDayBack(end, zone))

                TimeFilter.THIS_MONTH -> {
                    val currentMonthStart = Instant.ofEpochMilli(start).atZone(zone)
                        .toLocalDate()
                        .withDayOfMonth(1)
                    val previousStart = currentMonthStart.minusMonths(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    val previousEnd = (previousStart + (end - start))
                        .coerceAtMost(start - 1)
                        .coerceAtLeast(previousStart)
                    Pair(previousStart, previousEnd)
                }

                TimeFilter.CUSTOM -> {
                    val span = end - start
                    val previousEnd = start - 1
                    Pair((previousEnd - span).coerceAtLeast(0L), previousEnd.coerceAtLeast(0L))
                }
            }
        }

        /** Suggested SAF file name: `reporte_ventas_yyyyMMdd_HHmmss.csv`. (Req 15.2) */
        fun exportFileName(now: Long = System.currentTimeMillis()): String {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
            return "reporte_ventas_$stamp.csv"
        }

        private fun shiftOneDayBack(millis: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(millis).atZone(zone).minusDays(1).toInstant().toEpochMilli()
    }

    class Factory(
        private val orderRepository: OrderRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(orderRepository) as T
    }
}
