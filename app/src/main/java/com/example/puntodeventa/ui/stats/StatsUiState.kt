package com.example.puntodeventa.ui.stats

import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.model.ProductSaleSummary

/**
 * UI state holder for the Statistics Dashboard screen.
 * Emitted by StatsViewModel and observed by StatsScreen.
 */
data class StatsUiState(
    val selectedFilter: TimeFilter = TimeFilter.TODAY,
    val totalRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val averageTicket: Double = 0.0,
    val customerCount: Int = 0,
    val topProducts: List<ProductSaleSummary> = emptyList(),
    val recentOrders: List<OrderEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val showDateRangePicker: Boolean = false
)
