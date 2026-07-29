# Implementation Plan: Printer Configuration UI

## Overview

This implementation plan converts the printer configuration UI design into discrete coding tasks for a Kotlin/Android Compose application. The implementation follows the existing project structure and integrates with the current navigation system while adding new UI components for printer configuration and status display.

## Tasks

- [x] 1. Set up ViewModel and data structures
  - [x] 1.1 Create PrinterConfigUiState data class
    - Define data class with ipAddress, isLoading, errorMessage, connectionStatus, and lastTestResult properties
    - Add supporting enums ConnectionStatus and TestResult data class
    - _Requirements: 10.3, 10.5_

  - [x] 1.2 Implement PrinterConfigViewModel class
    - Create ViewModel class extending ViewModel with StateFlow<PrinterConfigUiState>
    - Implement updateIpAddress, testPrinter (logs a debug message), and saveIpAddress (persists to SharedPreferences) methods
    - Add ViewModelProvider.Factory inner class for dependency injection
    - _Requirements: 10.1, 10.2, 10.4, 10.6, 10.7_

  - [x] 1.3 Write property test for ViewModel state consistency
    - **Property 7: ViewModel State Update Consistency**
    - **Validates: Requirements 10.5**

- [x] 2. Extend color palette and create static configuration
  - [x] 2.1 Add StatusPanelBackground color to Color.kt
    - Add StatusPanelBackground = Color(0xFFE0E0E0) to existing Color.kt file
    - _Requirements: 11.5_

  - [x] 2.2 Create PrinterSpecs object with static configuration data
    - Define object with MODEL, PAPER_SIZE, CONNECTION_TYPE, PORT, CUTTER, PROTOCOL constants
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 3. Implement core UI components
  - [x] 3.1 Create StaticSettingRow composable component
    - Implement composable with label and value parameters
    - Apply BackgroundSecondary background and white text styling
    - _Requirements: 5.2, 5.3, 5.4, 5.5, 12.7_

  - [x]* 3.2 Write property test for StaticSettingRow styling
    - **Property 4: StaticDisplayRow Universal Styling**
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5**

  - [x] 3.3 Create StatusInfoRow composable for status panel
    - Implement component for displaying status information rows
    - Apply ModalBodyText colors with bold labels and normal values
    - _Requirements: 8.6, 8.7_

  - [x]* 3.4 Write property test for status row styling
    - **Property 5: Status Row Universal Styling**
    - **Validates: Requirements 8.6, 8.7**

- [x] 4. Implement ControlPanel composable
  - [x] 4.1 Create ControlPanel structure and header
    - Implement Column layout with CardBackground color
    - Add "IMPRESORA" title and "POS-8360 LAN" subtitle with specified styling
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 12.4_

  - [x] 4.2 Add IP address input field to ControlPanel
    - Implement OutlinedTextField with "IP local" label and white background
    - Add input filtering for numeric characters and periods only
    - Implement error state display with red border for invalid IP formats
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x]* 4.3 Write property test for input character filtering
    - **Property 2: Input Character Filtering**
    - **Validates: Requirements 4.6**

  - [x]* 4.4 Write property test for IP validation error display
    - **Property 3: IP Format Validation Error Display**
    - **Validates: Requirements 4.7**

  - [x] 4.5 Add static printer settings rows to ControlPanel
    - Implement four StaticSettingRow components for Puerto, Papel, Corte, Modo
    - Arrange in specified order with proper spacing
    - _Requirements: 5.1_

  - [x] 4.6 Add action buttons to ControlPanel
    - Implement Test_Button and Save_Button with equal horizontal arrangement
    - Apply ButtonConfirm styling and wire to ViewModel methods
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 5. Implement StatusPanel composable
  - [x] 5.1 Create StatusPanel structure and header
    - Implement Column layout with StatusPanelBackground color
    - Add "Estado de conexion" header with NavRailIconSelected color styling
    - _Requirements: 7.1, 7.2, 7.3, 12.6_

  - [x] 5.2 Add status information rows to StatusPanel
    - Implement five StatusInfoRow components for printer specifications
    - Display Modelo, Papel, Conexion, Puerto, Cortador information
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 5.3 Add description text to StatusPanel
    - Implement description text with ModalBodyText color and 14.sp size
    - Ensure text wrapping for multiple lines
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 5.4 Write property test for text wrapping responsiveness
    - **Property 6: Text Wrapping Responsiveness**
    - **Validates: Requirements 9.4**

- [x] 6. Implement main PrinterScreen composable
  - [x] 6.1 Create PrinterScreen entry point
    - Implement composable with ViewModel parameter and default value
    - Set up state collection using collectAsStateWithLifecycle
    - _Requirements: 12.1, 12.2_

  - [x] 6.2 Implement two-column layout in PrinterScreen
    - Create weighted Row layout with equal column distribution
    - Integrate ControlPanel and StatusPanel with proper spacing
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 6.3 Write property test for bidirectional state binding
    - **Property 1: Bidirectional State Binding**
    - **Validates: Requirements 4.3, 4.4**

- [x] 7. Checkpoint - Verify core UI implementation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Integrate with navigation system
  - [x] 8.1 Add printer destination to navigation routes
    - Add NavDestination.Printer to existing navigation enum
    - Update MainActivity navigation logic to handle printer route
    - _Requirements: 1.1, 1.2, 1.6_

  - [x] 8.2 Update NavRail to include printer navigation item
    - Add "Impresora" icon and label to NavRail component
    - Implement proper selected/default color states
    - Wire click handler to navigate to printer destination
    - _Requirements: 1.3, 1.4, 1.5_

  - [x] 8.3 Write unit tests for navigation integration
    - Test NavRail click behavior and destination changes
    - Test PrinterScreen display in main content area
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 9. Add comprehensive unit tests for UI components
  - [x]* 9.1 Write unit tests for ControlPanel component
    - Test header text display and styling
    - Test static settings row content and arrangement
    - Test button layout and click handlers
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 5.1, 6.1, 6.2, 6.3_

  - [x]* 9.2 Write unit tests for StatusPanel component
    - Test header styling and content
    - Test status information row display
    - Test description text content and formatting
    - _Requirements: 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3_

  - [x] 9.3 Write unit tests for PrinterScreen layout
    - Test two-column layout structure and weighting
    - Test component integration and proper rendering
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 10. Final integration and validation
  - [x] 10.1 Wire all components together in MainActivity
    - Ensure PrinterScreen renders correctly when printer route is selected
    - Verify ViewModel integration and state management
    - Test complete navigation flow from NavRail to screen display
    - _Requirements: 1.1, 1.2, 1.3, 10.1, 10.2, 12.1, 12.2_

  - [x] 10.2 Write property test for color palette compliance
    - **Property 8: Color Palette Compliance**
    - **Validates: Requirements 11.4**

  - [x] 10.3 Write integration tests for complete workflow
    - Test end-to-end navigation and state management
    - Test ViewModel interaction with UI components
    - _Requirements: 1.1, 1.2, 4.3, 4.4, 6.6, 6.7_

- [x] 11. Final checkpoint - Complete feature validation
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and component behavior
- The implementation follows Android Compose best practices with clear separation of concerns
- ViewModel methods testPrinter and saveIpAddress are placeholders for future network functionality
- All colors must be defined in Color.kt rather than hardcoded inline values
- Input validation ensures only valid IP address characters are accepted

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "3.1", "3.3"] },
    { "id": 2, "tasks": ["1.3", "3.2", "3.4", "4.1"] },
    { "id": 3, "tasks": ["4.2", "4.5", "5.1"] },
    { "id": 4, "tasks": ["4.3", "4.4", "4.6", "5.2", "5.3"] },
    { "id": 5, "tasks": ["5.4", "6.1"] },
    { "id": 6, "tasks": ["6.2", "8.1"] },
    { "id": 7, "tasks": ["6.3", "8.2"] },
    { "id": 8, "tasks": ["8.3", "9.1", "9.2", "9.3"] },
    { "id": 9, "tasks": ["10.1"] },
    { "id": 10, "tasks": ["10.2", "10.3"] }
  ]
}
```