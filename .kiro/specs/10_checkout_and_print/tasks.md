# Implementation Plan: Checkout and Print

## Overview

This plan implements the full checkout flow for the POS application in Kotlin with Jetpack Compose. The implementation extends existing entities (OrderEntity, OrderDao, PosViewModel, PosScreen) and adds new composables (CheckoutPanel, CashKeypad, ConfirmationModal) and a pure TicketFormatter utility. Work proceeds from data layer (schema) → domain logic (formatter) → ViewModel (state + actions) → UI (composables) → integration wiring.

## Tasks

- [x] 1. Extend database schema and data layer
  - [x] 1.1 Add new fields to OrderEntity and create database migration
    - Add nullable `customerName: String?`, `clientTicketText: String?`, and `internalTicketText: String?` fields to `OrderEntity`
    - Increment `AppDatabase` version from 3 to 4
    - Add `fallbackToDestructiveMigration` or `Migration(3, 4)` SQL with three ALTER TABLE ADD COLUMN statements
    - _Requirements: 1.1, 1.2, 1.4, 12.3_

  - [x] 1.2 Extend OrderDao with query-by-id method
    - Add `@Query("SELECT * FROM orders WHERE id = :orderId") suspend fun getOrderById(orderId: String): OrderEntity?` to `OrderDao`
    - _Requirements: 1.5_

  - [x] 1.3 Update OrderRepository to accept ticket text fields
    - Modify `persistOrder()` signature to accept `customerName`, `clientTicketText`, and `internalTicketText` parameters
    - Ensure the transaction inserts OrderEntity, OrderItemEntity list, and OrderItemCustomizationEntity list atomically
    - _Requirements: 1.3, 12.1, 12.2, 12.4_

  - [x]* 1.4 Write property test for ticket text persistence round-trip
    - **Property 1: Ticket Text Persistence Round-Trip**
    - **Validates: Requirements 1.3, 1.5, 12.1, 12.3**

- [x] 2. Implement TicketFormatter utility
  - [x] 2.1 Create TicketFormatter object with currency formatting and tax calculations
    - Create `TicketFormatter.kt` in `ui/pos/` (or a `util/` package)
    - Implement `formatCurrency(amount: Double): String` using HALF_UP rounding, "$X.XX" format
    - Implement `calculateSubtotal(total: Double): Double` — total / 1.16 rounded HALF_UP to 2 decimals
    - Implement `calculateIva(total: Double): Double` — computed so that SUBTOTAL + IVA = TOTAL exactly
    - Implement `TicketLineItem` data class
    - _Requirements: 10.2, 10.4, 10.5, 10.6_

  - [x] 2.2 Implement formatClientTicket function
    - Generate header ("LOS TACOS", ticket ID, date-time, customer name, payment status)
    - Generate items table with CANT (5 chars), DESCRIPCION (30 chars), IMPORTE (right-aligned 10 chars)
    - Generate SUBTOTAL, IVA 16%, TOTAL lines
    - Generate footer ("Gracias por su compra", "Conserve su ticket")
    - Use 48-character dash separator lines
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 10.1, 10.3_

  - [x] 2.3 Implement formatInternalTicket function
    - Generate header (same as client ticket: "LOS TACOS", ticket ID, date-time, customer name, payment status)
    - Generate items table with CANT and DESCRIPCION only (no prices)
    - Generate "Total: {count} Artículos" line
    - Generate footer and 48-char dash separators
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x]* 2.4 Write property tests for TicketFormatter
    - **Property 9: SUBTOTAL + IVA = TOTAL Invariant**
    - **Property 13: TicketFormatter Determinism**
    - **Property 14: Currency Formatting**
    - **Validates: Requirements 10.1, 10.2, 10.4, 10.5, 10.6**

  - [x]* 2.5 Write property tests for ticket content correctness
    - **Property 7: Ticket Header Contains Identifying Information**
    - **Property 8: Client Ticket Item Table Formatting**
    - **Property 10: Ticket Separator Lines Are 48 Dashes**
    - **Property 11: Internal Ticket Excludes Prices**
    - **Property 12: Internal Ticket Article Count**
    - **Validates: Requirements 8.2, 8.3, 8.4, 8.7, 9.2, 9.3, 9.4, 9.5, 9.7, 10.3**

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Extend PosViewModel with checkout state and actions
  - [x] 4.1 Create CheckoutState data class and PaymentStatus enum
    - Create `CheckoutState` data class with `customerName`, `paymentStatus`, `denominationCounts`, `cashReceived`, `printAttempts`, `isPrinting` fields
    - Create `PaymentStatus` enum with `PAGADO`, `NO_PAGO`, `PAGA_DESPUES` values and `displayText` property
    - _Requirements: 3.3, 4.3, 5.6_

  - [x] 4.2 Extend PosUiState and PosViewModel with checkout state flows
    - Add `isCheckoutVisible`, `checkoutState`, `isConfirmationModalVisible`, `confirmButtonText` fields to `PosUiState`
    - Add `_checkoutState` MutableStateFlow to `PosViewModel`
    - Add `_isCheckoutVisible` and `_isConfirmationModalVisible` MutableStateFlow
    - Incorporate new flows into the existing `combine(...)` producing `uiState`
    - _Requirements: 2.1, 2.5_

  - [x] 4.3 Implement checkout transition and customer name logic
    - Implement `showCheckout()` — sets `_isCheckoutVisible = true` (only when cart non-empty)
    - Implement `hideCheckout()` — sets `_isCheckoutVisible = false` without modifying cart
    - Implement `updateCustomerName(name: String)` — trims and truncates to 40 chars
    - Add logic: if all items removed while checkout visible, auto-hide checkout
    - _Requirements: 2.1, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 4.4 Implement payment status selection and cash keypad logic
    - Implement `selectPaymentStatus(status: PaymentStatus)` — updates checkout state
    - Implement `addDenomination(value: Int)` — adds value if cumulative ≤ $999,999.99
    - Implement `clearDenominations()` — resets all counts and cashReceived to 0
    - Default payment status to `PAGADO` on checkout show
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.5, 5.6, 5.7_

  - [x] 4.5 Implement order completion validation
    - Implement `isCompletarOrdenEnabled()` — disabled when name blank, or PAGADO with insufficient cash
    - Implement `showConfirmationModal()` and `dismissConfirmationModal()`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.5_

  - [x]* 4.6 Write property tests for ViewModel checkout logic
    - **Property 2: Customer Name Normalization**
    - **Property 3: Cash Received is Sum of Denominations**
    - **Property 4: Clear Denominations Resets to Zero**
    - **Property 5: Cash Received Maximum Cap Invariant**
    - **Property 6: Completar Orden Validation Logic**
    - **Validates: Requirements 3.3, 3.4, 3.5, 5.2, 5.5, 5.6, 5.7, 6.1, 6.2, 6.3, 6.4**

- [x] 5. Implement print and persist flow in PosViewModel
  - [x] 5.1 Implement confirmPayment with printer execution
    - Implement `confirmPayment()` — changes button text to "Imprimiendo Ticket", disables buttons
    - Retrieve printer IP from `PrinterPreferencesRepository.getIpAddress()`
    - If IP is empty, show Snackbar error and re-enable buttons
    - Call `EscPosPrinterLan.printTicket()` with 15-second timeout
    - On failure, show error Snackbar, increment `printAttempts`, allow retry up to 3 times
    - Add `PrinterPreferencesRepository` as a constructor dependency of `PosViewModel`
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 5.2 Implement order persistence with tickets and cycle reset
    - On print success, generate tickets via `TicketFormatter`, persist OrderEntity with `clientTicketText` and `internalTicketText` via `OrderRepository`
    - Use single Room transaction for OrderEntity + OrderItemEntity + OrderItemCustomizationEntity
    - On persistence success, call `resetPosState()` — clear cart, reset checkout state, hide checkout panel
    - On persistence failure, retain all state and show error Snackbar
    - _Requirements: 8.1, 8.8, 9.1, 12.1, 12.2, 12.3, 12.4, 12.5, 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x]* 5.3 Write property tests for POS cycle reset and persistence failure
    - **Property 15: Cancel Checkout Preserves Cart**
    - **Property 16: POS Cycle Reset Clears All State**
    - **Property 17: Persistence Failure Preserves State**
    - **Validates: Requirements 2.5, 13.1, 13.2, 13.3, 13.5**

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement Checkout UI composables
  - [x] 7.1 Create CashKeypad composable
    - Grid layout of denomination buttons ($1000, $500, $200, $100, $50, $20, $10, $5, $2, $1)
    - Display `Badge` on each button showing press count (only when count > 0)
    - Display cumulative cash received formatted as "$X.XX"
    - Include "Limpiar" button to clear all denominations
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 7.2 Create CheckoutPanel composable
    - Layout: customer name TextField (max 40 chars, label "Nombre del cliente"), payment status button row, CashKeypad, "Completar Orden" button, "Cancelar" button
    - Wire callbacks to ViewModel functions via parameters
    - Disable "Completar Orden" when validation returns false
    - _Requirements: 2.3, 3.1, 3.4, 4.1, 4.2, 6.1, 6.2, 6.3, 6.4_

  - [x] 7.3 Create ConfirmationModal composable
    - Display order total, payment status text
    - Conditionally show cash received and change (only when PAGADO)
    - Display "Confirmar Pago" and "Cancelar" buttons
    - Support button text change ("Imprimiendo Ticket") and disabled state during printing
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 11.1_

  - [x] 7.4 Integrate CheckoutPanel and ConfirmationModal into PosScreen
    - Modify `PosScreen` to conditionally show CheckoutPanel or CatalogPanel in left 70% based on `isCheckoutVisible`
    - Wire TOTAL button in `CartPanel` to `showCheckout()`, disable when cart is empty
    - Show `ConfirmationModal` as dialog when `isConfirmationModalVisible` is true
    - Handle auto-return to catalog when cart emptied during checkout
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 2.6_

  - [x]* 7.5 Write unit tests for UI state transitions
    - Test checkout show/hide transitions
    - Test modal show/dismiss
    - Test auto-return to catalog on empty cart
    - Test TOTAL button disabled with empty cart
    - _Requirements: 2.1, 2.4, 2.5, 2.6_

- [x] 8. Update PosViewModel Factory with new dependencies
  - [x] 8.1 Update PosViewModel.Factory to accept PrinterPreferencesRepository
    - Add `PrinterPreferencesRepository` parameter to the `Factory` class
    - Update all call sites that instantiate the Factory (likely in the navigation/DI setup)
    - _Requirements: 11.2_

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project uses Kotest Property for property-based tests and MockK for mocking
- All existing test dependencies (kotest-property, kotest-runner-junit5, mockk, turbine, kotlinx-coroutines-test) are already in build.gradle.kts
- The `TicketFormatter` is a pure object with no side effects, making it ideal for property-based testing
- The printer integration uses the existing `EscPosPrinterLan` class already available in the project

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "4.1"] },
    { "id": 1, "tasks": ["1.3", "2.1", "4.2"] },
    { "id": 2, "tasks": ["1.4", "2.2", "2.3", "4.3", "4.4"] },
    { "id": 3, "tasks": ["2.4", "2.5", "4.5"] },
    { "id": 4, "tasks": ["4.6", "5.1"] },
    { "id": 5, "tasks": ["5.2", "7.1"] },
    { "id": 6, "tasks": ["5.3", "7.2"] },
    { "id": 7, "tasks": ["7.3", "7.4"] },
    { "id": 8, "tasks": ["7.5", "8.1"] }
  ]
}
```
