package com.example.puntodeventa.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TimeFilter.TODAY)
    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _showDateRangePicker = MutableStateFlow(false)

    val uiState: StateFlow<StatsUiState> = combine(
        _selectedFilter,
        _customRange,
        _showDateRangePicker
    ) { filter, customRange, showPicker ->
        Triple(filter, customRange, showPicker)
    }.flatMapLatest { (filter, customRange, showPicker) ->
        val (start, end) = if (filter == TimeFilter.CUSTOM && customRange != null) {
            customRange
        } else {
            computeRange(filter)
        }

        combine(
            orderRepository.getTotalRevenueFlow(start, end),
            orderRepository.getOrderCountFlow(start, end),
            orderRepository.getCustomerCountFlow(start, end),
            orderRepository.getTopProductsFlow(start, end),
            orderRepository.getRecentOrdersFlow(start, end)
        ) { revenue, count, customers, products, orders ->
            StatsUiState(
                selectedFilter = filter,
                totalRevenue = revenue,
                orderCount = count,
                averageTicket = if (count > 0) revenue / count else 0.0,
                customerCount = customers,
                topProducts = products,
                recentOrders = orders,
                isLoading = false,
                customStartMillis = customRange?.first,
                customEndMillis = customRange?.second,
                showDateRangePicker = showPicker
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

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
    }

    class Factory(
        private val orderRepository: OrderRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(orderRepository) as T
    }
}
