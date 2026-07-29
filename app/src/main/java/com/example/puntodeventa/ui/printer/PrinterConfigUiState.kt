package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.model.PrinterConfig

/** Immutable source of truth for the multi-printer configuration UI. */
data class PrinterConfigUiState(
    val printers: List<PrinterConfig> = emptyList(),
    val draft: PrinterConfig = newPrinterDraft(),
    val selectedPrinterId: String? = null,
    val isAdding: Boolean = false,
    val isDiscovering: Boolean = false,
    val discoveredIps: List<String> = emptyList(),
    val portInput: String = draft.port.toString(),
    // Legacy field kept in lockstep with draft.ipAddress by PrinterConfigViewModel.
    val ipAddress: String = draft.ipAddress,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val lastTestResult: TestResult? = null
)

/** Defaults used whenever the user starts adding a printer. */
fun newPrinterDraft(id: String = ""): PrinterConfig = PrinterConfig(
    id = id,
    name = "Nueva impresora",
    ipAddress = "",
    port = 9100,
    paperSize = 80,
    autoCut = true,
    protocol = "ESC/POS",
    isActive = true
)

enum class ConnectionStatus {
    Connected,
    Disconnected,
    Testing,
    Error
}

data class TestResult(
    val success: Boolean,
    val timestamp: Long,
    val message: String
)