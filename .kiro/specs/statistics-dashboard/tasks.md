# Implementation Plan: Statistics Dashboard

## Overview

Replace the placeholder `StatsScreen` with a fully functional sales metrics dashboard. The implementation follows the existing MVVM + UDF architecture, adds new Room DAO queries, a dedicated ViewModel with reactive state, formatting utilities, and a Compose UI composed of metric cards, a sales trend placeholder, top products list, and recent orders list — all filterable by time period.

## Tasks

- [x] 1. Create data models and enums
  - [x] 1.1 Create the `TimeFilter` enum
    - Create `app/src/main/java/com/example/puntodeventa/ui/stats/TimeFilter.kt`
    - Define enum with four values: `TODAY("Hoy")`, `YESTERDAY("Ayer")`, `THIS_MONTH("Este mes")`, `ALL("Todo")`
    - Each value has a `label: String` property for display text
    - _Requirements: 3.1_

  - [x] 1.2 Create the `ProductSaleSummary` data class
    - Create `app/src/main/java/com/example/puntodeventa/data/model/ProductSaleSummary.kt`
    - Define `data class ProductSaleSummary(val productName: String, val totalQuantity: Int, val totalRevenue: Double)`
    - No `@Entity` annotation — used purely as a Room query-result projection
    - Field names must match column aliases in DAO queries: `productName`, `totalQuantity`, `totalRevenue`
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.3 Create the `StatsUiState` data class
    - Create `app/src/main/java/com/example/puntodeventa/ui/stats/StatsUiState.kt`
    - Fields: `selectedFilter: TimeFilter = TimeFilter.TODAY`, `totalRevenue: Double = 0.0`, `orderCount: Int = 0`, `averageTicket: Double = 0.0`, `customerCount: Int = 0`, `topProducts: List<ProductSaleSummary> = emptyList()`, `recentOrders: List<OrderEntity> = emptyList()`, `isLoading: Boolean = false`, `errorMessage: String? = null`
    - _Requirements: 12.1_

- [x] 2. Implement OrderDao query extensions
  - [x] 2.1 Add date-filtered query methods to `OrderDao`
    - Add `getRecentOrders(start: Long, end: Long): List<OrderEntity>` — returns up to 20 COMPLETED orders in [start, end] ordered by timestamp DESC
    - Add `getTotalRevenue(start: Long, end: Long): Double` — SUM of totalAmount for COMPLETED orders in range, COALESCE to 0.0
    - Add `getOrderCount(start: Long, end: Long): Int` — COUNT of COMPLETED orders in range
    - Add `getCustomerCount(start: Long, end: Long): Int` — COUNT(DISTINCT customerName) where customerName IS NOT NULL, COMPLETED, in range
    - Add `getTopProducts(start: Long, end: Long): List<ProductSaleSummary>` — JOIN orders + order_items, GROUP BY productName, SUM quantity and totalPrice, ORDER BY totalQuantity DESC, LIMIT 50
    - All methods are `suspend` and annotated with `@Query`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x]* 2.2 Write property tests for DAO queries (Properties 2–7)
    - **Property 2: Order filtering by status and timestamp range**
    - **Property 3: Revenue aggregation correctness**
    - **Property 4: Order count aggregation correctness**
    - **Property 5: Distinct customer count with case sensitivity**
    - **Property 6: Top products aggregation and ordering**
    - **Property 7: Recent orders ordering and limit**
    - Create `app/src/androidTest/java/com/example/puntodeventa/data/local/StatsQueryPropertyTest.kt`
    - Use Kotest Property to generate random OrderEntity and OrderItemEntity sets with varying timestamps, statuses, and customerNames
    - Run queries against in-memory Room database and verify results match expected manual computation
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 7.3**

- [x] 3. Implement OrderRepository extensions
  - [x] 3.1 Add statistics query methods to `OrderRepository`
    - Add `suspend fun getRecentOrders(start: Long, end: Long)` delegating to `orderDao.getRecentOrders(start, end)`
    - Add `suspend fun getTotalRevenue(start: Long, end: Long)` delegating to `orderDao.getTotalRevenue(start, end)`
    - Add `suspend fun getOrderCount(start: Long, end: Long)` delegating to `orderDao.getOrderCount(start, end)`
    - Add `suspend fun getCustomerCount(start: Long, end: Long)` delegating to `orderDao.getCustomerCount(start, end)`
    - Add `suspend fun getTopProducts(start: Long, end: Long)` delegating to `orderDao.getTopProducts(start, end)`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 4. Implement formatting utilities
  - [x] 4.1 Create `StatsFormatters` utility object
    - Create `app/src/main/java/com/example/puntodeventa/ui/stats/StatsFormatters.kt`
    - Implement `formatCurrency(amount: Double): String` — "$" prefix, comma thousands separator, exactly 2 decimal places (e.g., "$1,234.56", "$0.00")
    - Implement `formatCount(count: Int): String` — locale-aware thousand separators, no decimal (e.g., "1,234")
    - Implement `formatOrderTime(timestamp: Long): String` — "HH:mm" format in device local timezone
    - Implement `formatQuantitySold(quantity: Int): String` — returns "${quantity} vendidos"
    - Implement `displayCustomerName(customerName: String?): String` — returns "Cliente anónimo" if null/blank, otherwise the original name
    - _Requirements: 4.2, 5.1, 6.2, 9.3, 10.3_

  - [x]* 4.2 Write property tests for formatters (Properties 8, 9, 11, 12)
    - **Property 8: Currency formatting**
    - **Property 9: Integer count formatting**
    - **Property 11: Product sale row formatting**
    - **Property 12: Order row customer name display**
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/FormattersPropertyTest.kt`
    - Use Kotest Property to generate random Doubles, Ints, ProductSaleSummary instances, and OrderEntity with null/blank/valid customerName
    - **Validates: Requirements 4.2, 5.1, 6.2, 9.3, 10.3**

- [x] 5. Checkpoint - Ensure data layer compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement StatsViewModel
  - [x] 6.1 Implement `StatsViewModel` with time range computation and reactive state
    - Create `app/src/main/java/com/example/puntodeventa/ui/stats/StatsViewModel.kt`
    - Expose `val uiState: StateFlow<StatsUiState>` backed by `MutableStateFlow`
    - Implement `fun onFilterChange(filter: TimeFilter)` that updates selectedFilter, cancels in-flight job, and triggers `loadStats`
    - Implement `private fun loadStats(filter: TimeFilter)` with parallel async queries via `coroutineScope`
    - Implement `computeRange(filter: TimeFilter): Pair<Long, Long>` as companion function computing start/end timestamps based on device timezone:
      - TODAY → midnight today to now
      - YESTERDAY → midnight yesterday to 23:59:59.999 yesterday
      - THIS_MONTH → midnight 1st of month to now
      - ALL → 0 to now
    - Compute averageTicket as `if (count > 0) revenue / count else 0.0`
    - Catch exceptions (except CancellationException) and set errorMessage while retaining previous values
    - Call `loadStats(TimeFilter.TODAY)` in `init`
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 6.1, 6.3, 12.1, 12.2, 12.3, 12.4_

  - [x] 6.2 Create `StatsViewModelFactory`
    - Follow the existing pattern from `PosViewModel` for ViewModelProvider.Factory
    - Accept `OrderRepository` as constructor parameter
    - _Requirements: 12.1_

  - [x]* 6.3 Write property test for time range computation (Property 1)
    - **Property 1: Time range computation correctness**
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/TimeRangePropertyTest.kt`
    - Generate random Long instants as "now", verify start <= end, start >= 0, and correct boundary values for all 4 filters
    - **Validates: Requirements 3.4, 3.5, 3.6, 3.7**

  - [x]* 6.4 Write property test for average ticket computation (Property 10)
    - **Property 10: Average ticket computation**
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/AverageTicketPropertyTest.kt`
    - Generate random (revenue, count) pairs; verify division and zero-guard
    - **Validates: Requirements 6.1, 6.3**

  - [x]* 6.5 Write unit tests for StatsViewModel
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/StatsViewModelTest.kt`
    - Test initial state defaults to TODAY with zero/empty values
    - Test onFilterChange dispatches new queries and updates state
    - Test error handling retains previous state and sets errorMessage
    - Test cancellation on rapid filter changes
    - _Requirements: 3.2, 3.8, 12.2, 12.3, 12.4_

- [x] 7. Checkpoint - Ensure ViewModel and data layer tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement StatsScreen UI
  - [x] 8.1 Implement the top bar with title, subtitle, and TimeFilter selector
    - Create or replace `app/src/main/java/com/example/puntodeventa/ui/stats/StatsScreen.kt`
    - Top bar displays "Estadísticas" in bold and "Resumen de ventas y métricas" in regular weight, left-aligned
    - TimeFilter segmented selector on the right side, vertically centered with title
    - Highlight the selected filter segment visually
    - Default selection is "Hoy" on first load
    - Emit `onFilterChange(TimeFilter)` when user taps a segment
    - Use `CardBackground` color token for screen background, `CardText` for text
    - _Requirements: 3.1, 3.2, 3.3, 11.1, 11.2, 11.3, 11.5_

  - [x] 8.2 Implement the MetricCards row
    - Display four equally-sized cards in a horizontal row: INGRESOS, ÓRDENES, TICKET PROMEDIO, CLIENTES
    - Use `BackgroundPrimary` color token as card background, `CardText` for text
    - Apply 12dp rounded corners to each card
    - INGRESOS: display `StatsFormatters.formatCurrency(totalRevenue)`, "$0.00" when zero
    - ÓRDENES: display `StatsFormatters.formatCount(orderCount)`, "0" when zero
    - TICKET PROMEDIO: display `StatsFormatters.formatCurrency(averageTicket)`, "$0.00" when zero
    - CLIENTES: display `customerCount` as whole number without decimal
    - Update all cards when time filter changes
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 7.1, 7.2, 11.4, 11.5, 11.6_

  - [x] 8.3 Implement the Sales Trend placeholder card
    - Display a card labeled "Tendencia de ventas" spanning full width
    - Position between MetricCards and the bottom two-column section
    - Display centered placeholder text "Gráfico en construcción"
    - Minimum height of 200dp
    - _Requirements: 8.1, 8.2_

  - [x] 8.4 Implement the Top Products list (left column)
    - Display section labeled "Productos más vendidos" in bottom-left area
    - Render a `LazyColumn` with all ProductSaleSummary items ordered by totalQuantity descending
    - Each row: product name (truncated with ellipsis if >1 line), "{quantity} vendidos", "$revenue" with 2 decimal places
    - Show "Sin datos para este periodo" when list is empty
    - Preserve database order for ties in totalQuantity
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 8.5 Implement the Recent Orders list (right column)
    - Display section labeled "Órdenes recientes" in bottom-right area
    - Render a `LazyColumn` with up to 20 recent COMPLETED orders, ordered by timestamp descending
    - Each row: time formatted as "HH:mm", customer name (truncated, "Cliente anónimo" if null/blank), total with "$" prefix and 2 decimal places
    - Show "Sin órdenes para este periodo" when list is empty
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 9. Wire StatsScreen into navigation
  - [x] 9.1 Connect StatsScreen to the existing navigation graph
    - Replace the placeholder StatsScreen with the new implementation
    - Instantiate `StatsViewModel` via `StatsViewModelFactory` using `OrderRepository` from `AppDatabase.getInstance(context)`
    - Collect `uiState` via `collectAsStateWithLifecycle()` and pass to composable
    - _Requirements: 12.1, 12.2, 12.3_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project already has `kotest-property` configured — no new test dependencies needed
- All formatting uses "$" prefix with commas as thousand separators per the requirements (Spanish locale formatting for the app, but currency symbol is "$")

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "3.1", "4.2"] },
    { "id": 3, "tasks": ["6.1", "6.2"] },
    { "id": 4, "tasks": ["6.3", "6.4", "6.5"] },
    { "id": 5, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5"] },
    { "id": 6, "tasks": ["9.1"] }
  ]
}
```
