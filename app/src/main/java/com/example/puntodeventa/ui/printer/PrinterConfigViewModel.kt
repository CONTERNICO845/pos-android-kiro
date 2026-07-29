package com.example.puntodeventa.ui.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

/**
 * ViewModel for the printer configuration screen.
 *
 * Manages UI state for printer IP address configuration and persists the IP
 * address to local storage via [PrinterPreferencesRepository].
 *
 * The saved IP address is loaded immediately on construction so the field
 * is pre-populated when the screen opens.
 *
 * Requirements: 10.1, 10.2, 10.4, 10.5, 10.6, 10.7
 */
class PrinterConfigViewModel(
    private val prefsRepository: PrinterPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PrinterConfigUiState(ipAddress = prefsRepository.getIpAddress())
    )
    val uiState: StateFlow<PrinterConfigUiState> = _uiState.asStateFlow()

    /**
     * Updates the IP address in the UI state.
     * @param newIpAddress The new IP address string entered by the user.
     */
    fun updateIpAddress(newIpAddress: String) {
        _uiState.value = _uiState.value.copy(ipAddress = newIpAddress)
    }

    /**
     * Tests the connection to the thermal printer by sending a test print
     * via ESC/POS over TCP. Updates UI state through the entire lifecycle:
     * Testing → Connected (success) or Error (failure).
     */
    fun testPrinter() {
        val ip = _uiState.value.ipAddress
        _uiState.value = _uiState.value.copy(
            connectionStatus = ConnectionStatus.Testing,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                EscPosPrinterLan.testConnection(ip)
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.Connected,
                    isLoading = false,
                    lastTestResult = TestResult(
                        success = true,
                        timestamp = System.currentTimeMillis(),
                        message = "Conexión exitosa"
                    ),
                    errorMessage = null
                )
            } catch (e: SocketTimeoutException) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.Error,
                    isLoading = false,
                    errorMessage = "La impresora no respondió en el tiempo límite (5s)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.Error,
                    isLoading = false,
                    errorMessage = "No se pudo conectar a la impresora en $ip: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Persists the current IP address from [uiState] to local storage.
     */
    fun saveIpAddress() {
        val ip = _uiState.value.ipAddress
        prefsRepository.saveIpAddress(ip)
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * [ViewModelProvider.Factory] that injects [PrinterPreferencesRepository] into
     * [PrinterConfigViewModel]. Use this factory when creating the ViewModel from
     * [MainActivity] or any other Android lifecycle owner.
     */
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
