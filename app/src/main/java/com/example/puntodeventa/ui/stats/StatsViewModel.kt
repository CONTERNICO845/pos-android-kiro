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

    val uiState: StateFlow<StatsUiState> = _selectedFilter
        .flatMapLatest { filter ->
            val (start, end) = computeRange(filter)

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
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )

    fun onFilterChange(filter: TimeFilter) {
        _selectedFilter.value = filter
    }

    companion object {
        /**
         * Computes the start and end timestamps for the given [filter] based on
         * the device's default timezone.
         *
         * @param filter The time filter to compute the range for.
         * @param now An optional "current time" in epoch millis, defaults to
         *            [System.currentTimeMillis]. Exposed for testability.
         * @return A [Pair] of (startMillis, endMillis) inclusive.
         */
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
