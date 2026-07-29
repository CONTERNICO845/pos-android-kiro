package com.example.puntodeventa.ui.tickets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import com.example.puntodeventa.ui.stats.StatsViewModel
import com.example.puntodeventa.ui.stats.TimeFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

class TicketHistoryViewModel(
    private val orderRepository: OrderRepository,
    private val printerPreferencesRepository: PrinterPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketHistoryUiState())
    val uiState: StateFlow<TicketHistoryUiState> = _uiState.asStateFlow()

    private var queryJob: Job? = null

    init {
        loadOrders(TimeFilter.TODAY)
    }

    fun onFilterChange(filter: TimeFilter) {
        if (filter == TimeFilter.CUSTOM) {
            _uiState.update { it.copy(showDateRangePicker = true) }
        } else {
            _uiState.update {
                it.copy(
                    selectedFilter = filter,
                    customStartMillis = null,
                    customEndMillis = null
                )
            }
            loadOrders(filter)
        }
    }

    fun onDateRangeSelected(startMillis: Long, endMillis: Long) {
        val zone = ZoneId.systemDefault()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val adjustedEnd = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        _uiState.update {
            it.copy(
                selectedFilter = TimeFilter.CUSTOM,
                customStartMillis = startMillis,
                customEndMillis = adjustedEnd,
                showDateRangePicker = false
            )
        }
        loadOrdersWithRange(startMillis, adjustedEnd)
    }

    fun onDateRangePickerDismissed() {
        _uiState.update { it.copy(showDateRangePicker = false) }
    }

    fun onReprintTicket(order: OrderEntity) {
        val ipAddress = printerPreferencesRepository.getIpAddress()
        if (ipAddress.isBlank()) {
            Log.w(TAG, "Reprint requested but printer IP is empty")
            return
        }
        val ticketText = order.clientTicketText
        if (ticketText.isNullOrBlank()) {
            Log.w(TAG, "Reprint requested but clientTicketText is null/blank for order ${order.id}")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(reprintingOrderId = order.id) }
            try {
                EscPosPrinterLan.printTicket(ipAddress, ticketText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(reprintingOrderId = null) }
            }
        }
    }

    private fun loadOrders(filter: TimeFilter) {
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (start, end) = StatsViewModel.computeRange(filter)
                val orders = orderRepository.getOrdersByTimeRange(start, end)
                _uiState.update { it.copy(orders = orders, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun loadOrdersWithRange(start: Long, end: Long) {
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val orders = orderRepository.getOrdersByTimeRange(start, end)
                _uiState.update { it.copy(orders = orders, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    class Factory(
        private val orderRepository: OrderRepository,
        private val printerPreferencesRepository: PrinterPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TicketHistoryViewModel(orderRepository, printerPreferencesRepository) as T
    }

    companion object {
        private const val TAG = "TicketHistoryVM"
    }
}
