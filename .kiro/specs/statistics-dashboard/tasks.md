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

  - [x] 8.3 Implement the Sales Trend placeholder card ~~(superseded by task 13 — the placeholder was removed in the v2 enterprise upgrade)~~
    - Display a card labeled "Tendencia de ventas" spanning full width
    - Position between MetricCards and the bottom two-column section
    - Display centered placeholder text "Gráfico en construcción"
    - Minimum height of 200dp
    - _Requirements: 8.1, 8.2 (v1)_

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

## Phase 2 — Enterprise Upgrade

Turns the static v1 summary into an analytics dashboard: interactive trend chart, period-over-period
comparison, payment-method breakdown and CSV export. No new Gradle dependency — charts are drawn on
`Canvas` so every color resolves from `MaterialTheme.colorScheme`.

- [ ] 11. Payment method capture (schema + checkout)
  - [ ] 11.1 Create the `PaymentMethod` enum
    - Create `app/src/main/java/com/example/puntodeventa/data/model/PaymentMethod.kt`
    - Values `CASH("EFECTIVO", "Efectivo")`, `CARD("TARJETA", "Tarjeta")`, `TRANSFER("TRANSFERENCIA", "Transferencia")`
    - `companion object fun fromStorage(value: String?): PaymentMethod` falling back to `CASH` for unknown tokens
    - _Requirements: 14.1, 14.9_

  - [ ] 11.2 Add the `paymentMethod` column and migrate the database
    - Add `@ColumnInfo(defaultValue = "EFECTIVO") val paymentMethod: String = PaymentMethod.CASH.storageValue` to `OrderEntity`
    - Bump `AppDatabase` to `version = 5` and register an explicit `Migration(4, 5)` running
      `ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'EFECTIVO'`
    - Keep `fallbackToDestructiveMigration` as the last-resort path only
    - _Requirements: 14.1, 14.2_

  - [ ] 11.3 Capture the payment method at checkout
    - Add `paymentMethod: PaymentMethod = PaymentMethod.CASH` to `CheckoutState`
    - Add `selectPaymentMethod(method)` to `PosViewModel`; relax `isCompletarOrdenEnabled()` so the
      cash-covers-total rule applies only to `PaymentMethod.CASH`
    - Persist the selected method in the `OrderEntity` built by `confirmPayment()`
    - Render a `PaymentMethodPills` row in `CheckoutPanel` above the payment status pills
    - _Requirements: 14.3, 14.4_

- [ ] 12. Data layer for comparison, trend and breakdown
  - [ ] 12.1 Add the query projections
    - `data/model/PeriodSummary.kt` (`totalRevenue`, `orderCount`, `customerCount`, plus an `EMPTY` constant)
    - `data/model/PaymentMethodRevenue.kt` (`paymentMethod`, `totalRevenue`, `orderCount`)
    - `data/model/OrderTotalPoint.kt` (`timestamp`, `amount`)
    - _Requirements: 2.8, 2.9, 2.10_

  - [ ] 12.2 Add the DAO flows and repository delegation
    - `getPeriodSummaryFlow`, `getPaymentMethodBreakdownFlow` (ORDER BY totalRevenue DESC, paymentMethod ASC),
      `getOrderTotalsFlow` (ORDER BY timestamp ASC) in `OrderDao`
    - Thin delegating wrappers in `OrderRepository`
    - _Requirements: 2.8, 2.9, 2.10, 16.1_

  - [ ]* 12.3 Write the payment breakdown property test (Property 18)
    - **Property 18: Payment breakdown totality**
    - Extend `app/src/androidTest/java/com/example/puntodeventa/data/local/StatsQueryPropertyTest.kt`
    - **Validates: Requirements 2.9, 14.5, 14.6, 14.9**

- [ ] 13. Sales trend aggregation and chart
  - [ ] 13.1 Implement `SalesTrendCalculator`
    - Create `ui/stats/SalesTrendCalculator.kt` with `TrendGranularity`, `SalesTrendPoint`,
      `granularityFor(filter, start, end)` and `buildSeries(granularity, start, end, points, zone)`
    - Emit every bucket in range including zero-revenue ones, ascending by `bucketStartMillis`
    - Truncate with `java.time` (`truncatedTo(HOURS)` / `atStartOfDay` / `withDayOfMonth(1)`)
    - _Requirements: 8.3, 8.4, 8.5_

  - [ ] 13.2 Implement the `SalesTrendChart` Canvas composable
    - Create `ui/stats/SalesTrendChart.kt` with `ChartMode` (BAR, LINE)
    - Grid lines, Y labels via `formatCompactCurrency`, thinned X labels, rounded bars / gradient line+area
    - Tap-to-select a bucket with a tooltip showing `fullLabel` + currency; tapping the selection clears it
    - Render "Sin ventas en este periodo" when the series is empty or its max is 0
    - All colors from `MaterialTheme.colorScheme`; text via `rememberTextMeasurer()`
    - _Requirements: 8.1, 8.2, 8.3, 8.6, 8.7, 8.9, 11.7_

  - [ ] 13.3 Replace the placeholder with the chart section
    - Delete `SalesTrendPlaceholder` and the "Gráfico en construcción" text from `StatsScreen`
    - Add the bar/line mode toggle wired to `onChartModeChange`
    - _Requirements: 8.1, 8.2, 8.8, 8.10_

  - [ ]* 13.4 Write trend property tests (Properties 13, 14, 15)
    - **Property 13: Trend granularity selection**
    - **Property 14: Trend series conservation and ordering**
    - **Property 15: Trend series bucket assignment**
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/SalesTrendCalculatorPropertyTest.kt`
    - **Validates: Requirements 8.3, 8.4, 8.5**

- [ ] 14. Period-over-period comparison
  - [ ] 14.1 Implement `MetricDelta` and `computePreviousRange`
    - Create `ui/stats/MetricDelta.kt` with `TrendDirection` and `MetricDelta.of(current, previous, hasComparison)`
    - Add `StatsViewModel.computePreviousRange(filter, start, end): Pair<Long, Long>?` (null for ALL)
    - Never return NaN/Infinity; `previous == 0 && current > 0` → `NO_BASELINE`
    - _Requirements: 13.1, 13.2, 13.4, 13.5, 13.6, 13.7, 13.8, 13.9_

  - [ ] 14.2 Surface the deltas on the metric cards
    - Query the previous `PeriodSummary` in the ViewModel pipeline and expose the four previous values
    - Add a `MetricDeltaChip` (arrow + percentage + "vs periodo anterior") to each `MetricCard`
    - Hide the indicator when the filter has no baseline
    - _Requirements: 13.3, 13.4, 13.10_

  - [ ]* 14.3 Write comparison property tests (Properties 16, 17)
    - **Property 16: Metric delta correctness**
    - **Property 17: Previous range shape**
    - Create `MetricDeltaPropertyTest.kt` and `PreviousRangePropertyTest.kt`
    - **Validates: Requirements 13.1, 13.2, 13.4–13.9**

- [ ] 15. Payment method breakdown section
  - [ ] 15.1 Implement the `PaymentMethodDonut` composable
    - Create `ui/stats/PaymentMethodDonut.kt` drawing one `drawArc` stroke per slice with a 2° gap
    - Themed palette by slice index (primary, tertiary, secondary, outline); total revenue in the center
    - Legend rows with display name, currency revenue and share percentage
    - Render "Sin ventas en este periodo" when period revenue is 0
    - _Requirements: 14.5, 14.6, 14.7, 14.8, 11.7_

  - [ ] 15.2 Place the breakdown beside the trend chart
    - Row with the chart at weight 1.6f and the breakdown at weight 1f
    - Map raw storage tokens through `PaymentMethod.fromStorage`, merging unknown tokens into Efectivo
    - _Requirements: 11.8, 14.9_

- [ ] 16. CSV report export
  - [ ] 16.1 Implement `StatsCsvBuilder`
    - Create `ui/stats/StatsCsvBuilder.kt` with `build(state, generatedAtMillis)` and `escape(field)`
    - UTF-8 BOM; sections RESUMEN / VENTAS POR METODO DE PAGO / TENDENCIA DE VENTAS / PRODUCTOS MAS VENDIDOS
    - Numbers with `Locale.US`, no currency symbol, no thousands separators; empty cell when no baseline
    - _Requirements: 15.4, 15.5, 15.6, 15.7_

  - [ ] 16.2 Wire the export action through the Storage Access Framework
    - Add `StatsFormatters`/`StatsViewModel.exportFileName()` producing `reporte_ventas_yyyyMMdd_HHmmss.csv`
    - Add the "Exportar Reporte" `IconButton` to the top bar, disabled while `isExporting`
    - `rememberLauncherForActivityResult(CreateDocument("text/csv"))` in `StatsScreen`, mirroring
      `ConfigurationScreen`'s JSON export
    - `StatsViewModel.onExportUriReceived(uri, contentResolver)` writes on `Dispatchers.IO` and reports
      "Reporte exportado correctamente" / "Error al exportar: …"
    - Show the message once via Toast and call `clearUserMessage()`
    - _Requirements: 15.1, 15.2, 15.3, 15.8, 15.9, 15.10, 16.2, 16.3_

  - [ ]* 16.3 Write CSV property tests (Properties 19, 20)
    - **Property 19: CSV escaping round-trip**
    - **Property 20: CSV numeric locale independence**
    - Create `app/src/test/java/com/example/puntodeventa/ui/stats/StatsCsvBuilderPropertyTest.kt`
    - **Validates: Requirements 15.6, 15.7**

- [ ] 17. Final checkpoint — build and tests
  - `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug` must pass
  - Manual validation with the user: chart granularity per filter, tap tooltip, bar/line toggle,
    delta arrows, donut shares, and a real CSV export opened in a spreadsheet

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
    { "id": 6, "tasks": ["9.1"] },
    { "id": 7, "tasks": ["11.1", "12.1"] },
    { "id": 8, "tasks": ["11.2", "12.2", "13.1", "14.1", "16.1"] },
    { "id": 9, "tasks": ["11.3", "12.3", "13.2", "13.4", "14.3", "15.1", "16.3"] },
    { "id": 10, "tasks": ["13.3", "14.2", "15.2", "16.2"] },
    { "id": 11, "tasks": ["17"] }
  ]
}
```
