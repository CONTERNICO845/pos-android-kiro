# Requirements Document

## Introduction

The Statistics Dashboard ("Estadísticas") feature provides a comprehensive sales metrics screen that reads from the existing Room database order history. It enables the business owner to visualize revenue, order counts, average ticket size, customer counts, top-selling products, and recent orders — all filterable by time period (Today, Yesterday, This Month, All Time, or a custom date range). The screen replaces the current placeholder StatsScreen with a fully functional data-driven dashboard.

**Enterprise upgrade (v2).** The dashboard was extended from a static summary into an analytics tool: an interactive sales-trend chart whose granularity adapts to the selected period, period-over-period comparison indicators on every metric card, a revenue breakdown by payment method, and CSV export of the current dashboard through the Android Storage Access Framework.

## Glossary

- **Stats_Dashboard**: The full-screen composable that renders the statistics UI with metrics, the sales trend chart, the payment-method breakdown, product rankings, and recent orders.
- **StatsViewModel**: The ViewModel responsible for managing the selected time filter, querying the database for the current and previous periods, building the export payload, and exposing reactive UI state for the Stats_Dashboard.
- **OrderDao**: The Room DAO interface providing query methods against the `orders` and `order_items` tables.
- **ProductSaleSummary**: A data class (POJO) with fields `productName: String`, `totalQuantity: Int`, and `totalRevenue: Double`, returned by aggregation queries.
- **PeriodSummary**: A query projection with fields `totalRevenue: Double`, `orderCount: Int`, and `customerCount: Int` describing one time window, used for both the current and the previous comparison period.
- **PaymentMethod**: The enum of accepted tender types — `CASH` ("Efectivo"), `CARD` ("Tarjeta"), `TRANSFER` ("Transferencia") — persisted on each order as a stable storage value.
- **PaymentMethodRevenue**: A query projection with fields `paymentMethod: String`, `totalRevenue: Double`, and `orderCount: Int` describing revenue for one tender type.
- **Time_Filter**: A selector with five options — "Hoy" (Today), "Ayer" (Yesterday), "Este mes" (This Month), "Todo" (All Time) and "📅 Rango" (Custom) — that determines the date range used for all dashboard queries.
- **Metric_Cards**: A grid of four summary cards showing Revenue, Orders, Average Ticket, and Customers, each with a period-over-period comparison indicator.
- **Sales_Trend_Chart**: The interactive Canvas-rendered chart (bar or line) that plots revenue per time bucket for the selected period.
- **Trend_Bucket**: One aggregation unit of the Sales_Trend_Chart (an hour, a day, or a month) with a label, a bucket start timestamp, and a revenue value.
- **Metric_Delta**: The comparison result between a current-period value and the previous equivalent period, carrying a percentage change and a direction (up, down, flat, or no baseline).
- **Payment_Breakdown**: The donut chart plus legend that shows the share of revenue contributed by each PaymentMethod.
- **Csv_Report**: The exported `.csv` document containing the dashboard summary, the payment-method distribution, the sales trend series, and the top products for the selected period.
- **OrderEntity**: The Room entity representing a completed order with fields including `id`, `timestamp`, `totalAmount`, `status`, `customerName`, and `paymentMethod`.
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
8. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return a single PeriodSummary containing the total revenue, the order count and the distinct customer count for that range, so that the current and the previous comparison period can each be resolved with one query.
9. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return one PaymentMethodRevenue row per distinct `paymentMethod` present in the range, each carrying the sum of `totalAmount` as `totalRevenue` and the count of orders as `orderCount`, ordered by `totalRevenue` descending with `paymentMethod` ascending as a stable tie-breaker.
10. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return the `timestamp` and `totalAmount` of every order in that range ordered by `timestamp` ascending, so that the Sales_Trend_Chart series can be aggregated into Trend_Buckets outside SQL.

### Requirement 3: Time Filter Selection

**User Story:** As a business owner, I want to select a time period filter, so that all dashboard metrics update to reflect only that period.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a segmented selector with exactly five options: "Hoy", "Ayer", "Este mes", "Todo", and "📅 Rango" (custom range).
2. WHEN the screen first loads, THE Stats_Dashboard SHALL default the Time_Filter selection to "Hoy" and visually highlight the "Hoy" segment as the active selection.
3. WHEN the user selects a Time_Filter option, THE Stats_Dashboard SHALL visually highlight the selected segment and THE StatsViewModel SHALL recalculate the start and end timestamps corresponding to the selected period and re-query all metrics (revenue, order count, average ticket, customer count, top products, and recent orders).
4. WHEN "Hoy" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the current day in the device's default time zone and the end timestamp to the current moment.
5. WHEN "Ayer" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the previous day in the device's default time zone and the end timestamp to 23:59:59.999 of the previous day in the device's default time zone.
6. WHEN "Este mes" is selected, THE StatsViewModel SHALL set the start timestamp to midnight (00:00:00.000) of the first day of the current month in the device's default time zone and the end timestamp to the current moment.
7. WHEN "Todo" is selected, THE StatsViewModel SHALL set the start timestamp to 0 (epoch start) and the end timestamp to the current moment.
8. IF the database query triggered by a Time_Filter selection fails, THEN THE StatsViewModel SHALL retain the previously displayed metric values and THE Stats_Dashboard SHALL display an error indication to the user.
9. WHEN "📅 Rango" is selected, THE Stats_Dashboard SHALL open a Material 3 date range picker dialog, and WHEN the user confirms a range THE StatsViewModel SHALL use the selected start timestamp and the selected end date adjusted to 23:59:59.999 as the explicit query range instead of a computed one.
10. WHEN the user dismisses the date range picker without confirming, THE StatsViewModel SHALL keep the previously selected Time_Filter and its range unchanged.

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

### Requirement 8: Interactive Sales Trend Chart

**User Story:** As a business owner, I want to see how my revenue moves inside the selected period, so that I can spot my busiest hours and days instead of only a single total.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display a card labeled "Tendencia de ventas" that spans the full available width, positioned vertically between the Metric_Cards grid and the bottom two-column section (Productos más vendidos / Órdenes recientes), with a chart plot area of at least 180dp in height.
2. THE Stats_Dashboard SHALL NOT display the text "Gráfico en construcción" nor any other placeholder in place of the chart.
3. THE Sales_Trend_Chart SHALL render one visual element (bar or line vertex) per Trend_Bucket, where the element's height is proportional to that bucket's revenue relative to the maximum bucket revenue of the series.
4. THE Sales_Trend_Chart SHALL derive its Trend_Bucket granularity from the selected Time_Filter: hourly buckets for "Hoy" and "Ayer", daily buckets for "Este mes", monthly buckets for "Todo", and for a custom range hourly when the range spans at most 2 days, daily when it spans at most 62 days, and monthly otherwise.
5. THE Sales_Trend_Chart SHALL label the X axis with the labels of its Trend_Buckets — "HH" for hourly buckets, day-of-month for daily buckets, and abbreviated month plus two-digit year for monthly buckets — thinning the rendered labels when the available width cannot fit every bucket label.
6. THE Sales_Trend_Chart SHALL label the Y axis with at least a zero baseline and the maximum bucket value formatted as compact currency (e.g., "$1.2k").
7. WHEN the user taps a Trend_Bucket, THE Sales_Trend_Chart SHALL mark that bucket as selected and display its full label and its revenue formatted as currency; WHEN the user taps the selected bucket again or taps outside any bucket, THE Sales_Trend_Chart SHALL clear the selection.
8. THE Stats_Dashboard SHALL offer a control to switch the Sales_Trend_Chart between bar rendering and line rendering, defaulting to bar rendering.
9. WHEN every Trend_Bucket in the series has a revenue of 0.00 or the series is empty, THE Sales_Trend_Chart SHALL display the message "Sin ventas en este periodo" instead of the plot.
10. WHEN the Time_Filter selection changes, THE Sales_Trend_Chart SHALL rebuild its series and clear any bucket selection.

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
3. THE Stats_Dashboard SHALL position the Time_Filter segmented selector and the "Exportar Reporte" action icon on the right side of the top bar, vertically centered with the title, with the export icon placed after the selector.
4. THE Metric_Cards SHALL use the `BackgroundPrimary` color token as their background and be arranged in a single horizontal row of four equally-sized cards.
5. THE Stats_Dashboard SHALL use the `CardText` color token (white) for all labels and values displayed over the `CardBackground` surface.
6. THE Stats_Dashboard SHALL apply rounded corners with a radius of 12dp to each Metric_Card.
7. THE Sales_Trend_Chart and the Payment_Breakdown SHALL derive every color they draw from the active Material 3 `colorScheme` so that the 9 application themes propagate to the charts without hard-coded color literals.
8. THE Stats_Dashboard SHALL lay the Sales_Trend_Chart and the Payment_Breakdown side by side in a single row, giving the chart the larger share of the width, and SHALL keep them scrollable within the dashboard's vertical scroll container.

### Requirement 12: Reactive State Management

**User Story:** As a developer, I want the dashboard to reactively update when the time filter changes, so that metrics always reflect the selected period without manual refresh.

#### Acceptance Criteria

1. THE StatsViewModel SHALL expose a single StateFlow of UI state containing: selected time filter, the resolved range start and end, total revenue, order count, average ticket, customer count, the previous-period revenue, order count, average ticket and customer count, the Trend_Bucket series with its granularity and chart mode, the PaymentMethodRevenue list, the list of ProductSaleSummary, the list of recent orders, an isLoading boolean flag, an isExporting boolean flag, and a transient user message.
2. WHEN the Time_Filter selection changes, THE StatsViewModel SHALL cancel any in-flight database queries for the previous filter, trigger new database queries for the selected period, and emit the updated UI state to the StateFlow within 2 seconds under normal database load.
3. WHILE the StatsViewModel is loading data, THE Stats_Dashboard SHALL display the previously loaded values unchanged and set the isLoading flag to true so the UI can optionally indicate a loading state, without blocking user interaction with the Time_Filter selector.
4. IF a database query fails during a Time_Filter change, THEN THE StatsViewModel SHALL retain the previously loaded values in the UI state and expose an error message indicating the failure reason.


### Requirement 13: Period-over-Period Comparison

**User Story:** As a business owner, I want each metric compared against the previous equivalent period, so that I can tell whether the business is improving without doing the math myself.

#### Acceptance Criteria

1. THE StatsViewModel SHALL compute a previous comparison range for the selected Time_Filter as follows: "Hoy" → the same clock window of the previous day, "Ayer" → the full previous-to-yesterday day, "Este mes" → the equal-length window starting at midnight of the first day of the previous month, and a custom range → the immediately preceding window of the same duration.
2. WHEN the selected Time_Filter is "Todo", THE StatsViewModel SHALL NOT compute a previous comparison range, and THE Metric_Cards SHALL omit their comparison indicators.
3. THE StatsViewModel SHALL query the PeriodSummary of the previous comparison range and expose the previous revenue, previous order count, previous average ticket and previous customer count in the UI state.
4. THE Stats_Dashboard SHALL display on each Metric_Card a Metric_Delta indicator composed of a direction arrow and the percentage change relative to the previous period, computed as `(current - previous) / previous * 100` and rounded to one decimal place.
5. WHEN a Metric_Delta percentage is greater than 0, THE Metric_Card SHALL render the indicator with an upward arrow and a positive-trend color, and the label SHALL be prefixed with "+".
6. WHEN a Metric_Delta percentage is less than 0, THE Metric_Card SHALL render the indicator with a downward arrow and a negative-trend color.
7. WHEN the current and previous values are equal, THE Metric_Card SHALL render a neutral indicator reading "0.0%".
8. IF the previous period value is 0 and the current value is greater than 0, THEN THE Metric_Card SHALL render the indicator as "Nuevo" with the positive-trend styling instead of an undefined percentage.
9. IF both the previous and the current value are 0, THEN THE Metric_Card SHALL render the neutral "0.0%" indicator.
10. THE Metric_Card SHALL append the comparison scope caption "vs periodo anterior" so that the baseline of the percentage is unambiguous.

### Requirement 14: Payment Method Capture and Breakdown

**User Story:** As a business owner, I want to know how much of my revenue arrives as cash, card, or transfer, so that I can reconcile my drawer and my bank deposits.

#### Acceptance Criteria

1. THE OrderEntity SHALL persist a non-null `paymentMethod` column whose value is one of the PaymentMethod storage values `EFECTIVO`, `TARJETA`, or `TRANSFERENCIA`, defaulting to `EFECTIVO`.
2. THE AppDatabase SHALL migrate existing installations to the new schema version by adding the `paymentMethod` column with the default value `EFECTIVO` for already-stored orders, without deleting existing order history.
3. THE Checkout panel SHALL display a mutually exclusive selector of the three PaymentMethod options labeled "Efectivo", "Tarjeta" and "Transferencia", defaulting to "Efectivo", and THE selected method SHALL be persisted with the order.
4. WHEN the selected PaymentMethod is not "Efectivo", THE Checkout SHALL NOT require cash received to reach the cart total in order to enable "Completar Orden".
5. THE Stats_Dashboard SHALL display a section labeled "Ventas por método de pago" showing a donut chart whose arc sweep for each PaymentMethod is proportional to that method's share of the period revenue.
6. THE Payment_Breakdown SHALL display a legend listing, for each PaymentMethod present in the period, its display name, its revenue formatted as currency, and its share of total revenue formatted as a percentage with one decimal place.
7. THE Payment_Breakdown SHALL display the total revenue of the period in the center of the donut.
8. WHEN no revenue exists for the selected period, THE Payment_Breakdown SHALL display the message "Sin ventas en este periodo" instead of the donut and legend.
9. WHEN an order carries a `paymentMethod` value that does not match any known PaymentMethod storage value, THE Payment_Breakdown SHALL classify it as "Efectivo" rather than discarding its revenue.

### Requirement 15: Export Report to CSV

**User Story:** As a business owner, I want to export the dashboard to a CSV file, so that I can archive it or open it in a spreadsheet for my accountant.

#### Acceptance Criteria

1. THE Stats_Dashboard SHALL display an "Exportar Reporte" action icon in the top bar.
2. WHEN the user activates the export action, THE Stats_Dashboard SHALL launch the Android Storage Access Framework `CreateDocument` contract with the MIME type `text/csv` and a suggested file name containing the prefix `reporte_ventas_` and a `yyyyMMdd_HHmmss` timestamp with the `.csv` extension.
3. WHEN the user confirms a destination, THE StatsViewModel SHALL write the Csv_Report to the returned Uri through the content resolver on a background dispatcher.
4. THE Csv_Report SHALL be encoded as UTF-8 with a byte-order mark so that accented Spanish text renders correctly in spreadsheet applications.
5. THE Csv_Report SHALL contain a header block with the report title, the selected period label and the exported range as human-readable dates, followed by four labeled sections: "RESUMEN" with revenue, orders, average ticket, customers and each metric's previous-period value and percentage change; "VENTAS POR METODO DE PAGO" with one row per method including revenue, order count and share; "TENDENCIA DE VENTAS" with one row per Trend_Bucket including its label and revenue; and "PRODUCTOS MAS VENDIDOS" with one row per product including quantity and revenue.
6. THE Csv_Report SHALL quote any field containing a comma, a double quote, or a line break, escaping embedded double quotes by doubling them, so that the document is parseable per RFC 4180.
7. THE Csv_Report SHALL write monetary and percentage values as plain unformatted decimal numbers with a period as decimal separator and no currency symbol or thousands separator, so that spreadsheets parse them as numbers.
8. WHEN the export completes successfully, THE Stats_Dashboard SHALL show the confirmation message "Reporte exportado correctamente".
9. IF writing the file fails or the destination Uri cannot be opened, THEN THE StatsViewModel SHALL expose an error message beginning with "Error al exportar" and THE Stats_Dashboard SHALL surface it without crashing.
10. WHEN the user cancels the Storage Access Framework picker, THE Stats_Dashboard SHALL take no action and show no message.

### Requirement 16: Dashboard Refresh and Loading Feedback

**User Story:** As a business owner, I want the dashboard to always reflect stored orders and tell me when it is busy, so that I trust what I am reading.

#### Acceptance Criteria

1. THE StatsViewModel SHALL source every dashboard value from reactive Room queries so that an order persisted while the Stats_Dashboard is visible updates the metrics, the chart, the payment breakdown and the lists without user interaction.
2. WHILE an export is in progress, THE Stats_Dashboard SHALL disable the "Exportar Reporte" action so the same report cannot be written twice concurrently.
3. WHEN the StatsViewModel emits a non-null message, THE Stats_Dashboard SHALL display it once and THEN request the StatsViewModel to clear it, so that it is not shown again on recomposition or configuration change.
