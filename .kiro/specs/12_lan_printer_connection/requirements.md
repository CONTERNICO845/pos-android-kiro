# Requirements Document

## Introduction

This feature implements the real LAN printer connection for the POS application using raw ESC/POS commands over TCP/IP. The existing placeholder test function and basic socket implementation are upgraded to use proper connection timeouts, CP850 encoding for thermal printer compatibility, and full test-print functionality wired through the ViewModel to the UI.

## Glossary

- **EscPosPrinterLan**: Singleton object responsible for raw TCP socket communication with an ESC/POS thermal printer over LAN on port 9100.
- **PrinterConfigViewModel**: ViewModel managing the printer configuration screen state, including IP address, connection status, and test results.
- **PrinterPreferencesRepository**: Repository that persists and retrieves the printer IP address from SharedPreferences.
- **PrinterConfigUiState**: Data class holding all UI state for the printer configuration screen.
- **ConnectionStatus**: Enum representing printer connection states (Connected, Disconnected, Testing, Error).
- **TestResult**: Data class representing the outcome of a printer test operation.
- **ESC/POS**: Command protocol used by thermal receipt printers for initialization, text printing, and paper cutting.
- **CP850**: Character encoding (Code Page 850) used by thermal printers for Western European character support including Spanish characters.
- **InetSocketAddress**: Java class that creates an unresolved socket address, allowing separate connection with a timeout parameter.

## Requirements

### Requirement 1: INTERNET Permission

**User Story:** As a developer, I want the app to declare the INTERNET permission, so that the app can open TCP sockets to communicate with the LAN printer.

#### Acceptance Criteria

1. THE AndroidManifest.xml SHALL declare `android.permission.INTERNET` as a uses-permission element before the application tag.

### Requirement 2: Default IP Address

**User Story:** As a user, I want the printer IP field pre-populated with a sensible default, so that I do not have to manually type the address on first use.

#### Acceptance Criteria

1. WHEN no IP address has been previously saved, THE PrinterPreferencesRepository SHALL return "192.168.1.248" as the default value.
2. WHEN an IP address has been previously saved, THE PrinterPreferencesRepository SHALL return the saved value unchanged.

### Requirement 3: Socket Connection with Timeout

**User Story:** As a user, I want the printer connection to fail quickly if the printer is unreachable, so that I do not wait excessively for error feedback.

#### Acceptance Criteria

1. WHEN establishing a connection, THE EscPosPrinterLan SHALL create an unconnected Socket and connect using InetSocketAddress with the target IP and port 9100.
2. WHEN establishing a connection, THE EscPosPrinterLan SHALL use a connect timeout of 5000 milliseconds.
3. THE EscPosPrinterLan SHALL use 10000 milliseconds as the overall operation timeout (withTimeout coroutine wrapper).
4. IF the connection exceeds the 5000ms connect timeout, THEN THE EscPosPrinterLan SHALL throw a SocketTimeoutException.

### Requirement 4: CP850 Text Encoding

**User Story:** As a user, I want printed tickets to display Spanish characters correctly, so that accented characters and special symbols render properly on the thermal printer.

#### Acceptance Criteria

1. WHEN encoding text for the printer, THE EscPosPrinterLan SHALL use Charset CP850 (Charset.forName("Cp850")) instead of UTF-8.
2. WHEN encoding line feed characters, THE EscPosPrinterLan SHALL use Charset CP850.

### Requirement 5: Coroutine Execution Context

**User Story:** As a developer, I want printer I/O operations to run on the IO dispatcher, so that the main thread remains unblocked during network communication.

#### Acceptance Criteria

1. THE EscPosPrinterLan SHALL execute all socket I/O operations within `withContext(Dispatchers.IO)`.

### Requirement 6: Test Print Functionality

**User Story:** As a user, I want to press "Probar impresora" and have the printer produce a test page, so that I can verify the connection is working before printing real tickets.

#### Acceptance Criteria

1. WHEN the user triggers testPrinter, THE PrinterConfigViewModel SHALL read the current IP address from uiState.
2. WHEN the user triggers testPrinter, THE PrinterConfigViewModel SHALL launch a coroutine in viewModelScope.
3. WHEN a test print is initiated, THE PrinterConfigViewModel SHALL set connectionStatus to Testing.
4. WHEN executing a test print, THE EscPosPrinterLan SHALL send the ESC/POS initialization command (0x1B, 0x40).
5. WHEN executing a test print, THE EscPosPrinterLan SHALL send the text "Prueba de Conexion Exitosa" encoded in CP850.
6. WHEN executing a test print, THE EscPosPrinterLan SHALL send line feed characters followed by a full paper cut command (0x1D, 0x56, 0x00).
7. WHEN the test print succeeds, THE PrinterConfigViewModel SHALL set connectionStatus to Connected and lastTestResult to a successful TestResult with a descriptive message.
8. IF the test print fails due to a timeout or connection error, THEN THE PrinterConfigViewModel SHALL set connectionStatus to Error and update errorMessage with a user-friendly description.

### Requirement 7: Error Handling and UI Feedback

**User Story:** As a user, I want to see clear error messages when the printer connection fails, so that I can troubleshoot the issue.

#### Acceptance Criteria

1. IF a SocketTimeoutException occurs, THEN THE PrinterConfigViewModel SHALL set errorMessage to a message indicating the printer did not respond within the timeout period.
2. IF a ConnectException or IOException occurs, THEN THE PrinterConfigViewModel SHALL set errorMessage to a message indicating the printer is unreachable at the specified IP.
3. WHEN an error occurs during test print, THE PrinterConfigViewModel SHALL set isLoading to false and connectionStatus to Error.
4. WHEN a test print succeeds, THE PrinterConfigViewModel SHALL clear any previous errorMessage by setting it to null.
5. WHEN connectionStatus or errorMessage changes, THE PrinterConfigScreen SHALL display appropriate visual feedback to the user (Snackbar or inline message).
