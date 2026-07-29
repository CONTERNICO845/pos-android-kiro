package com.example.puntodeventa.ui.tickets

import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.ui.stats.TimeFilter

data class TicketHistoryUiState(
    val orders: List<OrderEntity> = emptyList(),
    val selectedFilter: TimeFilter = TimeFilter.TODAY,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reprintingOrderId: String? = null,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val showDateRangePicker: Boolean = false
)
