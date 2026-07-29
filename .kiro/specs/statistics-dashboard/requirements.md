# Requirements Document

## Introduction

The Statistics Dashboard ("Estadísticas") feature provides a comprehensive sales metrics screen that reads from the existing Room database order history. It enables the business owner to visualize revenue, order counts, average ticket size, customer counts, top-selling products, and recent orders — all filterable by time period (Today, Yesterday, This Month, All Time). The screen replaces the current placeholder StatsScreen with a fully functional data-driven dashboard.

## Glossary

- **Stats_Dashboard**: The full-screen composable that renders the statistics UI with metrics, product rankings, and recent orders.
- **StatsViewModel**: The ViewModel responsible for managing the selected time filter, querying the database, and exposing reactive UI state for the Stats_Dashboard.
- **OrderDao**: The Room DAO interface providing query methods against the `orders` and `order_items` tables.
- **ProductSaleSummary**: A data class (POJO) with fields `productName: String`, `totalQuantity: Int`, and `totalRevenue: Double`, returned by aggregation queries.
- **Time_Filter**: A selector with four options — "Hoy" (Today), "Ayer" (Yesterday), "Este mes" (This Month), and "Todo" (All Time) — that determines the date range used for all dashboard queries.
- **Metric_Cards**: A grid of four summary cards showing Revenue, Orders, Average Ticket, and Customers.
- **OrderEntity**: The Room entity representing a completed order with fields including `id`, `timestamp`, `totalAmount`, `status`, and `customerName`.
- **OrderItemEntity**: The Room entity representing a line item within an order, with fields including `productName`, `quantity`, and `totalPrice`.

## Requirements

### Requirement 1: Product Sale Summary Data Model

**User Story:** As a developer, I want a dedicated data model for aggregated product sales, so that Room queries can return typed results for the dashboard.

#### Acceptance Criteria

1. THE ProductSaleSummary SHALL be declared as a Kotlin `data class` containing exactly three fields: `productName` of type String (non-null), `totalQuantity` of type Int (non-null, minimum value 0), and `totalRevenue` of type Double (non-null, minimum value 0.00).
2. THE ProductSaleSummary SHALL NOT carry an `@Entity` annotation, so that Room uses it solely as a query-result projection without creating a database table.
3. THE ProductSaleSummary field names SHALL match the column aliases used in the OrderDao aggregation queries (`productName`, `totalQuantity`, `totalRevenue`) so that Room can map query results by constructor parameter name.

### Requirement 2: Date-Filtered Order Queries

**User Story:** As a business owner, I want to query orders within a specific date range, so that I can view metrics for different time periods.

#### Acceptance Criteria

1. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return only orders whose `timestamp` field falls within the inclusive range [startTimestamp, endTimestamp] and whose `status` is "COMPLETED".
2. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return the total revenue as the sum of `totalAmount` for all orders with status "COMPLETED" within that range, returning 0.0 when no matching orders exist.
3. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return the count of orders with status "COMPLETED" within that range, returning 0 when no matching orders exist.
4. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return the count of distinct non-null `customerName` values from orders with status "COMPLETED" within that range, returning 0 when no matching orders exist.
5. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return a list of ProductSaleSummary (limited to a maximum of 50 entries) by joining `orders` and `order_items` tables, grouping by `productName`, summing `quantity` as `totalQuantity` and `totalPrice` as `totalRevenue`, filtered to orders with status "COMPLETED" within that range, ordered by `totalQuantity` descending.
6. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return the most recent orders with status "COMPLETED" within that range, ordered by `timestamp` descending, limited to a maximum of 20 results.
7. IF no orders with status "COMPLETED" exist within the provided range, THEN THE OrderDao SHALL return an empty list for order queries and ProductSaleSummary queries.

### Requirement 3: Time Filter Selection

**User Story:** As a business owner, I want to select a time period filter, so that all dashboard metrics update to reflect only that period.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a segmented selector with exactly four options: "Hoy", "Ayer", "Este mes", and "Todo".
2. WHEN the screen first loads, THE Stats_Dashboard SHALL default the Time_Filter selection to "Hoy" and visually highlight the "Hoy" segment as the active selection.
3. WHEN the user selects a Time_Filter option, THE Stats_Dashboard SHALL visually highlight the selected segment and THE StatsViewModel SHALL recalculate the start and end timestamps corresponding to the selected period and re-query all metrics (revenue, order count, average ticket, customer count, top products, and recent orders).
4. WHEN "Hoy" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the current day in the device's default time zone and the end timestamp to the current moment.
5. WHEN "Ayer" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the previous day in the device's default time zone and the end timestamp to 23:59:59.999 of the previous day in the device's default time zone.
6. WHEN "Este mes" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the first day of the current month in the device's default time zone and the end timestamp to the current moment.
7. WHEN "Todo" is selected, THE StatsViewModel SHALL set the start timestamp to 0 (epoch start) and the end timestamp to the current moment.
8. IF the database query triggered by a Time_Filter selection fails, THEN THE StatsViewModel SHALL retain the previously displayed metric values and THE Stats_Dashboard SHALL display an error indication to the user.

### Requirement 4: Revenue Metric Card

**User Story:** As a business owner, I want to see my total revenue for the selected period, so that I can track income at a glance.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a metric card labeled "INGRESOS" showing the sum of `totalAmount` for all orders with status "COMPLETED" in the selected time range.
2. THE Stats_Dashboard SHALL format the revenue value as currency with a "$" prefix, comma as thousands grouping separator, and exactly two decimal places (e.g., "$0.00", "$999.99", "$1,234.56", "$1,000,000.00").
3. WHEN no completed orders exist in the selected range, THE Stats_Dashboard SHALL display "$0.00" for the INGRESOS card.
4. WHEN the time filter selection changes, THE Stats_Dashboard SHALL update the INGRESOS card value to reflect the recalculated revenue for the newly selected time range.

### Requirement 5: Orders Count Metric Card

**User Story:** As a business owner, I want to see the total number of orders for the selected period, so that I can understand sales volume.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a metric card labeled "ÓRDENES" showing the count of completed orders in the selected time range, formatted as a whole number with locale-aware thousand separators (e.g., "1,234").
2. IF no completed orders exist in the selected range, THEN THE Stats_Dashboard SHALL display "0" for the ÓRDENES card.
3. WHEN the user selects a different Time_Filter option, THE Stats_Dashboard SHALL update the ÓRDENES card value to reflect only orders with status "COMPLETED" whose timestamp falls within the newly selected range.

### Requirement 6: Average Ticket Metric Card

**User Story:** As a business owner, I want to see the average ticket value, so that I can understand typical order sizes.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a metric card labeled "TICKET PROMEDIO" showing the result of total revenue divided by total completed order count for the selected time range, rounded half-up to two decimal places.
2. THE Stats_Dashboard SHALL format the average ticket value as currency with a "$" prefix, comma thousand separators, and exactly two decimal places (e.g., "$1,234.56").
3. IF the completed order count for the selected time range is zero, THEN THE Stats_Dashboard SHALL display "$0.00" for the TICKET PROMEDIO card.

### Requirement 7: Customers Metric Card

**User Story:** As a business owner, I want to see how many unique customers I served, so that I can gauge my customer base.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a metric card labeled "CLIENTES" showing the count of distinct non-null `customerName` values from completed orders in the selected time range, formatted as a whole number without decimal places (e.g., "5", "128").
2. WHEN no completed orders with non-null `customerName` exist in the selected range, THE Stats_Dashboard SHALL display "0" for the CLIENTES card.
3. WHEN counting distinct customers, THE Stats_Dashboard SHALL treat `customerName` values as case-sensitive (e.g., "Juan" and "juan" count as two distinct customers).

### Requirement 8: Sales Trend Placeholder

**User Story:** As a business owner, I want a placeholder for the sales trend chart, so that the layout is prepared for future chart implementation.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a card labeled "Tendencia de ventas" that spans the full available width, positioned vertically between the Metric_Cards grid and the bottom two-column section (Productos más vendidos / Órdenes recientes).
2. THE Stats_Dashboard SHALL display placeholder text "Gráfico en construcción" centered inside the sales trend card, with the card having a minimum height of 200dp to reserve visible space for the future chart.

### Requirement 9: Top Products List

**User Story:** As a business owner, I want to see my best-selling products ranked by quantity sold, so that I can identify popular items.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a section labeled "Productos más vendidos" in the bottom-left area.
2. THE Stats_Dashboard SHALL render a scrollable list of all ProductSaleSummary items returned by the query, ordered by totalQuantity descending, using a LazyColumn.
3. WHEN rendering each product row, THE Stats_Dashboard SHALL display the product name (truncated with ellipsis if it exceeds 1 line), the quantity sold formatted as "{quantity} vendidos", and the revenue formatted as "${amount}" with two decimal places (e.g., "$1,234.56").
4. WHEN no product sales exist in the selected range, THE Stats_Dashboard SHALL display an empty state message "Sin datos para este periodo" in place of the list.
5. IF two or more products have the same totalQuantity, THEN THE Stats_Dashboard SHALL preserve the order returned by the database query without additional sorting.

### Requirement 10: Recent Orders List

**User Story:** As a business owner, I want to see my most recent orders, so that I can quickly review recent activity.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a section labeled "Órdenes recientes" in the bottom-right area.
2. THE Stats_Dashboard SHALL render a scrollable list of the most recent orders with status "COMPLETED" (maximum 20) within the selected time range, ordered by timestamp descending.
3. WHEN rendering each order row, THE Stats_Dashboard SHALL display the order time formatted as "HH:mm" in the device's local timezone, the customer name truncated to a single line with ellipsis if it exceeds the available width (or "Cliente anónimo" when customerName is null or blank), and the total amount formatted with a "$" prefix and two decimal places (e.g., "$150.00").
4. IF no orders with status "COMPLETED" exist in the selected range, THEN THE Stats_Dashboard SHALL display the empty state message "Sin órdenes para este periodo" in place of the list.

### Requirement 11: Dashboard Layout and Styling

**User Story:** As a business owner, I want the statistics screen to be visually consistent with the app's design language, so that the experience feels cohesive.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL use the `CardBackground` color token as its full-screen background color.
2. THE Stats_Dashboard SHALL display a top bar with the title "Estadísticas" in bold and the subtitle "Resumen de ventas y métricas" in regular weight, both left-aligned within the top bar.
3. THE Stats_Dashboard SHALL position the Time_Filter segmented selector on the right side of the top bar, vertically centered with the title.
4. THE Metric_Cards SHALL use the `BackgroundPrimary` color token as their background and be arranged in a single horizontal row of four equally-sized cards.
5. THE Stats_Dashboard SHALL use the `CardText` color token (white) for all labels and values displayed over the `CardBackground` surface.
6. THE Stats_Dashboard SHALL apply rounded corners with a radius of 12dp to each Metric_Card.

### Requirement 12: Reactive State Management

**User Story:** As a developer, I want the dashboard to reactively update when the time filter changes, so that metrics always reflect the selected period without manual refresh.

#### Acceptance Criteria

1. THE StatsViewModel SHALL expose a single StateFlow of UI state containing: selected time filter, total revenue, order count, average ticket, customer count, list of ProductSaleSummary, list of recent orders, and an isLoading boolean flag.
2. WHEN the Time_Filter selection changes, THE StatsViewModel SHALL cancel any in-flight database queries for the previous filter, trigger new database queries for the selected period, and emit the updated UI state to the StateFlow within 2 seconds under normal database load.
3. WHILE the StatsViewModel is loading data, THE Stats_Dashboard SHALL display the previously loaded values unchanged and set the isLoading flag to true so the UI can optionally indicate a loading state, without blocking user interaction with the Time_Filter selector.
4. IF a database query fails during a Time_Filter change, THEN THE StatsViewModel SHALL retain the previously loaded values in the UI state and expose an error message indicating the failure reason.
