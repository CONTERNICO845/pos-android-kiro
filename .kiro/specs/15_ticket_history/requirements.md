# Requirements Document

## Introduction

Pantalla "Historial de Tickets" que permite al usuario visualizar todos los tickets generados, filtrados por fecha. La pantalla reemplaza el placeholder actual de la ruta `tickets` en el NavHost, mostrando cada orden como una tarjeta estilo recibo térmico con texto monoespaciado. Incluye funcionalidad de reimpresión.

## Glossary

- **Ticket_History_Screen**: Composable principal que muestra la lista de tickets históricos filtrados por rango de tiempo.
- **TicketHistoryViewModel**: ViewModel que gestiona el estado de la pantalla, incluyendo el filtro de tiempo seleccionado y la lista de órdenes observada desde la base de datos.
- **OrderDao**: Data Access Object de Room que provee queries para obtener órdenes filtradas por rango de timestamps.
- **OrderRepository**: Capa de repositorio que expone las operaciones del OrderDao al ViewModel.
- **TimeFilter**: Enum con las opciones de filtrado temporal: Hoy, Ayer, Este mes, Todo.
- **Ticket_Card**: Composable Card individual que renderiza el texto del ticket de una orden con estilo de recibo térmico.
- **Filter_Selector**: Componente UI (SegmentedButton o Pills) que permite cambiar el filtro de tiempo activo.
- **Reprint_Button**: Botón en cada tarjeta que invoca la función de impresión TCP o registra la acción en log.

## Requirements

### Requirement 1: Consulta de órdenes por rango de tiempo

**User Story:** As a cashier, I want to query orders filtered by a time range, so that I can retrieve tickets for a specific period.

#### Acceptance Criteria

1. WHEN a start timestamp and end timestamp are provided, THE OrderDao SHALL return all OrderEntity records with status 'COMPLETED' whose timestamp falls within the inclusive range [startTimestamp, endTimestamp], ordered by timestamp descending.
2. THE OrderDao SHALL return an empty list when no orders exist within the specified time range.
3. WHEN multiple orders share the same timestamp, THE OrderDao SHALL return them in a stable order determined by the database engine.

### Requirement 2: ViewModel con filtro de tiempo

**User Story:** As a cashier, I want to select a time filter (Hoy, Ayer, Este mes, Todo), so that I can quickly see tickets from the desired period.

#### Acceptance Criteria

1. WHEN the TicketHistoryViewModel is initialized, THE TicketHistoryViewModel SHALL load orders using the TimeFilter.TODAY filter as the default.
2. WHEN the user selects a different TimeFilter option, THE TicketHistoryViewModel SHALL recompute the time range and emit the updated list of orders matching that range.
3. THE TicketHistoryViewModel SHALL expose a StateFlow containing the current list of OrderEntity objects and the selected TimeFilter.
4. WHEN an error occurs during database query, THE TicketHistoryViewModel SHALL emit an error state without crashing the application.

### Requirement 3: Navegación a la pantalla de historial

**User Story:** As a user, I want to tap the existing "Tickets" option in the navigation rail, so that I can access the ticket history screen.

#### Acceptance Criteria

1. WHEN the user selects the "Tickets" destination in the NavRail, THE Navigation_System SHALL display the Ticket_History_Screen in the main content area.
2. THE Navigation_System SHALL NOT add any new icon or destination to the NavRail.
3. THE Ticket_History_Screen SHALL replace the current placeholder TicketsScreen composable.

### Requirement 4: Barra superior con título y selector de filtro

**User Story:** As a user, I want to see a title bar with filter options, so that I can identify the screen and switch between time periods.

#### Acceptance Criteria

1. THE Ticket_History_Screen SHALL display the title "Historial de Tickets" in the top bar area.
2. THE Filter_Selector SHALL display four options: "Hoy", "Ayer", "Este mes", "Todo" using a SegmentedButton or pill-style component.
3. THE Filter_Selector SHALL be positioned to the right of the title in the top bar.
4. WHEN the user taps a filter option, THE Filter_Selector SHALL visually indicate the active selection and notify the ViewModel of the change.

### Requirement 5: Lista principal de tickets

**User Story:** As a cashier, I want to see all tickets for the selected period in a scrollable list, so that I can review past transactions.

#### Acceptance Criteria

1. THE Ticket_History_Screen SHALL display orders in a vertically scrollable layout (LazyColumn or LazyVerticalGrid).
2. WHEN orders exist for the selected filter, THE Ticket_History_Screen SHALL render one Ticket_Card per order.
3. WHEN no orders exist for the selected filter, THE Ticket_History_Screen SHALL display a message indicating that no tickets are available for the selected period.
4. THE Ticket_History_Screen SHALL display orders sorted from newest to oldest (timestamp descending).

### Requirement 6: Diseño de tarjeta estilo recibo térmico

**User Story:** As a cashier, I want each ticket displayed like a real thermal receipt, so that I can quickly recognize and read the ticket content.

#### Acceptance Criteria

1. THE Ticket_Card SHALL use a Card composable with a pure white background (Color.White) and slight elevation to simulate paper.
2. THE Ticket_Card SHALL render the clientTicketText field from the OrderEntity inside the card body.
3. THE Ticket_Card SHALL use FontFamily.Monospace for the ticket text to ensure columns, spaces, and dashes align correctly.
4. THE Ticket_Card SHALL use a font size of 12.sp for the ticket text.
5. IF the clientTicketText field is null or empty, THEN THE Ticket_Card SHALL display a placeholder message indicating that no ticket text is available.

### Requirement 7: Botón de reimpresión

**User Story:** As a cashier, I want a reprint button on each ticket card, so that I can reprint a past ticket when needed.

#### Acceptance Criteria

1. THE Ticket_Card SHALL display an outlined button with the label "Reimprimir Ticket" at the bottom of the card.
2. WHEN the user taps the Reprint_Button, THE Ticket_History_Screen SHALL invoke the TCP print function if a printer connection is available.
3. IF no printer connection is available, THEN THE Ticket_History_Screen SHALL log the reprint action for future implementation.
4. WHILE a reprint operation is in progress, THE Reprint_Button SHALL indicate the loading state to prevent duplicate taps.
