# Implementation Plan: Ticket History Screen

## Overview

Implementación de la pantalla "Historial de Tickets" que reemplaza el placeholder actual de `TicketsScreen`. Cada commit es atómico: data layer → ViewModel → UI composables → navegación → reimpresión → property tests.

## Tasks

- [x] 1. Data layer: DAO query + Repository method
  - [x] 1.1 Add `getOrdersByTimeRange` query to OrderDao and repository method
    - Add a new `@Query` method `getOrdersByTimeRange(start: Long, end: Long): List<OrderEntity>` to `OrderDao.kt` that selects all COMPLETED orders within the timestamp range, ordered by timestamp DESC, without a LIMIT clause
    - Add `suspend fun getOrdersByTimeRange(start: Long, end: Long)` to `OrderRepository.kt` delegating to the DAO
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. ViewModel + UiState
  - [x] 2.1 Create TicketHistoryUiState data class and TicketHistoryViewModel
    - Create `app/src/main/java/com/example/puntodeventa/ui/tickets/TicketHistoryUiState.kt` with fields: `orders: List<OrderEntity>`, `selectedFilter: TimeFilter`, `isLoading: Boolean`, `errorMessage: String?`, `reprintingOrderId: String?`
    - Create `app/src/main/java/com/example/puntodeventa/ui/tickets/TicketHistoryViewModel.kt` following the StatsViewModel pattern:
      - Inject `OrderRepository` and `PrinterPreferencesRepository`
      - Expose `StateFlow<TicketHistoryUiState>`
      - `init` calls `loadOrders(TimeFilter.TODAY)`
      - `onFilterChange(filter)`: cancels in-flight job, recomputes range via `StatsViewModel.computeRange(filter)`, reloads orders
      - `onReprintTicket(order)`: sets `reprintingOrderId`, calls `EscPosPrinterLan.printTicket(ip, clientTicketText)`, clears state; logs if IP is empty or clientTicketText is null
      - Error handling: catches exceptions (re-throws CancellationException), sets `errorMessage`
      - Include `Factory` inner class implementing `ViewModelProvider.Factory`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 7.2, 7.3, 7.4_

- [x] 3. UI composables
  - [x] 3.1 Create TicketHistoryTopBar composable
    - Create `app/src/main/java/com/example/puntodeventa/ui/tickets/TicketHistoryTopBar.kt`
    - Display title "Historial de Tickets" on the left
    - Display a SegmentedButton or pill-style row with the four TimeFilter labels ("Hoy", "Ayer", "Este mes", "Todo") to the right
    - Highlight the currently selected filter and invoke `onFilterChange` callback on tap
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 3.2 Create TicketCard composable
    - Create `app/src/main/java/com/example/puntodeventa/ui/tickets/TicketCard.kt`
    - Card with `Color.White` background and slight elevation
    - Render `order.clientTicketText` with `FontFamily.Monospace` at `12.sp`
    - If `clientTicketText` is null or blank, show placeholder "Sin texto de ticket disponible"
    - Display an `OutlinedButton` labeled "Reimprimir Ticket" at the bottom
    - When `isReprinting == true`, show a loading indicator on the button and disable it
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.4_

  - [x] 3.3 Create TicketHistoryScreen composable with empty state
    - Replace the content of `app/src/main/java/com/example/puntodeventa/ui/tickets/TicketsScreen.kt` with the full `TicketHistoryScreen` composable
    - Accept `uiState: TicketHistoryUiState`, `onFilterChange: (TimeFilter) -> Unit`, `onReprintTicket: (OrderEntity) -> Unit`
    - Compose layout: `TicketHistoryTopBar` at top, `LazyColumn` of `TicketCard` items below
    - When `uiState.orders` is empty and `isLoading == false`, display centered text "No hay tickets para este periodo"
    - When `isLoading == true`, show a loading indicator
    - Background: `BackgroundPrimary`
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 4. Navigation wiring
  - [x] 4.1 Wire TicketHistoryScreen into MainActivity NavDestination.Tickets
    - In `MainActivity.kt`, replace `NavDestination.Tickets -> TicketsScreen()` with:
      - Instantiate `TicketHistoryViewModel` via `viewModel(factory = TicketHistoryViewModel.Factory(orderRepo, printerPrefsRepo))`
      - Collect `uiState` with `collectAsStateWithLifecycle()`
      - Render `TicketHistoryScreen(uiState, onFilterChange, onReprintTicket)`
    - Update imports accordingly
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. Checkpoint - Verify compilation and manual flow
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Property-based tests
  - [x]* 6.1 Write property test for DAO time range query (Property 1)
    - **Property 1: DAO time range query returns only COMPLETED orders within bounds, sorted descending**
    - **Validates: Requirements 1.1, 1.2, 5.4**
    - Create instrumented test in `app/src/androidTest/.../data/local/OrderDaoTicketHistoryPropertyTest.kt`
    - Use Kotest property testing with Arb generators for OrderEntity lists with random timestamps and statuses
    - Insert into in-memory Room DB, query with random [start, end] ranges
    - Assert: all returned orders have status COMPLETED, timestamps within [start, end], sorted descending

  - [x]* 6.2 Write property test for filter change reload (Property 2)
    - **Property 2: Filter change triggers correct time range reload**
    - **Validates: Requirements 2.2**
    - Create unit test in `app/src/test/.../ui/tickets/TicketHistoryViewModelPropertyTest.kt`
    - Mock OrderRepository, generate random TimeFilter values
    - Call `onFilterChange`, verify `getOrdersByTimeRange` is called with the range from `computeRange(filter)`

  - [x]* 6.3 Write property test for error state propagation (Property 3)
    - **Property 3: Error state propagation without crash**
    - **Validates: Requirements 2.4**
    - Mock OrderRepository to throw random exceptions
    - Verify ViewModel emits UiState with `isLoading = false` and `errorMessage` set, no unhandled exception

  - [x]* 6.4 Write property test for reprint loading state lifecycle (Property 5)
    - **Property 5: Reprint loading state prevents duplicate operations**
    - **Validates: Requirements 7.4**
    - Mock printer operations with random delays
    - Invoke `onReprintTicket`, verify `reprintingOrderId` is set before print and cleared after completion

- [x] 7. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task (1.1, 2.1, 3.1–3.3, 4.1) corresponds to an atomic commit
- The design uses `StatsViewModel.computeRange()` directly—no duplication of time range logic
- `TimeFilter` enum is reused from `com.example.puntodeventa.ui.stats`
- Property tests use Kotest (already in the project) with minimum 100 iterations
- Checkpoints ensure incremental validation before moving to test phase

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3"] },
    { "id": 4, "tasks": ["4.1"] },
    { "id": 5, "tasks": ["6.1", "6.2", "6.3", "6.4"] }
  ]
}
```
