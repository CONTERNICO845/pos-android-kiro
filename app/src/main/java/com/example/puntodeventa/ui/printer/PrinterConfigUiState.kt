package com.example.puntodeventa.ui.printer

/**
 * UI state data class for printer configuration screen
 * Contains all state needed for printer IP configuration and testing
 */
data class PrinterConfigUiState(
    val ipAddress: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val lastTestResult: TestResult? = null
)

/**
 * Represents the current connection status of the printer
 */
enum class ConnectionStatus {
    Connected,
    Disconnected, 
    Testing,
    Error
}

/**
 * Data class representing the result of a printer test operation
 */
data class TestResult(
    val success: Boolean,
    val timestamp: Long,
    val message: String
)