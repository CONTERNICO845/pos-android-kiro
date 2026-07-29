# Requirements Document

## Introduction

Este spec cubre dos entregables para el sistema de impresión del Punto de Venta:

1. **Auditoría de arquitectura de impresión** — Documentar el flujo completo desde que el cajero presiona "Confirmar Pago" hasta que los bytes llegan a la impresora térmica por TCP.
2. **Corrección del formato de ticket** — Agregar al ticket del cliente la información de pago en efectivo / cambio, y las notas extra (`extraNotes`) debajo de cada producto junto a sus personalizaciones.

## Glossary

- **PosViewModel**: ViewModel principal de la pantalla POS. Orquesta el carrito, checkout y la impresión.
- **TicketFormatter**: Objeto utilitario puro que genera el texto formateado del ticket (cliente e interno).
- **TicketLineItem**: Data class que representa un renglón del ticket (cantidad, nombre, total, personalizaciones).
- **EscPosPrinterLan**: Singleton que envía bytes ESC/POS por TCP al puerto 9100 usando charset Cp850.
- **CartItem**: Modelo en memoria de un producto en el carrito, contiene `selectedCustomizations` y `extraNotes`.
- **CheckoutState**: Estado del flujo de checkout, contiene `cashReceived`, `paymentStatus`, `denominationCounts`.
- **ESC/POS**: Protocolo de comandos para impresoras térmicas de punto de venta.
- **Cp850**: Code page 850 — charset compatible con ESC/POS para caracteres latinos.
- **Folio**: Número secuencial del ticket (e.g., "001", "002").
- **IVA**: Impuesto al Valor Agregado (16% en México).

## Requirements

### Requirement 1: Documentación de Arquitectura de Impresión

**User Story:** Como desarrollador, quiero un documento que describa el flujo completo de impresión, para poder entender y mantener el sistema sin leer todo el código fuente.

#### Acceptance Criteria

1. THE printer_architecture.md document SHALL describe the sequence of operations from the moment "Confirmar Pago" is pressed in PosViewModel until EscPosPrinterLan sends bytes over TCP, including: IP validation from PrinterPreferencesRepository, folio generation (sequential order count zero-padded to 3 digits), ticket text generation via TicketFormatter, and the printDoubleTicket call
2. THE printer_architecture.md document SHALL describe how PosViewModel maps CartItem fields (quantity, productName, totalPrice, selectedCustomizations[].optionName) into TicketLineItem instances (quantity, productName, lineTotal, customizations)
3. THE printer_architecture.md document SHALL describe how TicketFormatter formats text using a fixed total width of 48 characters with column layout CANT (5 chars) + DESCRIPCION (30 chars) + IMPORTE (13 chars), and how the client ticket includes itemized prices, subtotal, IVA 16%, and total while the internal ticket omits prices and shows only an article count
4. THE printer_architecture.md document SHALL describe how EscPosPrinterLan opens a TCP socket on port 9100 with a 5-second connect timeout and 15-second overall coroutine timeout enforced via kotlinx.coroutines withTimeout
5. THE printer_architecture.md document SHALL describe the ESC/POS byte sequence: ESC_INIT (0x1B 0x40), text payload encoded in Cp850, 3 line-feed characters, and ESC_CUT (0x1D 0x56 0x00)
6. THE printer_architecture.md document SHALL describe the double-ticket flow where client and internal tickets are each preceded by ESC_INIT and followed by line feeds plus ESC_CUT, sent sequentially over a single TCP connection
7. THE printer_architecture.md document SHALL describe the error-handling flow: IF printing fails, the system increments printAttempts and allows retry up to a maximum of 3 attempts; IF all 3 attempts fail, the system displays an error message and re-enables the confirm button without persisting the order

### Requirement 2: Pasar Datos de Pago al TicketFormatter

**User Story:** Como cajero, quiero que el ticket del cliente muestre cuánto efectivo recibí y cuánto cambio di, para que el cliente tenga un comprobante claro de su transacción.

#### Acceptance Criteria

1. WHEN PosViewModel generates the client ticket and CheckoutState.paymentStatus is PAGADO and CheckoutState.cashReceived is greater than zero, THE PosViewModel SHALL pass `cashReceived` and the calculated change amount to TicketFormatter.formatClientTicket
2. THE TicketFormatter.formatClientTicket function SHALL accept parameters for cash received (Double) and change amount (Double) in addition to its existing parameters (ticketId, dateTime, customerName, paymentStatus, items, totalAmount)
3. WHEN `cashReceived` is greater than zero and paymentStatus is PAGADO, THE TicketFormatter SHALL render a payment section between the totals separator and the footer, containing one line "Pago (Efectivo MXN): $X.XX" and one line "Cambio: $X.XX", each right-aligned within 48 characters and formatted to 2 decimal places with HALF_UP rounding
4. THE TicketFormatter SHALL calculate change as `cashReceived - totalAmount` using BigDecimal with HALF_UP rounding to 2 decimal places, where the result is zero or positive
5. IF `cashReceived` is zero or paymentStatus is not PAGADO, THEN THE TicketFormatter SHALL omit the "Pago (Efectivo MXN)" and "Cambio" lines from the ticket output
6. IF `cashReceived` is less than `totalAmount` while paymentStatus is PAGADO, THEN THE TicketFormatter SHALL render the payment section with change displayed as "$0.00"

### Requirement 3: Incluir extraNotes en TicketLineItem

**User Story:** Como cajero, quiero que las notas adicionales de cada producto aparezcan en el ticket impreso, para que la cocina vea instrucciones especiales del cliente.

#### Acceptance Criteria

1. THE TicketLineItem data class SHALL include an `extraNotes` field of type String (empty string when no notes exist)
2. WHEN PosViewModel maps CartItem to TicketLineItem, THE PosViewModel SHALL pass `cartItem.extraNotes` into the TicketLineItem `extraNotes` field
3. WHEN a TicketLineItem has a non-empty `extraNotes`, THE TicketFormatter SHALL render it below the customizations line using the format: "      * Nota: {extraNotes}" where the first line allows 34 characters of note content (48 - 14 prefix chars)
4. WHEN a TicketLineItem has an empty `extraNotes`, THE TicketFormatter SHALL not render any note line for that item
5. THE TicketFormatter SHALL wrap `extraNotes` that exceed 34 characters on the first line to additional continuation lines indented with 13 spaces, allowing 35 characters per continuation line, with a maximum of 8 continuation lines
6. THE TicketFormatter SHALL render extraNotes on both the client ticket and the internal ticket using the same format

### Requirement 4: Formato del Desglose Financiero en el Ticket del Cliente

**User Story:** Como dueño del negocio, quiero que el ticket del cliente muestre un desglose financiero completo (subtotal, IVA, total, pago, cambio) separado visualmente, para que se vea profesional y sea fácil de leer.

#### Acceptance Criteria

1. THE TicketFormatter SHALL render the financial section in the following order: separator line, subtotal (antes de IVA), IVA (16%), total, pago (efectivo), cambio, separator line
2. THE TicketFormatter SHALL format each financial line as the label left-aligned followed by the currency amount right-aligned, such that label + amount together occupy exactly 48 characters padded with spaces
3. THE TicketFormatter SHALL use the label "Subtotal (antes de IVA):" followed by the formatted currency amount
4. THE TicketFormatter SHALL use the label "IVA (16%):" followed by the formatted currency amount
5. THE TicketFormatter SHALL use the label "Total:" followed by the formatted currency amount
6. WHEN cash payment info is present, THE TicketFormatter SHALL use the label "Pago (Efectivo MXN):" followed by the formatted cash received amount
7. WHEN cash payment info is present, THE TicketFormatter SHALL use the label "Cambio:" followed by the formatted change amount calculated as cash received minus total, rounded to 2 decimal places using HALF_UP
8. IF cash payment info is not present, THEN THE TicketFormatter SHALL omit the "Pago (Efectivo MXN):" and "Cambio:" lines from the financial section
9. THE TicketFormatter SHALL maintain the arithmetic invariant: Subtotal + IVA = Total, where Subtotal = Total / 1.16 rounded HALF_UP to 2 decimal places, and IVA = Total - Subtotal

### Requirement 5: Formato de Personalizaciones y Notas en la Lista de Productos

**User Story:** Como cajero, quiero que cada producto muestre sus opciones elegidas y la nota extra de forma visualmente clara, para que la cocina no confunda instrucciones.

#### Acceptance Criteria

1. THE TicketFormatter SHALL render each selected customization on its own line using the format: "      - {optionName}", truncating optionName to a maximum of 40 characters if it exceeds the available line width
2. WHEN a product has both customizations and a non-empty extraNotes, THE TicketFormatter SHALL render all customization lines first, then the note line immediately below them
3. WHEN a product has a non-empty extraNotes but no customizations, THE TicketFormatter SHALL render the note line immediately below the product line
4. IF a TicketLineItem has a non-empty extraNotes, THEN THE TicketFormatter SHALL use the prefix "      * Nota: " for the first line of extraNotes, providing visual distinction from customization lines that use "      - "
5. IF a TicketLineItem has an empty extraNotes, THEN THE TicketFormatter SHALL not render any note line for that item
6. THE TicketFormatter SHALL apply the same customization and note rendering rules to both the client ticket and the internal ticket

### Requirement 6: Consistencia de Datos entre CartItem y Ticket Impreso

**User Story:** Como desarrollador, quiero garantizar que ningún dato del carrito se pierda durante la transformación a ticket, para evitar tickets incompletos.

#### Acceptance Criteria

1. WHEN PosViewModel.confirmPayment() maps CartItem instances to TicketLineItem instances, THE PosViewModel SHALL produce exactly one TicketLineItem per CartItem, where each TicketLineItem preserves: quantity equal to CartItem.quantity, productName equal to CartItem.productName, lineTotal equal to CartItem.totalPrice, customizations list equal to CartItem.selectedCustomizations mapped to their optionName values in the same order, and extraNotes equal to CartItem.extraNotes
2. WHEN TicketFormatter.formatClientTicket receives a list of TicketLineItem, THE TicketFormatter SHALL include in the output string the productName (truncated to 30 characters if longer) of every TicketLineItem in the input list, each appearing on its own formatted line
3. WHEN TicketFormatter.formatClientTicket receives a list of TicketLineItem where at least one item has a non-empty extraNotes value (length > 0 after trimming), THE TicketFormatter SHALL include each such non-empty extraNotes value in the output string
4. WHEN TicketFormatter.formatClientTicket receives a list of TicketLineItem where at least one item has a non-empty customizations list, THE TicketFormatter SHALL include every customization option name from each TicketLineItem's customizations list in the output string, each prefixed with "- "
5. IF cashReceived is provided and totalAmount > 0, THEN THE change amount displayed on the ticket SHALL equal cashReceived minus totalAmount, computed using BigDecimal with RoundingMode.HALF_UP and scale of 2 decimal places
6. WHEN the cart contains between 1 and 100 CartItem instances (inclusive), THE PosViewModel SHALL produce a TicketLineItem list with the same size as the cart, preserving the original cart ordering
