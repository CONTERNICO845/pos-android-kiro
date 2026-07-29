# Requirements Document

## Introduction

This document specifies the requirements for the Printer Configuration UI feature in the PuntoDeVenta Android application. The feature provides a visual interface for configuring a POS-8360 thermal printer connection via IP address, testing the connection, and displaying printer status information. This phase focuses strictly on UI composition and navigation routing without implementing network communication logic.

## Glossary

- **Printer_Config_Screen**: The main composable screen that displays printer configuration and status
- **Control_Panel**: The left column of the Printer_Config_Screen with dark green background containing editable and static printer settings
- **Status_Panel**: The right column of the Printer_Config_Screen with light gray background displaying read-only printer information
- **IP_Address_Field**: An editable OutlinedTextField for entering the printer's local IP address
- **Static_Display_Row**: A read-only visual row showing a label and value with light green background
- **Test_Button**: The "Probar impresora" button that triggers a test print action
- **Save_Button**: The "Guardar" button that persists the entered IP address
- **NavRail**: The existing left lateral navigation bar component
- **Main_Content_Area**: The screen region to the right of the NavRail where destination screens render
- **ViewModel**: The state management component holding the IP address and UI state

## Requirements

### Requirement 1: Navigation Integration

**User Story:** As a user, I want to access the printer configuration screen from the navigation bar, so that I can configure my thermal printer.

#### Acceptance Criteria

1. WHEN the user clicks the "Impresora" icon in the NavRail, THE NavHost SHALL navigate to the printer route destination
2. WHEN the printer route is navigated to, THE Printer_Config_Screen SHALL display in the Main_Content_Area
3. THE NavRail SHALL maintain its existing visual design and positioning when the Printer_Config_Screen is displayed
4. WHEN the current destination is the printer route, THE "Impresora" navigation item SHALL use NavRailIconSelected color
5. WHEN the current destination is not the printer route, THE "Impresora" navigation item SHALL use NavRailIconDefault color
6. WHEN the app initializes, THE current destination SHALL not be the printer route by default

### Requirement 2: Two-Column Layout

**User Story:** As a user, I want to see configuration controls and status information side-by-side, so that I can understand my printer setup at a glance.

#### Acceptance Criteria

1. THE Printer_Config_Screen SHALL display two columns using a weighted Row layout
2. THE left column SHALL occupy 50% of the Main_Content_Area width (weight = 1f)
3. THE right column SHALL occupy 50% of the Main_Content_Area width (weight = 1f)
4. THE left column SHALL render the Control_Panel with CardBackground color
5. THE right column SHALL render the Status_Panel with StatusPanelBackground color

### Requirement 3: Control Panel Header

**User Story:** As a user, I want to see clear identification of the printer model, so that I know which device I am configuring.

#### Acceptance Criteria

1. THE Control_Panel SHALL display "IMPRESORA" as the title text using Color.White with FontWeight.Bold
2. THE Control_Panel SHALL display "POS-8360 LAN" as the subtitle text using Color.White with FontWeight.Normal
3. THE subtitle text SHALL be positioned 8.dp below the title text vertically
4. THE title text SHALL use a font size of 24.sp
5. THE subtitle text SHALL use a font size of 16.sp
6. THE title and subtitle text SHALL be center-aligned horizontally within the Control_Panel

### Requirement 4: IP Address Input Field

**User Story:** As a user, I want to enter and edit the printer's IP address, so that the system can connect to the correct network device.

#### Acceptance Criteria

1. THE Control_Panel SHALL display an editable IP_Address_Field with the label "IP local"
2. THE IP_Address_Field SHALL be an OutlinedTextField component with Color.White background
3. WHEN the user types in the IP_Address_Field, THE ViewModel SHALL update its ipAddress state property
4. THE IP_Address_Field SHALL display the current value from the ViewModel ipAddress state
5. THE IP_Address_Field SHALL use InputBorder color for its outline
6. THE IP_Address_Field SHALL accept only numeric characters (0-9) and periods (.)
7. WHEN the user enters an invalid IP format, THE IP_Address_Field SHALL display an error state with red border color

### Requirement 5: Static Printer Settings Display

**User Story:** As a user, I want to see non-editable printer settings, so that I can verify the printer configuration without accidentally changing values.

#### Acceptance Criteria

1. THE Control_Panel SHALL display Static_Display_Row components in the following top-to-bottom order: Puerto (9100), Papel (80mm), Corte (Automatico), Modo (ESC/POS)
2. WHEN a Static_Display_Row is rendered, THE row SHALL have BackgroundSecondary color background
3. WHEN a Static_Display_Row is rendered, THE label and value SHALL be arranged horizontally with the label on the left and value on the right
4. WHEN a Static_Display_Row is rendered, THE label text SHALL use Color.White with FontWeight.Bold and font size 16.sp
5. WHEN a Static_Display_Row is rendered, THE value text SHALL use Color.White with FontWeight.Normal and font size 16.sp

### Requirement 6: Action Buttons

**User Story:** As a user, I want to test the printer connection and save my settings, so that I can verify the configuration works before proceeding.

#### Acceptance Criteria

1. THE Control_Panel SHALL display a Test_Button with label "Probar impresora" at the bottom
2. THE Control_Panel SHALL display a Save_Button with label "Guardar" at the bottom
3. THE Test_Button and Save_Button SHALL be arranged horizontally with equal width
4. THE Test_Button SHALL use ButtonConfirm background color and ButtonConfirmText text color
5. THE Save_Button SHALL use ButtonConfirm background color and ButtonConfirmText text color
6. WHEN the user clicks the Test_Button, THE ViewModel SHALL invoke its testPrinter method
7. WHEN the user clicks the Save_Button, THE ViewModel SHALL invoke its saveIpAddress method

### Requirement 7: Status Panel Header

**User Story:** As a user, I want to see a clear section title for connection status, so that I understand what information is being displayed.

#### Acceptance Criteria

1. THE Status_Panel SHALL display "Estado de conexion" as the header text
2. THE header text SHALL use NavRailIconSelected color with bold font weight
3. THE header text SHALL use a font size of 20.sp

### Requirement 8: Status Information Display

**User Story:** As a user, I want to see detailed printer specifications, so that I can confirm the system recognizes the correct printer model and capabilities.

#### Acceptance Criteria

1. THE Status_Panel SHALL display a row with label "Modelo" and value "POS-8360 Termica"
2. THE Status_Panel SHALL display a row with label "Papel" and value "80mm"
3. THE Status_Panel SHALL display a row with label "Conexion" and value "LAN / Socket TCP"
4. THE Status_Panel SHALL display a row with label "Puerto" and value "9100"
5. THE Status_Panel SHALL display a row with label "Cortador" and value "Activo al finalizar ticket"
6. WHEN a status row is rendered, THE label text SHALL use ModalBodyText color with bold font weight
7. WHEN a status row is rendered, THE value text SHALL use ModalBodyText color with normal font weight

### Requirement 9: Status Panel Description

**User Story:** As a user, I want to understand what happens when I test the printer, so that I know what to expect.

#### Acceptance Criteria

1. THE Status_Panel SHALL display the description text "Cuando presiones imprimir prueba, se enviara el ticket real por red usando la clase Java ESC/POS." at the bottom
2. THE description text SHALL use ModalBodyText color with normal font weight
3. THE description text SHALL use a font size of 14.sp
4. THE description text SHALL wrap to multiple lines if necessary

### Requirement 10: ViewModel State Management

**User Story:** As a developer, I want a ViewModel to manage printer configuration state, so that the UI remains decoupled from business logic.

#### Acceptance Criteria

1. THE system SHALL provide a PrinterConfigViewModel class extending ViewModel
2. THE PrinterConfigViewModel SHALL expose a uiState property of type StateFlow<PrinterConfigUiState>
3. THE PrinterConfigUiState data class SHALL contain an ipAddress property of type String
4. THE PrinterConfigViewModel SHALL provide an updateIpAddress method accepting a String parameter
5. WHEN updateIpAddress is called, THE ViewModel SHALL update the ipAddress property in uiState
6. THE PrinterConfigViewModel SHALL provide a testPrinter method that logs a debug message (placeholder for future network implementation)
7. THE PrinterConfigViewModel SHALL provide a saveIpAddress method that persists the current ipAddress from uiState to local storage
8. WHEN the PrinterConfigViewModel is initialized, THE ViewModel SHALL load the previously saved ipAddress from local storage and set it as the initial value in uiState

### Requirement 11: Color Palette Compliance

**User Story:** As a developer, I want the printer configuration screen to use the existing color palette, so that visual consistency is maintained across the application.

#### Acceptance Criteria

1. THE Control_Panel background SHALL use Color(0xFF2D5A1B) (CardBackground)
2. THE Status_Panel background SHALL use Color(0xFFE0E0E0) (light gray)
3. THE Static_Display_Row background SHALL use Color(0xFF5AAD30) (BackgroundSecondary)
4. THE Printer_Config_Screen SHALL NOT introduce any hardcoded Color values not defined in Color.kt
5. WHERE a color token does not exist in Color.kt for Status_Panel background, THE developer SHALL add StatusPanelBackground = Color(0xFFE0E0E0) to Color.kt

### Requirement 13: Local Storage for IP Address

**User Story:** As a user, I want my printer IP address to be saved between app sessions, so that I don't have to re-enter it every time I open the app.

#### Acceptance Criteria

1. THE system SHALL provide a PrinterPreferencesRepository class that reads and writes the printer IP address using SharedPreferences
2. WHEN the user clicks the Save_Button, THE system SHALL persist the current IP address value to local storage via PrinterPreferencesRepository
3. WHEN the Printer_Config_Screen is opened, THE IP_Address_Field SHALL display the previously saved IP address loaded from local storage
4. IF no IP address has been saved previously, THE IP_Address_Field SHALL display an empty string as the initial value

**User Story:** As a developer, I want the printer screen to follow Compose best practices, so that the code is maintainable and testable.

#### Acceptance Criteria

1. THE system SHALL provide a PrinterScreen composable function as the main entry point
2. THE PrinterScreen composable SHALL accept a viewModel parameter of type PrinterConfigViewModel with a default value
3. THE system SHALL provide a ControlPanel composable function for the left column
4. THE ControlPanel composable SHALL accept ipAddress, onIpAddressChange, onTestClick, and onSaveClick parameters
5. THE system SHALL provide a StatusPanel composable function for the right column
6. THE StatusPanel composable SHALL not accept any parameters (displays static data only)
7. THE system SHALL provide a StaticSettingRow composable function accepting label and value parameters

