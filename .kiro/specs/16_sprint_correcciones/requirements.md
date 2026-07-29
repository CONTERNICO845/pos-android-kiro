# Requirements Document

## Introduction

Sprint de correcciones UX para la aplicación POS (PuntoDeVenta). Este sprint aborda tres áreas clave: (1) Navegación y filtros en la pantalla POS mediante chips de menú y barra de búsqueda, (2) Divisor de orden (tijeras) con impresión de línea separadora y aumento de fuente en ticket, y (3) Rediseño completo del panel de Checkout con un layout estilo "Calculadora/Asistente" en tema claro con acentos verdes.

## Glossary

- **POS_Screen**: Pantalla principal del punto de venta que contiene el CatalogPanel y el CartPanel
- **CatalogPanel**: Panel izquierdo (70% del ancho) que muestra las categorías y la grilla de productos
- **CartPanel**: Panel derecho (30% del ancho) que muestra los artículos en el carrito
- **CheckoutPanel**: Panel que reemplaza al CatalogPanel cuando se procede al cobro
- **CategoryTabBar**: Barra de pestañas horizontales que muestra las categorías de productos
- **Menu_Filter_Bar**: Fila horizontal de chips/botones que representa los menús creados (e.g., Tacos, Pizzas), posicionada encima del CategoryTabBar
- **Search_TextField**: Campo de texto que aparece al presionar el icono de búsqueda y filtra productos en tiempo real por nombre
- **Cart_Divider_Item**: Elemento especial del carrito con nombre "--- DIVISOR ---", precio $0.00 y flag isDivider = true que actúa como separador visual de orden
- **TicketFormatter**: Objeto utilitario puro que genera el texto formateado de los tickets de cliente e interno
- **EscPosPrinterLan**: Objeto que maneja la comunicación TCP con la impresora térmica vía comandos ESC/POS
- **PosViewModel**: ViewModel que gestiona el estado del POS incluyendo categorías, productos, carrito y checkout
- **MenuItem**: Data class que representa un menú creado por el usuario (id, emoji, nombre)
- **Double_Height_Command**: Comando ESC/POS (0x1B, 0x21, 0x10) que aplica altura doble al texto impreso
- **Bills_Grid**: Contenedor de botones de denominaciones (billetes y monedas) para registrar efectivo recibido
- **Change_Assistant**: Panel inferior del Checkout que muestra Total, Recibido y Cambio con alerta visual del cambio exacto a entregar

---

## Requirements

### Requirement 1: Menu Filter Bar

**User Story:** As a cashier, I want to filter products by menu using visible chip buttons, so that I can quickly find products belonging to a specific menu without navigating categories.

#### Acceptance Criteria

1. WHEN the POS_Screen loads, THE Menu_Filter_Bar SHALL display a horizontal scrollable row of chip buttons, one per existing MenuItem showing its emoji and name, positioned above the CategoryTabBar with no chip pre-selected so that all products are visible by default
2. WHEN a menu chip is pressed, THE CatalogPanel SHALL filter the product grid to show ONLY products whose category belongs to the selected menu (matching Category.associatedMenuId to MenuItem.id), and THE CategoryTabBar SHALL update to show only categories belonging to the selected menu
3. WHILE a menu chip is selected, THE Menu_Filter_Bar SHALL visually highlight the active chip with a distinct background color differentiating it from unselected chips
4. WHEN a selected menu chip is pressed again, THE Menu_Filter_Bar SHALL deselect the filter, THE CategoryTabBar SHALL restore all categories for the current context, and THE CatalogPanel SHALL return to showing all products with no category or menu filter applied
5. WHILE a menu filter is active, THE CategoryTabBar SHALL remain visible and functional, filtering products within the intersection of the selected menu AND the selected category
6. IF a menu filter is applied and the currently selected category does not belong to that menu, THEN THE CategoryTabBar SHALL reset its selection to show all categories within the filtered menu
7. IF the intersection of the selected menu and selected category yields zero products, THEN THE CatalogPanel SHALL display the product grid area empty with no error indication

### Requirement 2: Search Bar Toggle

**User Story:** As a cashier, I want to search products by name using the magnifying glass icon, so that I can quickly locate a product without scrolling through the grid.

#### Acceptance Criteria

1. WHEN the search icon in the CategoryTabBar is pressed, THE POS_Screen SHALL toggle the visibility of the Search_TextField below the CategoryTabBar, with the Search_TextField hidden by default on screen load
2. WHILE the Search_TextField is visible and contains text, THE CatalogPanel SHALL filter the product grid within 300 milliseconds of the last keystroke to show only products whose name contains the typed text (case-insensitive), limited to a maximum query length of 100 characters
3. WHEN the Search_TextField is cleared or toggled hidden, THE CatalogPanel SHALL restore the product grid to the unfiltered state (respecting active menu and category filters)
4. IF the Search_TextField contains text and no products match the query, THEN THE CatalogPanel SHALL display an empty-state message indicating no products were found
5. IF text is present in the Search_TextField, THEN THE Search_TextField SHALL display a clear icon (X) that clears the search query on single tap
6. WHEN a category tab is selected while the Search_TextField is visible, THE POS_Screen SHALL clear the search query and hide the Search_TextField

### Requirement 3: Order Divider (Scissors Button)

**User Story:** As a cashier, I want to add a visual divider to the order, so that the kitchen receives a clear separation between groups of items on the printed ticket.

#### Acceptance Criteria

1. WHEN the scissors icon (ContentCut) in the CategoryTabBar is pressed, THE PosViewModel SHALL append a CartItem to the cart with productName "--- DIVISOR ---", basePrice 0.00, totalPrice 0.00, quantity 1, isDivider flag set to true, an empty selectedCustomizations list, and an empty extraNotes string
2. THE CartPanel SHALL render any CartItem where isDivider is true as a full-width horizontal dashed line spanning the cart row width, without displaying product name, price, quantity, emoji, or the remove/edit controls shown on regular items
3. WHEN the cart total is computed, THE PosViewModel SHALL exclude all CartItems where isDivider is true from the sum in every calculation path (cartTotalFlow, isCompletarOrdenEnabled, and completeOrder totalAmount)
4. WHEN completeOrder is called, THE PosViewModel SHALL exclude all CartItems where isDivider is true from the persisted OrderItemEntity list so that dividers are not stored as order line items
5. IF the cart is empty (contains zero non-divider items), THEN THE PosViewModel SHALL still permit adding a divider but the cart total SHALL remain 0.00

### Requirement 4: Divider Printing on Ticket

**User Story:** As a kitchen worker, I want to see a clear dashed line on the printed ticket separating groups of items, so that I can prepare orders in the correct batches.

#### Acceptance Criteria

1. WHEN the TicketFormatter encounters a TicketLineItem where the productName equals a designated divider sentinel value, THE TicketFormatter SHALL print a line consisting of exactly 48 dash characters ("-") instead of a regular product row
2. WHEN rendering a divider TicketLineItem, THE TicketFormatter SHALL NOT print quantity, product name, price columns, customizations, or extra notes for that line item
3. THE TicketFormatter SHALL NOT include divider line items in the article count calculation on the internal ticket, nor in any subtotal, IVA, or total calculations on the client ticket
4. THE TicketFormatter SHALL render the divider line on both the client ticket (formatClientTicket) and the internal ticket (formatInternalTicket) at the same position relative to surrounding items as it appears in the input list

### Requirement 5: Double Height Font for Product Rows

**User Story:** As a kitchen worker, I want the product names on the printed ticket to be larger and more readable, so that I can quickly identify items without straining my eyes.

#### Acceptance Criteria

1. WHEN printing the internal ticket, THE EscPosPrinterLan SHALL send the Double_Height_Command (ESC ! 0x10) immediately before the first product item row and after the items table header line ("CANT DESCRIPCION IMPORTE") and its separator
2. WHEN the last product item row (including its customization sub-lines and extra notes lines) has been sent, THE EscPosPrinterLan SHALL send the Normal_Size_Command (ESC ! 0x00) before printing the "Total: N Artículos" line or any footer content
3. THE EscPosPrinterLan SHALL maintain the column layout of CANT (5 chars) + DESCRIPCION (30 chars) + IMPORTE (13 chars) = 48 chars total for product item rows when Double Height is applied, since Double Height affects only vertical character size and not horizontal character width
4. THE Double_Height_Command SHALL apply to product item rows, their customization sub-lines (prefixed with "- "), and their extra notes lines (prefixed with "* Nota:"); the ticket header, items table column header, separators, "Total: N Artículos" line, and footer lines SHALL remain in normal height
5. THE EscPosPrinterLan SHALL apply the Double_Height_Command exclusively to the internal ticket; the client ticket SHALL be printed entirely in normal text height

### Requirement 6: Checkout Panel Redesign - Top Section

**User Story:** As a cashier, I want a clean, organized checkout interface with a customer name field and payment status pills, so that I can quickly enter order information in an intuitive layout.

#### Acceptance Criteria

1. THE CheckoutPanel SHALL use a light mode theme with white (0xFFFFFFFF) background and light-gray (0xFFF5F5F5) section backgrounds
2. THE CheckoutPanel SHALL display a single-line OutlinedTextField labeled "Nombre del cliente (opcional)" at the top of the panel, accepting a maximum of 40 characters
3. BELOW the customer name field, THE CheckoutPanel SHALL display 3 pill-shaped buttons (fully rounded corners) in a horizontal row with equal width for payment status: "Pagado", "No pagó", "Paga después"
4. WHEN a payment status pill is selected, THE CheckoutPanel SHALL highlight the selected pill with a ButtonConfirm (0xFF4CAF50) background and white (0xFFFFFFFF) text, while unselected pills remain outlined with InputBorder-colored borders and default text color
5. WHEN the CheckoutPanel first appears, THE CheckoutPanel SHALL display "Pagado" as the default selected payment status pill

### Requirement 7: Checkout Panel Redesign - Total Section

**User Story:** As a cashier, I want to see the total amount prominently displayed, so that I can quickly communicate the amount to the customer.

#### Acceptance Criteria

1. BELOW the payment status pills, THE CheckoutPanel SHALL display a horizontally centered label "Total a cobrar" in a medium gray text color (opacity or tone clearly distinguishable from both the bold total value and the white background)
2. BELOW the "Total a cobrar" label, THE CheckoutPanel SHALL display the cart total amount horizontally centered, in bold weight with a minimum font size of 32.sp, formatted as currency with a "$" prefix and exactly 2 decimal places (e.g., "$1,234.56", "$0.00")
3. IF the cart total is zero, THEN THE CheckoutPanel SHALL still display "$0.00" in the total amount position using the same styling as a non-zero total

### Requirement 8: Checkout Panel Redesign - Bills Grid Section

**User Story:** As a cashier, I want to tap bill and coin buttons to register cash received, so that I can quickly track payment without manual input.

#### Acceptance Criteria

1. THE Bills_Grid SHALL display bill denomination buttons ($1000, $500, $200, $100, $50, $20) using a CardBackground background color
2. THE Bills_Grid SHALL display coin denomination buttons ($10, $5, $2, $1) using a distinct lighter green background color that is visually distinguishable from the bill buttons without requiring side-by-side comparison
3. WHEN a denomination button is tapped, THE Bills_Grid SHALL increment the tap count for that denomination by 1 and display a Badge on the button showing the current cumulative tap count
4. IF the tap count for a denomination is 0, THEN THE Bills_Grid SHALL hide the Badge for that denomination button
5. THE Bills_Grid SHALL be contained within a rounded container with a minimum corner radius of 8dp and a 1dp-wide light border for visual grouping
6. WHEN a denomination button is tapped, THE Bills_Grid SHALL update the displayed cash received total by adding the tapped denomination value to the previous total

### Requirement 9: Checkout Panel Redesign - Exact Payment Section

**User Story:** As a cashier, I want to enter custom payment amounts for odd values, so that I can handle exact change situations and non-standard denominations.

#### Acceptance Criteria

1. BELOW the Bills_Grid, THE CheckoutPanel SHALL display a labeled input field "Pago impar/exacto" with an "Agregar" button that adds the entered amount to the cash received total
2. WHEN the "Agregar" button is pressed with a valid numeric amount (positive decimal number), THE PosViewModel SHALL add that amount to cashReceived and clear the input field
3. IF the "Agregar" button is pressed with an empty, non-numeric, zero, or negative input, THEN THE CheckoutPanel SHALL ignore the press without modifying cashReceived
4. THE CheckoutPanel SHALL display a "Limpiar" button that resets all denomination counts, custom amounts, and cashReceived to zero

### Requirement 10: Checkout Panel Redesign - Change Assistant Section

**User Story:** As a cashier, I want to see a clear breakdown of total, received, and change amounts with a suggestion for exact change, so that I can hand back the correct amount confidently.

#### Acceptance Criteria

1. BELOW the exact payment section, THE CheckoutPanel SHALL display a light gray (0xFFEEEEEE) panel with 3 columns showing "Total", "Recibido", and "Cambio" values formatted as currency with a dollar sign prefix and exactly 2 decimal places (e.g., "$45.00")
2. WHEN cashReceived is greater than or equal to the cart total, THE Change_Assistant SHALL compute and display the change as (cashReceived - cartTotal) rounded to 2 decimal places using HALF_UP rounding, formatted as currency in the "Cambio" column
3. WHEN cashReceived is greater than the cart total, THE CheckoutPanel SHALL display a soft yellow/orange (0xFFFFF3E0) alert box below the 3-column summary with a lightbulb icon and the text "Dar $XX.XX de cambio exacto" where XX.XX is the calculated change amount
4. WHEN cashReceived equals the cart total exactly, THE CheckoutPanel SHALL display the alert box with the text "Pago exacto" instead of "Dar $0.00 de cambio exacto"
5. WHILE cashReceived is less than the cart total, THE Change_Assistant SHALL display "$0.00" as the change value in the "Cambio" column and the alert box SHALL display "Falta $XX.XX" where XX.XX is (cartTotal - cashReceived) rounded to 2 decimal places

### Requirement 11: Checkout Panel Redesign - Final Action Button

**User Story:** As a cashier, I want a large, clear button to complete the order, so that I can finalize the sale with confidence.

#### Acceptance Criteria

1. THE CheckoutPanel SHALL display a full-width "Completar Orden" button at the bottom of the panel with a green (ButtonConfirm) background and bold white (ButtonConfirmText) text at a minimum font size of 18.sp
2. WHILE the payment status is "Pagado" AND cashReceived is less than cartTotal, THE "Completar Orden" button SHALL be disabled with an opacity of 0.38 and SHALL NOT respond to press events
3. IF the customer name (trimmed) is empty, THEN THE "Completar Orden" button SHALL be disabled with an opacity of 0.38 and SHALL NOT respond to press events
4. IF the payment status is "No pagó" or "Paga después", THEN THE "Completar Orden" button SHALL be enabled regardless of the cashReceived amount, provided the customer name (trimmed) is not empty
5. WHEN the "Completar Orden" button is pressed and the button is in the enabled state, THE PosViewModel SHALL display the confirmation modal

### Requirement 12: Cart Divider Data Model

**User Story:** As a developer, I want the CartItem model to support divider items, so that the divider concept is type-safe and clearly distinguishable from regular products.

#### Acceptance Criteria

1. THE CartItem data class SHALL include an isDivider Boolean field defaulting to false
2. IF isDivider is true, THEN THE CartItem SHALL have productName "--- DIVISOR ---", productId as an empty string, emoji as an empty string, basePrice 0.00, totalPrice 0.00, quantity 1, an empty selectedCustomizations list, and extraNotes as an empty string
3. THE TicketLineItem data class SHALL include an isDivider Boolean field defaulting to false
4. WHEN converting CartItems to TicketLineItems for ticket generation, THE PosViewModel SHALL preserve the isDivider flag value
5. WHEN calculating the cart total, THE PosViewModel SHALL exclude CartItems where isDivider is true from the sum of totalPrice values
6. WHEN formatting a ticket that contains a TicketLineItem with isDivider equal to true, THE TicketFormatter SHALL render a visual separator line instead of a standard product line, and SHALL exclude that item from financial totals
