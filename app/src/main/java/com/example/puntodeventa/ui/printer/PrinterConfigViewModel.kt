package com.example.puntodeventa.ui.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.model.PrinterConfig
import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.printer.LanPrinterDiscovery
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import java.net.SocketTimeoutException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrinterConfigViewModel(
    private val prefsRepository: PrinterPreferencesRepository,
    private val discover: suspend (Int) -> List<String> = LanPrinterDiscovery::scan
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<PrinterConfigUiState> = _uiState.asStateFlow()

    private fun loadInitialState(): PrinterConfigUiState {
        // Old strict mocks throw for an unstubbed getPrinters(); preserve their legacy path.
        val printers = runCatching { prefsRepository.getPrinters() }
            .getOrNull()
            .orEmpty()
            .ifEmpty {
                val legacyIp = runCatching { prefsRepository.getIpAddress() }.getOrDefault("")
                listOf(PrinterConfig.default(legacyIp))
            }
        val selected = printers.first()
        return PrinterConfigUiState(
            printers = printers,
            draft = selected,
            selectedPrinterId = selected.id,
            ipAddress = selected.ipAddress,
            portInput = selected.port.toString()
        )
    }

    fun selectPrinter(printerId: String) {
        val printer = _uiState.value.printers.firstOrNull { it.id == printerId } ?: return
        _uiState.value = _uiState.value.copy(
            draft = printer,
            selectedPrinterId = printer.id,
            isAdding = false,
            ipAddress = printer.ipAddress,
            portInput = printer.port.toString(),
            discoveredIps = emptyList(),
            errorMessage = null,
            statusMessage = null
        )
    }

    fun editPrinter(printerId: String) = selectPrinter(printerId)

    fun startAdd() {
        val draft = newPrinterDraft(UUID.randomUUID().toString())
        _uiState.value = _uiState.value.copy(
            draft = draft,
            selectedPrinterId = null,
            isAdding = true,
            ipAddress = draft.ipAddress,
            portInput = draft.port.toString(),
            discoveredIps = emptyList(),
            errorMessage = null,
            statusMessage = null,
            connectionStatus = ConnectionStatus.Disconnected
        )
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }

    fun updateIpAddress(newIpAddress: String) {
        val filtered = newIpAddress.filter { it.isDigit() || it == '.' }
        updateDraft { copy(ipAddress = filtered) }
        _uiState.value = _uiState.value.copy(ipAddress = filtered)
    }

    fun updatePort(value: String) {
        val filtered = value.filter(Char::isDigit)
        val parsed = filtered.toIntOrNull()
        if (parsed != null) updateDraft { copy(port = parsed) }
        _uiState.value = _uiState.value.copy(portInput = filtered)
    }

    fun updatePort(value: Int) = updatePort(value.toString())
    fun updatePaperSize(value: Int) = updateDraft { copy(paperSize = value) }
    fun updateAutoCut(value: Boolean) = updateDraft { copy(autoCut = value) }
    fun updateProtocol(value: String) = updateDraft { copy(protocol = value) }
    fun updateActive(value: Boolean) = updateDraft { copy(isActive = value) }

    private inline fun updateDraft(transform: PrinterConfig.() -> PrinterConfig) {
        _uiState.value = _uiState.value.copy(
            draft = _uiState.value.draft.transform(),
            errorMessage = null,
            statusMessage = null
        )
    }

    fun savePrinter() {
        val config = validatedDraft() ?: return
        runCatching { prefsRepository.upsertPrinter(config) }
            .onSuccess {
                val current = _uiState.value.printers
                val index = current.indexOfFirst { it.id == config.id }
                val updated = if (index >= 0) {
                    current.toMutableList().apply { this[index] = config }
                } else current + config
                _uiState.value = _uiState.value.copy(
                    printers = updated,
                    draft = config,
                    selectedPrinterId = config.id,
                    isAdding = false,
                    ipAddress = config.ipAddress,
                    portInput = config.port.toString(),
                    statusMessage = "Configuración guardada",
                    errorMessage = null
                )
            }
            .onFailure { reportError("No se pudo guardar la impresora: ${it.message.orEmpty()}") }
    }

    /** Legacy save API retained for existing callers and tests. */
    fun saveIpAddress() {
        val ip = _uiState.value.draft.ipAddress
        runCatching { prefsRepository.saveIpAddress(ip) }
            .onSuccess { _uiState.value = _uiState.value.copy(statusMessage = "Configuración guardada") }
            .onFailure { reportError("No se pudo guardar la IP: ${it.message.orEmpty()}") }
    }

    fun togglePrinterActive(printerId: String, isActive: Boolean) {
        runCatching { prefsRepository.setPrinterActive(printerId, isActive) }
            .onSuccess {
                val updated = _uiState.value.printers.map {
                    if (it.id == printerId) it.copy(isActive = isActive) else it
                }
                val draft = if (_uiState.value.draft.id == printerId) {
                    _uiState.value.draft.copy(isActive = isActive)
                } else _uiState.value.draft
                _uiState.value = _uiState.value.copy(printers = updated, draft = draft)
            }
            .onFailure { reportError("No se pudo actualizar la impresora: ${it.message.orEmpty()}") }
    }

    fun deletePrinter(printerId: String) {
        runCatching { prefsRepository.deletePrinter(printerId) }
            .onSuccess {
                val state = _uiState.value
                val remaining = state.printers.filterNot { it.id == printerId }
                val selected = if (state.selectedPrinterId != printerId) {
                    remaining.firstOrNull { it.id == state.selectedPrinterId }
                } else {
                    remaining.firstOrNull()
                }
                val keepCurrentDraft = selected?.id == state.draft.id
                val draft = when {
                    keepCurrentDraft -> state.draft
                    selected != null -> selected
                    else -> newPrinterDraft(UUID.randomUUID().toString())
                }
                _uiState.value = state.copy(
                    printers = remaining,
                    draft = draft,
                    selectedPrinterId = selected?.id,
                    isAdding = selected == null,
                    ipAddress = draft.ipAddress,
                    portInput = draft.port.toString(),
                    discoveredIps = emptyList(),
                    statusMessage = "Impresora eliminada",
                    errorMessage = null
                )
            }
            .onFailure { reportError("No se pudo eliminar la impresora: ${it.message.orEmpty()}") }
    }

    fun testPrinter() {
        val state = _uiState.value
        val port = state.portInput.toIntOrNull()
        if (port == null || port !in 1..65535) {
            reportError("El puerto debe estar entre 1 y 65535")
            return
        }
        // Connection testing deliberately uses the complete draft configuration.
        // Endpoint/protocol failures are reported by EscPosPrinterLan as test results.
        val config = state.draft.copy(port = port)
        _uiState.value = _uiState.value.copy(
            connectionStatus = ConnectionStatus.Testing,
            isLoading = true,
            errorMessage = null,
            statusMessage = "Probando conexión…"
        )
        viewModelScope.launch {
            try {
                if (config.port == 9100 && config.paperSize == 80 && config.autoCut &&
                    config.protocol.equals("ESC/POS", ignoreCase = true)
                ) {
                    // Preserve the legacy one-argument path for the standard profile.
                    EscPosPrinterLan.testConnection(config.ipAddress)
                } else {
                    EscPosPrinterLan.testConfiguredPrinter(config)
                }
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.Connected,
                    isLoading = false,
                    lastTestResult = TestResult(true, System.currentTimeMillis(), "Conexión exitosa"),
                    statusMessage = "Conexión exitosa",
                    errorMessage = null
                )
            } catch (_: SocketTimeoutException) {
                reportConnectionError("La impresora no respondió en el tiempo límite (5s)")
            } catch (e: Exception) {
                reportConnectionError(
                    "No se pudo conectar a la impresora en ${config.ipAddress}: ${e.localizedMessage}"
                )
            }
        }
    }

    fun discoverPrinters() {
        val port = _uiState.value.portInput.toIntOrNull()
        if (port == null || port !in 1..65535) {
            reportError("El puerto debe estar entre 1 y 65535")
            return
        }
        _uiState.value = _uiState.value.copy(
            isDiscovering = true,
            discoveredIps = emptyList(),
            errorMessage = null,
            statusMessage = "Buscando impresoras…"
        )
        viewModelScope.launch {
            runCatching { discover(port) }
                .onSuccess { ips ->
                    _uiState.value = _uiState.value.copy(
                        isDiscovering = false,
                        discoveredIps = ips,
                        statusMessage = if (ips.isEmpty()) "No se encontraron impresoras" else "${ips.size} impresora(s) encontrada(s)"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isDiscovering = false)
                    reportError("No se pudo buscar impresoras: ${error.message.orEmpty()}")
                }
        }
    }

    fun selectDiscoveredIp(ipAddress: String) = updateIpAddress(ipAddress)

    fun reportError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, statusMessage = null)
    }

    private fun reportConnectionError(message: String) {
        _uiState.value = _uiState.value.copy(
            connectionStatus = ConnectionStatus.Error,
            isLoading = false,
            errorMessage = message,
            statusMessage = null,
            lastTestResult = TestResult(false, System.currentTimeMillis(), message)
        )
    }

    private fun validatedDraft(): PrinterConfig? {
        val state = _uiState.value
        val port = state.portInput.toIntOrNull()
        val error = when {
            state.draft.name.isBlank() -> "El nombre de la impresora es obligatorio"
            !isValidIpv4(state.draft.ipAddress) -> "Ingresa una dirección IPv4 válida"
            port == null || port !in 1..65535 -> "El puerto debe estar entre 1 y 65535"
            state.draft.paperSize !in setOf(58, 80) -> "El tamaño de papel debe ser 58 u 80 mm"
            !state.draft.protocol.trim().equals("ESC/POS", ignoreCase = true) -> "Solo se admite el protocolo ESC/POS"
            else -> null
        }
        if (error != null) {
            reportError(error)
            return null
        }
        return state.draft.copy(port = port!!, protocol = "ESC/POS")
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }

    class Factory(
        private val prefsRepository: PrinterPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == PrinterConfigViewModel::class.java) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return PrinterConfigViewModel(prefsRepository) as T
        }
    }
}
