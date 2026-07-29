# Requirements Document

## Introduction

This feature implements the complete checkout flow for the POS application: transitioning from the catalog view to a checkout ("Cobrar") panel, collecting customer name and payment information, calculating change via a cash denomination keypad, generating formatted receipt tickets (client and internal), printing via thermal LAN printer, persisting the order with ticket text to Room, and resetting the POS cycle for the next order.

## Glossary

- **Checkout_Panel**: The composable UI panel that replaces the CatalogPanel (left 70% of PosScreen) when the user taps the TOTAL button, collecting customer name, payment status, and cash denominations before finalizing the order.
- **Cash_Keypad**: A grid of denomination buttons ($1000, $500, $200, $100, $50, $20, $10, $5, $2, $1) used to register cash received from the customer.
- **Payment_Status**: One of three mutually exclusive states for an order: "Pagado", "No pagó", or "Paga después".
- **Client_Ticket**: A formatted text string representing the customer-facing receipt, including itemized prices, subtotal, IVA, and total.
- **Internal_Ticket**: A formatted text string representing the kitchen/internal receipt, including item quantities without individual prices but with a total article count.
- **Confirmation_Modal**: A dialog displayed after pressing "Completar Orden" that shows the order total, payment received, and change due, requiring explicit confirmation before printing.
- **POS_Cycle**: The complete workflow from adding items to cart, through checkout, to printing and resetting for the next customer.
- **Denomination_Badge**: A visual indicator (Badge composable) on each cash denomination button showing how many times that denomination has been pressed.
- **PosViewModel**: The ViewModel managing cart state, checkout state, and order persistence logic.
- **OrderEntity**: The Room entity representing a persisted order, including ticket text fields.
- **Thermal_Printer**: The EscPosPrinterLan device connected via local network IP address for receipt printing.
- **CartPanel**: The right 30% panel of PosScreen displaying the current order items and total.
- **CatalogPanel**: The left 70% panel of PosScreen displaying the product catalog.

## Requirements

### Requirement 1: Database Schema Extension

**User Story:** As a business owner, I want completed orders to store the exact printed ticket text, so that I can review historical receipts without regenerating them.

#### Acceptance Criteria

1. THE OrderEntity SHALL include a nullable String field named "clientTicketText" with a maximum length of 10,000 characters for storing the client receipt content.
2. THE OrderEntity SHALL include a nullable String field named "internalTicketText" with a maximum length of 10,000 characters for storing the internal receipt content.
3. WHEN an order is persisted with at least one non-null ticket text value (clientTicketText or internalTicketText), THE OrderRepository SHALL store both the clientTicketText and internalTicketText fields in the Room database within the same transaction as the order.
4. WHEN an order is persisted with both ticket text values set to null, THE OrderRepository SHALL store the order with null clientTicketText and null internalTicketText fields without error.
5. WHEN a previously persisted order is queried by its id, THE OrderDao SHALL return the stored clientTicketText and internalTicketText values exactly as they were persisted, including null values.

### Requirement 2: Checkout Panel Transition

**User Story:** As a cashier, I want the catalog panel to switch to a checkout view when I press TOTAL, so that I can collect payment information without leaving the POS screen.

#### Acceptance Criteria

1. WHEN the TOTAL button in CartPanel is pressed, THE PosScreen SHALL replace the CatalogPanel with the Checkout_Panel in the left 70% of the layout.
2. WHILE the Checkout_Panel is displayed, THE CartPanel SHALL remain visible and interactive in the right 30%, showing the current order items and total, and allowing item removal via swipe-to-delete.
3. THE Checkout_Panel SHALL display, from top to bottom, a customer name text field, the three payment status buttons, and the Cash_Keypad.
4. WHILE the cart contains zero items, THE TOTAL button SHALL be disabled (non-clickable and visually dimmed).
5. WHEN the "Cancelar" button in the Checkout_Panel is pressed, THE PosScreen SHALL replace the Checkout_Panel with the CatalogPanel, returning to the catalog view without modifying cart contents.
6. IF all cart items are removed while the Checkout_Panel is displayed, THEN THE PosScreen SHALL automatically replace the Checkout_Panel with the CatalogPanel and disable the TOTAL button.

### Requirement 3: Customer Name Input

**User Story:** As a cashier, I want to record the customer name for each order, so that orders can be identified and called out when ready.

#### Acceptance Criteria

1. THE Checkout_Panel SHALL display a mandatory text field with the label "Nombre del cliente" for customer name entry.
2. WHILE the customer name field is empty or contains only whitespace characters, THE "Completar Orden" button SHALL remain disabled.
3. THE PosViewModel SHALL store the trimmed customer name as part of the checkout state.
4. THE Checkout_Panel SHALL limit the customer name input to a maximum of 40 characters.
5. WHEN the customer name field receives input, THE Checkout_Panel SHALL trim leading and trailing whitespace before storing the value in the checkout state.

### Requirement 4: Payment Status Selection

**User Story:** As a cashier, I want to indicate whether the customer has paid, so that the order status reflects the actual payment situation.

#### Acceptance Criteria

1. THE Checkout_Panel SHALL display three mutually exclusive payment status buttons: "Pagado", "No pagó", and "Paga después".
2. WHEN one payment status button is selected, THE Checkout_Panel SHALL visually highlight the selected button with a distinct background color and deselect the others.
3. THE PosViewModel SHALL store the selected Payment_Status as part of the checkout state, updating immediately on selection.
4. WHEN the Checkout_Panel is first displayed, THE Payment_Status SHALL default to "Pagado".
5. WHEN the currently active payment status button is pressed again, THE Checkout_Panel SHALL keep it selected (no deselection to empty state); exactly one status is always active.

### Requirement 5: Cash Denomination Keypad

**User Story:** As a cashier, I want to register cash received using denomination buttons, so that I can quickly tally the payment amount without manual calculation.

#### Acceptance Criteria

1. THE Cash_Keypad SHALL display buttons for denominations: $1000, $500, $200, $100, $50, $20, $10, $5, $2, and $1.
2. WHEN a denomination button is pressed, THE Cash_Keypad SHALL add that denomination value to the cumulative "cash received" amount.
3. WHEN a denomination button has been pressed one or more times, THE Cash_Keypad SHALL display a Denomination_Badge on that button indicating the press count. WHILE a denomination button has a count of zero, THE Cash_Keypad SHALL NOT display a Denomination_Badge on that button.
4. THE Checkout_Panel SHALL display the current cumulative "cash received" amount formatted as "$X.XX" with a leading dollar sign and exactly 2 decimal places.
5. WHEN the "Limpiar" button is pressed, THE Cash_Keypad SHALL reset all denomination counts to zero and the cumulative cash received to $0.00.
6. THE PosViewModel SHALL store the cash received amount and individual denomination counts as part of the checkout state.
7. THE Cash_Keypad SHALL limit the cumulative "cash received" amount to a maximum of $999,999.99. IF a denomination press would cause the cumulative amount to exceed $999,999.99, THEN THE Cash_Keypad SHALL ignore that press and leave the amount unchanged.

### Requirement 6: Order Completion Validation

**User Story:** As a cashier, I want the system to prevent order completion when required information is missing, so that incomplete orders are not processed.

#### Acceptance Criteria

1. WHILE the customer name field is blank or contains only whitespace characters (trimmed length equals zero), THE "Completar Orden" button SHALL remain disabled.
2. WHILE the Payment_Status is "Pagado" and the cash received is strictly less than the cart total, THE "Completar Orden" button SHALL remain disabled.
3. WHILE the Payment_Status is "No pagó" or "Paga después", THE "Completar Orden" button SHALL be enabled regardless of the cash received amount, provided the customer name trimmed length is greater than zero.
4. WHILE the Payment_Status is "Pagado" and the cash received is greater than or equal to the cart total and the customer name trimmed length is greater than zero, THE "Completar Orden" button SHALL be enabled.

### Requirement 7: Confirmation Modal

**User Story:** As a cashier, I want to review the order summary before finalizing, so that I can catch errors before printing.

#### Acceptance Criteria

1. WHEN the "Completar Orden" button is pressed, THE PosScreen SHALL display the Confirmation_Modal.
2. THE Confirmation_Modal SHALL display the order total amount formatted as "$X.XX".
3. THE Confirmation_Modal SHALL display the selected Payment_Status text.
4. WHEN the Payment_Status is "Pagado", THE Confirmation_Modal SHALL display the cash received amount and the calculated change (cash received minus total) formatted as "$X.XX".
5. THE Confirmation_Modal SHALL display a "Confirmar Pago" button to proceed and a "Cancelar" button to dismiss the modal and return to the Checkout_Panel.
6. WHEN the Payment_Status is "No pagó" or "Paga después", THE Confirmation_Modal SHALL NOT display cash received or change fields.

### Requirement 8: Client Ticket Generation

**User Story:** As a business owner, I want a formatted client receipt, so that customers receive a professional itemized ticket.

#### Acceptance Criteria

1. WHEN "Confirmar Pago" is pressed, THE PosViewModel SHALL generate the Client_Ticket string.
2. THE Client_Ticket SHALL include the header "LOS TACOS", the ticket ID (the persisted OrderEntity ID), and the date-time formatted as "dd/MM/yyyy HH:mm:ss".
3. THE Client_Ticket SHALL include the customer name prefixed with "Nombre: " and the Payment_Status text on separate lines.
4. THE Client_Ticket SHALL include a table with columns CANT (left-aligned, 5 characters wide), DESCRIPCION (left-aligned, 30 characters wide), and IMPORTE (right-aligned, 13 characters wide), listing each order item with quantity, product name truncated to 30 characters if longer, and line total formatted with a leading $ and exactly two decimal places.
5. THE Client_Ticket SHALL include SUBTOTAL (total divided by 1.16, rounded to two decimal places), IVA 16% (total multiplied by 0.16 divided by 1.16, rounded to two decimal places), and TOTAL (cart total with two decimal places) lines right-aligned below the items table.
6. THE Client_Ticket SHALL include the footer text "Gracias por su compra" and "Conserve su ticket" on separate lines.
7. THE Client_Ticket SHALL use 48-character-wide separator lines composed of dashes to delimit the header, items table, totals section, and footer.
8. IF the cart contains zero items when "Confirmar Pago" is pressed, THEN THE PosViewModel SHALL not generate a Client_Ticket and SHALL not proceed with printing.

### Requirement 9: Internal Ticket Generation

**User Story:** As a kitchen staff member, I want an internal ticket showing item quantities without prices, so that I can prepare orders efficiently.

#### Acceptance Criteria

1. WHEN "Confirmar Pago" is pressed, THE PosViewModel SHALL generate the Internal_Ticket string.
2. THE Internal_Ticket SHALL include the header "LOS TACOS", the ticket ID (the persisted OrderEntity ID), and the date-time formatted as "dd/MM/yyyy HH:mm:ss".
3. THE Internal_Ticket SHALL include the customer name prefixed with "Nombre: " and the Payment_Status text on separate lines.
4. THE Internal_Ticket SHALL include a table with columns CANT (left-aligned, 5 characters wide) and DESCRIPCION (left-aligned), listing each order item with quantity and product name without prices.
5. THE Internal_Ticket SHALL include a total article count line formatted as "Total: {count} Artículos" below the items table, where {count} is the sum of all item quantities.
6. THE Internal_Ticket SHALL include the footer text "Gracias por su compra" and "Conserve su ticket" on separate lines.
7. THE Internal_Ticket SHALL use 48-character-wide separator lines composed of dashes to delimit the header, items table, and footer.

### Requirement 10: Ticket Text Formatting

**User Story:** As a developer, I want ticket generation to produce deterministic output from order data, so that the printed output is predictable and testable.

#### Acceptance Criteria

1. THE Ticket_Formatter SHALL produce identical Client_Ticket output given the same ticket ID, date-time, customer name, payment status, and order items, such that parsing the header recovers the original ticket ID, date-time, customer name, and payment status without loss.
2. THE Ticket_Formatter SHALL format currency values with a leading $ sign, no thousands separator, and exactly two decimal places (e.g., "$1500.00"), using HALF_UP rounding when the raw value has more than two decimal digits.
3. THE Ticket_Formatter SHALL right-align the IMPORTE column values within a fixed column width of 10 characters in the Client_Ticket.
4. THE Ticket_Formatter SHALL calculate IVA as exactly totalAmount × 0.16 rounded to two decimal places using HALF_UP rounding.
5. THE Ticket_Formatter SHALL calculate SUBTOTAL as totalAmount divided by 1.16 rounded to two decimal places using HALF_UP rounding.
6. THE Ticket_Formatter SHALL ensure that SUBTOTAL + IVA equals TOTAL; IF a one-cent rounding discrepancy occurs, THEN THE Ticket_Formatter SHALL adjust SUBTOTAL so that SUBTOTAL + IVA equals the displayed TOTAL exactly.

### Requirement 11: Thermal Printer Execution

**User Story:** As a cashier, I want the receipt to print automatically after confirmation, so that the customer receives their ticket without additional steps.

#### Acceptance Criteria

1. WHEN "Confirmar Pago" is pressed in the Confirmation_Modal, THE PosViewModel SHALL change the button text to "Imprimiendo Ticket" and disable the "Confirmar Pago" button and the cancel button until the print operation completes or fails.
2. WHEN the print operation is initiated, THE PosViewModel SHALL invoke EscPosPrinterLan.printTicket() using the printer IP address retrieved from PrinterPreferencesRepository.getIpAddress().
3. IF the printer IP address retrieved from PrinterPreferencesRepository is an empty string, THEN THE PosViewModel SHALL display an error message via the Snackbar indicating that no printer IP is configured and SHALL re-enable the Confirmation_Modal buttons without attempting the print operation.
4. IF the printer operation fails or does not complete within 15 seconds, THEN THE PosViewModel SHALL display an error message via the Snackbar indicating the print failure, re-enable the Confirmation_Modal buttons, and display a "Reintentar" button allowing the user to retry the print operation up to a maximum of 3 attempts.
5. WHEN the print operation completes successfully, THE PosViewModel SHALL proceed to order persistence.

### Requirement 12: Order Persistence with Tickets

**User Story:** As a business owner, I want completed orders saved with their ticket text, so that I have a complete audit trail.

#### Acceptance Criteria

1. WHEN printing completes successfully, THE PosViewModel SHALL persist the OrderEntity with the generated clientTicketText (maximum 10,000 characters) and internalTicketText (maximum 10,000 characters).
2. THE PosViewModel SHALL persist all OrderItemEntity records and OrderItemCustomizationEntity records associated with the order.
3. THE OrderEntity SHALL store the customerName (maximum 120 characters), the status field set to the Payment_Status value, and the totalAmount (ranging from 0.00 to 999,999,999.99) alongside the clientTicketText and internalTicketText fields.
4. THE PosViewModel SHALL persist the order within a single Room database transaction so that either all records (OrderEntity, OrderItemEntity list, OrderItemCustomizationEntity list) are inserted, or none are inserted.
5. IF the persistence transaction fails, THEN THE PosViewModel SHALL retain the current cart items in memory and expose an error message via the error state indicating that the order could not be saved.

### Requirement 13: POS Cycle Reset

**User Story:** As a cashier, I want the system to automatically reset after completing an order, so that I can immediately serve the next customer.

#### Acceptance Criteria

1. WHEN order persistence completes successfully, THE PosViewModel SHALL clear all cart items from the in-memory state.
2. WHEN order persistence completes successfully, THE PosViewModel SHALL reset the checkout state: customer name to empty string, payment status to "Pagado", cash received to $0.00, and all denomination counts to zero.
3. WHEN order persistence completes successfully, THE PosScreen SHALL transition the left panel from the Checkout_Panel back to the CatalogPanel.
4. WHEN the POS cycle resets, THE CartPanel SHALL display an empty cart with a total of "$0.00".
5. IF order persistence fails, THEN THE PosViewModel SHALL retain all cart items and checkout state unchanged, allowing the user to retry.
