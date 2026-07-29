# Bugfix Requirements Document

## Introduction

Three related bugs exist in the checkout printing flow of the POS application:

1. **Incorrect Ticket ID Format** — The printed ticket displays a full UUID (e.g., `a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d`) instead of a sequential zero-padded folio (e.g., `001`, `002`, `003`).
2. **Only One Ticket Printed** — Only the client ticket is sent to the thermal printer; the internal (kitchen) ticket is generated and stored in Room but never printed.
3. **Ticket Formatting Mismatch** — The output of `TicketFormatter.formatClientTicket()` and `TicketFormatter.formatInternalTicket()` does not match the required templates. Discrepancies include unwanted prefixes (`Fecha:`, `Estado:`), incorrect separators, and the internal ticket missing the `IMPORTE` column header.

These bugs impact the daily operation of the POS system — staff receive incorrectly formatted tickets, the kitchen never gets a printed copy, and the ticket folio is unreadable for quick reference.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN `confirmPayment()` is called THEN the system prints a full UUID string (e.g., `a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d`) as the ticket folio on both client and internal tickets

1.2 WHEN `confirmPayment()` completes printing successfully THEN the system only sends the client ticket to the printer via `EscPosPrinterLan.printTicket()` — the internal ticket is never sent to the printer hardware

1.3 WHEN `formatClientTicket()` generates the client ticket THEN the system outputs `Fecha: {dateTime}` with a "Fecha:" prefix instead of the date-time string alone

1.4 WHEN `formatClientTicket()` generates the client ticket THEN the system outputs `Estado: {paymentStatus}` with an "Estado:" prefix instead of the payment status text alone

1.5 WHEN `formatClientTicket()` generates the client ticket THEN the system adds a leading separator line (`------------------------------------------------`) before the "LOS TACOS" header

1.6 WHEN `formatClientTicket()` generates the client ticket THEN the system adds a trailing separator line after the footer ("Conserve su ticket")

1.7 WHEN `formatInternalTicket()` generates the internal ticket THEN the system outputs only `CANT DESCRIPCION` as the column header instead of `CANT  DESCRIPCION                         IMPORTE`

1.8 WHEN `formatInternalTicket()` generates the internal ticket THEN the system outputs `Fecha: {dateTime}` with a "Fecha:" prefix instead of the date-time string alone

1.9 WHEN `formatInternalTicket()` generates the internal ticket THEN the system outputs `Estado: {paymentStatus}` with an "Estado:" prefix instead of the payment status text alone

1.10 WHEN `formatInternalTicket()` generates the internal ticket THEN the system adds leading and trailing separator lines around the header and footer sections

### Expected Behavior (Correct)

2.1 WHEN `confirmPayment()` is called THEN the system SHALL use a sequential zero-padded folio number (format: `(orderCount + 1).toString().padStart(3, '0')`) as the ticket identifier on printed tickets, derived from the current order count in the database

2.2 WHEN `confirmPayment()` completes ticket generation THEN the system SHALL send both the client ticket AND the internal ticket to the printer in sequence — client ticket first with a paper cut, then internal ticket with a paper cut — within a single TCP connection

2.3 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL output the date-time string directly (e.g., `01/06/2025 14:30:00`) without any prefix

2.4 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL output the payment status text directly (e.g., `Pagado`) without any prefix

2.5 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL NOT include a separator line before the "LOS TACOS" header nor after the footer

2.6 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL use `CANT  DESCRIPCION                         IMPORTE` as the column header (matching the client ticket header) followed by a separator line, but item lines SHALL have no price values (trailing spaces only)

2.7 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL output the date-time string directly without any prefix

2.8 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL output the payment status text directly without any prefix

2.9 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL NOT include a separator line before the "LOS TACOS" header nor after the footer

2.10 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL place the "Total: {N} Artículos" line directly after the last item line without a separator between items and the total line

### Unchanged Behavior (Regression Prevention)

3.1 WHEN `confirmPayment()` is called with a non-empty cart THEN the system SHALL CONTINUE TO persist the order to Room with UUID as the primary key (database storage is unaffected by folio formatting)

3.2 WHEN `confirmPayment()` encounters a print failure THEN the system SHALL CONTINUE TO increment print attempts and show retry up to 3 attempts maximum

3.3 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL CONTINUE TO include the `Nombre: {customerName}` line with the "Nombre:" prefix

3.4 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL CONTINUE TO display SUBTOTAL, IVA 16%, and TOTAL with correct tax calculations (SUBTOTAL + IVA = TOTAL)

3.5 WHEN `formatClientTicket()` generates the client ticket THEN the system SHALL CONTINUE TO format currency as `$X.XX` with HALF_UP rounding and no thousands separator

3.6 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL CONTINUE TO calculate the article count as the sum of all item quantities

3.7 WHEN `formatInternalTicket()` generates the internal ticket THEN the system SHALL CONTINUE TO exclude price values from individual item lines

3.8 WHEN the order is persisted THEN the system SHALL CONTINUE TO store both `clientTicketText` and `internalTicketText` in the OrderEntity

3.9 WHEN `EscPosPrinterLan.printTicket()` is called THEN the system SHALL CONTINUE TO use TCP port 9100 with the ESC/POS charset Cp850 and the existing timeout configuration (5s connect, 10s overall)

3.10 WHEN `formatClientTicket()` or `formatInternalTicket()` generates a ticket THEN the system SHALL CONTINUE TO use a 48-character width for separator lines and content alignment
