# Implementation Plan: Real LAN Printer Connection (ESC/POS)

## Overview

This plan upgrades the existing placeholder printer connection to a fully functional LAN thermal printer integration. Changes are minimal and surgical — modifying four existing files (AndroidManifest.xml, PrinterPreferencesRepository, EscPosPrinterLan, PrinterConfigViewModel) with no new classes needed. The implementation follows the existing MVVM architecture and coroutine patterns already in the project.

## Tasks

- [x] 1. Add INTERNET permission and update default IP
  - [x] 1.1 Add INTERNET permission to AndroidManifest.xml
    - Add `<uses-permission android:name="android.permission.INTERNET" />` before the `<application>` tag in `app/src/main/AndroidManifest.xml`
    - _Requirements: 1.1_

  - [x] 1.2 Change default IP in PrinterPreferencesRepository
    - In `app/src/main/java/com/example/puntodeventa/data/repository/PrinterPreferencesRepository.kt`
    - Add `private const val DEFAULT_IP = "192.168.1.248"` to the companion object
    - Change `getIpAddress()` to use `DEFAULT_IP` as the fallback: `prefs.getString(KEY_IP_ADDRESS, DEFAULT_IP) ?: DEFAULT_IP`
    - _Requirements: 2.1, 2.2_

  - [x]* 1.3 Write property test for IP address persistence round-trip
    - **Property 1: Default IP Round-Trip**
    - For any fresh repository, getIpAddress() returns "192.168.1.248"; for any saved IP string, getIpAddress() returns that string unchanged
    - **Validates: Requirements 2.1, 2.2**

- [x] 2. Update EscPosPrinterLan with InetSocketAddress, timeout, and CP850
  - [x] 2.1 Refactor EscPosPrinterLan socket connection and encoding
    - In `app/src/main/java/com/example/puntodeventa/data/printer/EscPosPrinterLan.kt`
    - Change `TIMEOUT_MS = 15_000L` to `OVERALL_TIMEOUT_MS = 10_000L`
    - Add `private const val CONNECT_TIMEOUT_MS = 5_000`
    - Add `private val CHARSET = Charset.forName("Cp850")`
    - Add import for `java.net.InetSocketAddress`
    - In `printTicket()`: replace `Socket(ipAddress, PORT)` with `Socket()` + `socket.connect(InetSocketAddress(ipAddress, PORT), CONNECT_TIMEOUT_MS)`
    - Change all `Charsets.UTF_8` references to `CHARSET` (the CP850 charset)
    - Update `soTimeout` to use `CONNECT_TIMEOUT_MS` instead of `TIMEOUT_MS.toInt()`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 5.1_

  - [x] 2.2 Add testConnection method to EscPosPrinterLan
    - Add `suspend fun testConnection(ipAddress: String)` method
    - Create unconnected `Socket()`, connect with `InetSocketAddress(ipAddress, PORT)` and `CONNECT_TIMEOUT_MS`
    - Send ESC_INIT (0x1B, 0x40)
    - Send `"Prueba de Conexion Exitosa".toByteArray(CHARSET)`
    - Send `"\n\n\n".toByteArray(CHARSET)`
    - Send ESC_CUT (0x1D, 0x56, 0x00)
    - Flush and close socket in finally block
    - Wrap in `withTimeout(OVERALL_TIMEOUT_MS)` and `withContext(Dispatchers.IO)`
    - _Requirements: 6.4, 6.5, 6.6_

  - [x]* 2.3 Write property test for CP850 encoding round-trip
    - **Property 4: CP850 Encoding Round-Trip for Spanish Characters**
    - For any string containing characters in the CP850 character set (ñ, á, é, í, ó, ú, ¡, ¿), encoding then decoding with Cp850 produces the original string
    - **Validates: Requirements 4.1, 4.2**

- [x] 3. Wire PrinterConfigViewModel.testPrinter() to EscPosPrinterLan
  - [x] 3.1 Implement testPrinter() with coroutine and state management
    - In `app/src/main/java/com/example/puntodeventa/ui/printer/PrinterConfigViewModel.kt`
    - Add imports: `androidx.lifecycle.viewModelScope`, `kotlinx.coroutines.launch`, `java.net.SocketTimeoutException`
    - Add import for `com.example.puntodeventa.data.printer.EscPosPrinterLan`
    - Replace the current `testPrinter()` body with:
      - Read IP from `_uiState.value.ipAddress`
      - Set state to `connectionStatus = Testing, isLoading = true, errorMessage = null`
      - Launch coroutine in `viewModelScope`
      - Call `EscPosPrinterLan.testConnection(ip)`
      - On success: set `connectionStatus = Connected`, `isLoading = false`, `lastTestResult = TestResult(success=true, timestamp=System.currentTimeMillis(), message="Conexión exitosa")`, `errorMessage = null`
      - Catch `SocketTimeoutException`: set `connectionStatus = Error`, `isLoading = false`, `errorMessage = "La impresora no respondió en el tiempo límite (5s)"`
      - Catch general `Exception`: set `connectionStatus = Error`, `isLoading = false`, `errorMessage = "No se pudo conectar a la impresora en $ip: ${e.localizedMessage}"`
    - _Requirements: 6.1, 6.2, 6.3, 6.7, 6.8, 7.1, 7.2, 7.3, 7.4_

  - [x]* 3.2 Write property test for state transition consistency
    - **Property 2: Test Print State Transition Consistency**
    - For any invocation of testPrinter(), connectionStatus transitions to Testing before the network call, then to either Connected (success) or Error (failure) — never remaining in Testing
    - **Validates: Requirements 6.3, 6.7, 6.8**

  - [x]* 3.3 Write property test for error state consistency
    - **Property 3: Error State Implies Error Message**
    - For any state where connectionStatus == Error, errorMessage is non-null and non-empty; for any state where connectionStatus == Connected, errorMessage is null
    - **Validates: Requirements 7.1, 7.2, 7.4**

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Wire UI feedback for connection status
  - [x] 5.1 Add Snackbar/Toast feedback to PrinterConfigScreen
    - In the printer config screen composable, observe `uiState.errorMessage` and `uiState.connectionStatus`
    - Show a Snackbar with the error message when `errorMessage` is non-null
    - Show a success Snackbar or visual indicator when `connectionStatus` transitions to Connected
    - Clear the Snackbar when the user dismisses it or starts a new test
    - Use `LaunchedEffect` keyed on `errorMessage` or `lastTestResult` to trigger the Snackbar
    - _Requirements: 7.5_

- [x] 6. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- The project uses Kotest Property for property-based tests (already in build.gradle.kts)
- All changes modify existing files — no new classes are introduced
- The `EscPosPrinterLan` object is a singleton, so no dependency injection is needed for it
- The ViewModel already has `PrinterPreferencesRepository` injected via its Factory

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["3.1"] },
    { "id": 4, "tasks": ["3.2", "3.3"] },
    { "id": 5, "tasks": ["5.1"] }
  ]
}
```
