# Implementation Plan: Sprint de Correcciones UX (16_sprint_correcciones)

## Overview

Implementación incremental de tres áreas UX: (1) Navegación con chips de menú y barra de búsqueda, (2) Divisor de orden con impresión en ticket y fuente doble altura, (3) Rediseño completo del CheckoutPanel estilo calculadora/asistente. El código se organiza en capas: data models → ViewModel logic → UI composables → printing → wiring final.

## Tasks

- [x] 1. Data models y color tokens
  - [x] 1.1 Add isDivider field to CartItem and TicketLineItem, add new color tokens
    - In `CartItem.kt`, add `val isDivider: Boolean = false` field to the data class
    - In `TicketFormatter.kt`, add `val isDivider: Boolean = false` field to the `TicketLineItem` data class
    - Create `InternalTicketSegments` data class in `TicketFormatter.kt` with fields: `header: String`, `items: String`, `footer: String`
    - In the app's color tokens file (e.g., `Color.kt` or `Theme.kt`), add new tokens: `CheckoutBackground = Color(0xFFFFFFFF)`, `CheckoutSectionBg = Color(0xFFF5F5F5)`, `CheckoutChangePanel = Color(0xFFEEEEEE)`, `CheckoutAlertBg = Color(0xFFFFF3E0)`, `CoinButtonBg = Color(0xFFA5D6A7)`
    - _Requirements: 12.1, 12.3, 6.1_

  - [x] 1.2 Add CheckoutState modifications
    - In the existing `CheckoutState` data class, add `val customAmounts: List<Double> = emptyList()` field for tracking custom amount entries
    - Ensure `denominationCounts: Map<Int, Int> = emptyMap()` and `cashReceived: Double = 0.0` fields are present
    - _Requirements: 8.3, 9.1_

- [x] 2. ViewModel logic: Filters and search
  - [x] 2.1 Add menu filter state and logic to PosViewModel
    - Add `private val _selectedMenu = MutableStateFlow<String?>(null)` to PosViewModel
    - Add `fun selectMenu(menuId: String?)` that sets `_selectedMenu.value`
    - Modify `productsFlow` to incorporate `_selectedMenu` filter: when non-null, only emit products whose category has `associatedMenuId` matching the selected menu
    - When menu filter changes and the currently selected category doesn't belong to the new menu, auto-reset `_selectedCategory` to null
    - _Requirements: 1.2, 1.4, 1.5, 1.6_

  - [x] 2.2 Add search state and logic to PosViewModel
    - Add `private val _searchQuery = MutableStateFlow("")` and `private val _isSearchVisible = MutableStateFlow(false)` to PosViewModel
    - Add `fun toggleSearch()` that flips `_isSearchVisible` and clears query when hiding
    - Add `fun updateSearchQuery(query: String)` that sets `_searchQuery.value` (max 100 chars)
    - Add `fun clearSearch()` that sets `_searchQuery.value = ""`
    - Apply 300ms debounce on `_searchQuery` within `productsFlow` and filter products by case-insensitive name contains
    - When a category tab is selected, clear search query and hide search field
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 2.6_

  - [x] 2.3 Add divider logic to PosViewModel
    - Add `fun addDivider()` that appends a CartItem with `isDivider = true`, `productName = "--- DIVISOR ---"`, `productId = ""`, `emoji = ""`, `basePrice = 0.00`, `totalPrice = 0.00`, `quantity = 1`, `selectedCustomizations = emptyList()`, `extraNotes = ""`
    - Modify `cartTotalFlow` to filter out items where `isDivider == true`
    - Modify `isCompletarOrdenEnabled()` to exclude dividers from sum
    - Modify `confirmPayment()`/`completeOrder()` to exclude dividers from `OrderItemEntity` list, but preserve `isDivider` flag in `TicketLineItem` list
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 12.2, 12.4, 12.5_

- [x] 3. ViewModel logic: Checkout
  - [x] 3.1 Add checkout cash management functions to PosViewModel
    - Add `fun addDenomination(value: Int)` that increments `denominationCounts[value]` by 1 and recalculates `cashReceived` as sum of `(denomination * count)` across all entries plus custom amounts
    - Add `fun addCustomAmount(amount: String)` that parses the input with `toDoubleOrNull()`, ignores if null/zero/negative, otherwise adds to `cashReceived` and appends to `customAmounts` list, clears input
    - Add `fun clearCashReceived()` that resets `denominationCounts` to empty, `customAmounts` to empty, and `cashReceived` to 0.0
    - Guard: if total `cashReceived` would exceed $999,999.99, ignore the tap
    - _Requirements: 8.3, 8.6, 9.2, 9.3, 9.4_

  - [x] 3.2 Implement Completar Orden button enablement logic
    - Modify the existing completar orden enablement logic:
      - If `customerName.trim().isEmpty()` → disabled
      - If `paymentStatus == PAGADO` AND `cashReceived < cartTotal` → disabled
      - If `paymentStatus ∈ {NO_PAGO, PAGA_DESPUES}` AND `customerName.trim().isNotEmpty()` → enabled
      - If `paymentStatus == PAGADO` AND `cashReceived >= cartTotal` AND `customerName.trim().isNotEmpty()` → enabled
    - _Requirements: 11.2, 11.3, 11.4_

- [x] 4. Checkpoint - Verify ViewModel compilation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. UI: Navigation and filter composables
  - [x] 5.1 Create MenuFilterBar composable
    - Create `app/src/main/java/com/example/puntodeventa/ui/pos/MenuFilterBar.kt`
    - Implement a horizontal scrollable row (LazyRow) of chip buttons, each showing `emoji + name` from MenuItem
    - Selected chip: `NavRailIconSelected` background with white text
    - Unselected chip: `CardBackground` background with `CardText` color
    - Tap selected chip → `onMenuSelected(null)` (deselect)
    - Tap unselected chip → `onMenuSelected(menuItem.id)`
    - If `menuItems` is empty, render nothing
    - _Requirements: 1.1, 1.3, 1.4_

  - [x] 5.2 Create SearchTextField composable
    - Create `app/src/main/java/com/example/puntodeventa/ui/pos/SearchTextField.kt`
    - `OutlinedTextField` with magnifying glass `leadingIcon`
    - Trailing clear icon (X) visible when `query.isNotEmpty()`
    - `maxLength = 100` characters enforced via `onValueChange` filter
    - Placeholder text "Buscar producto..."
    - _Requirements: 2.1, 2.2, 2.5_

  - [x] 5.3 Integrate MenuFilterBar and SearchTextField into PosScreen
    - In `PosScreen.kt`, add `MenuFilterBar` above the existing `CategoryTabBar`
    - Add a search icon button in the `CategoryTabBar` area that toggles `SearchTextField` visibility
    - Add a scissors icon button (ContentCut) in the `CategoryTabBar` area that calls `addDivider()`
    - When `SearchTextField` is visible, display it below the `CategoryTabBar`
    - Wire `onMenuSelected`, `onQueryChange`, `onClear`, and `toggleSearch` to PosViewModel functions
    - _Requirements: 1.1, 2.1, 2.6, 3.1_

- [x] 6. UI: Cart divider rendering
  - [x] 6.1 Render divider items in CartPanel
    - In the CartPanel composable (or CartItemRow), when `cartItem.isDivider == true`, render a full-width horizontal dashed line spanning the cart row width
    - Do NOT display product name, price, quantity, emoji, or remove/edit controls for divider items
    - _Requirements: 3.2_

- [x] 7. UI: CheckoutPanel redesign
  - [x] 7.1 Create PaymentStatusPills composable
    - Create or extract a `PaymentStatusPills` composable showing 3 pill-shaped buttons ("Pagado", "No pagó", "Paga después") in a horizontal row with equal width
    - Selected pill: `ButtonConfirm (0xFF4CAF50)` background + white text
    - Unselected pills: outlined with `InputBorder` color + default text
    - Default selection: "Pagado"
    - _Requirements: 6.3, 6.4, 6.5_

  - [x] 7.2 Create BillsGrid composable
    - Create `BillsGrid` composable with denomination buttons in a grid layout
    - Bill buttons ($1000, $500, $200, $100, $50, $20): `CardBackground` background
    - Coin buttons ($10, $5, $2, $1): `CoinButtonBg` (lighter green) background
    - Each button shows denomination value; on tap calls `onDenominationPressed(value)`
    - Display Badge with tap count when count > 0; hide Badge when count == 0
    - Container: rounded with 8dp corner radius and 1dp light border
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 7.3 Create ExactPaymentInput composable
    - Input field labeled "Pago impar/exacto" with "Agregar" button
    - "Agregar" validates and calls `onAddCustomAmount(amount)`
    - "Limpiar" button calls `onClearCashReceived()`
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 7.4 Create ChangeAssistant composable
    - Light gray panel (0xFFEEEEEE) with 3 columns: "Total", "Recibido", "Cambio"
    - All values formatted as currency with "$" prefix and 2 decimal places
    - Below the 3-column summary: soft yellow/orange alert box (0xFFFFF3E0) with lightbulb icon
    - If `cashReceived > cartTotal`: "Dar $XX.XX de cambio exacto"
    - If `cashReceived == cartTotal`: "Pago exacto"
    - If `cashReceived < cartTotal`: "Falta $XX.XX"
    - Change = max(0, cashReceived - cartTotal) rounded HALF_UP to 2dp
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 7.5 Rewrite CheckoutPanel composable with full layout
    - Rewrite `CheckoutPanel.kt` using light mode theme (white background)
    - Layout top-to-bottom: Customer name OutlinedTextField (max 40 chars) → PaymentStatusPills → TotalDisplay (label "Total a cobrar" + bold 32.sp amount) → BillsGrid → ExactPaymentInput → ChangeAssistant → full-width "Completar Orden" button
    - "Completar Orden" button: green (ButtonConfirm) background, bold white text 18.sp, disabled with 0.38 opacity when conditions not met
    - Wire all callbacks to PosViewModel functions via CheckoutPanel parameters
    - _Requirements: 6.1, 6.2, 7.1, 7.2, 7.3, 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 8. Checkpoint - Verify UI compilation and visual check
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Ticket formatting and printing
  - [x] 9.1 Modify TicketFormatter to handle divider items
    - In `formatClientTicket` and `formatInternalTicket`, when encountering a `TicketLineItem` with `isDivider == true`, output a line of exactly 48 dash characters ("-")
    - Do NOT print quantity, name, price, customizations, or notes for divider items
    - Exclude divider items from article count and financial totals (subtotal, IVA, total)
    - Preserve relative order of all items (dividers at correct position)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 12.6_

  - [x] 9.2 Implement formatInternalTicketSegmented in TicketFormatter
    - Add a new function `formatInternalTicketSegmented(...)` that returns `InternalTicketSegments`
    - `header`: everything from the ticket title through the column header line and its separator
    - `items`: all product rows (including customization sub-lines, extra notes, and divider dashes)
    - `footer`: "Total: N Artículos" line and any footer content
    - Article count excludes dividers
    - _Requirements: 5.1, 5.2, 5.4_

  - [x] 9.3 Implement printInternalTicketWithDoubleHeight in EscPosPrinterLan
    - Add method `suspend fun printInternalTicketWithDoubleHeight(ipAddress: String, headerText: String, itemsText: String, footerText: String)`
    - Send `headerText` in normal size
    - Send ESC ! 0x10 (Double Height command: bytes 0x1B, 0x21, 0x10)
    - Send `itemsText` (product rows, customization lines, notes, divider dashes)
    - Send ESC ! 0x00 (Normal Size command: bytes 0x1B, 0x21, 0x00)
    - Send `footerText` in normal size
    - Column layout (48 chars = CANT 5 + DESCRIPCION 30 + IMPORTE 13) remains unchanged since Double Height only affects vertical size
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 9.4 Wire segmented printing into PosViewModel confirmPayment flow
    - In `confirmPayment()`/`completeOrder()`, call `formatInternalTicketSegmented(...)` to get `InternalTicketSegments`
    - Call `printInternalTicketWithDoubleHeight(ip, segments.header, segments.items, segments.footer)` for the internal ticket
    - Client ticket continues to use existing `printTicket` method with normal text (no Double Height)
    - _Requirements: 5.5_

- [x] 10. Checkpoint - Full integration verification
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Property-based tests
  - [x]* 11.1 Write property tests for filter logic (Properties 1-5)
    - **Property 1: Menu filter shows only matching products**
    - **Property 2: Menu filter toggle restores unfiltered state**
    - **Property 3: Menu and Category intersection filter**
    - **Property 4: Search filter by name (case-insensitive)**
    - **Property 5: Search clear restores state respecting active filters**
    - **Validates: Requirements 1.2, 1.4, 1.5, 2.2, 2.3**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/FilterLogicPropertyTest.kt`
    - Use Kotest property testing with Arb generators for product lists, menu IDs, category IDs, and search strings
    - Minimum 100 iterations per property

  - [x]* 11.2 Write property tests for cart divider logic (Properties 6-8)
    - **Property 6: Cart total excludes divider items**
    - **Property 7: Divider addition produces correct fixed values**
    - **Property 8: Order persistence excludes divider items**
    - **Validates: Requirements 3.3, 3.4, 12.2, 12.5**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/CartTotalPropertyTest.kt`
    - Generate random CartItem lists with mix of regular and divider items
    - Minimum 100 iterations per property

  - [x]* 11.3 Write property tests for ticket formatter divider (Properties 9-11)
    - **Property 9: TicketFormatter renders dividers as 48-dash lines and excludes from totals**
    - **Property 10: Divider position preserved in ticket output**
    - **Property 11: Ticket line width is exactly 48 characters**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 5.3, 12.6**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/TicketFormatterDividerPropertyTest.kt`
    - Generate random TicketLineItem lists with dividers at random positions
    - Minimum 100 iterations per property

  - [x]* 11.4 Write property tests for checkout logic (Properties 12-17)
    - **Property 12: Denomination tap updates count and cashReceived consistently**
    - **Property 13: Valid custom amount adds to cashReceived**
    - **Property 14: Invalid custom amount is ignored**
    - **Property 15: Limpiar resets all cash state to zero**
    - **Property 16: Change assistant calculation**
    - **Property 17: Completar Orden button enablement logic**
    - **Validates: Requirements 8.3, 8.6, 9.2, 9.3, 9.4, 10.2, 10.3, 10.4, 10.5, 11.2, 11.3, 11.4**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/CheckoutLogicPropertyTest.kt`
    - Generate random denomination sequences, custom amounts, cashReceived/cartTotal pairs, PaymentStatus combinations
    - Minimum 100 iterations per property

  - [x]* 11.5 Write property test for CartItem-to-TicketLineItem conversion (Property 18)
    - **Property 18: CartItem-to-TicketLineItem preserves isDivider**
    - **Validates: Requirements 12.4**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/CartItemConversionPropertyTest.kt`
    - Generate random CartItem lists, convert, verify isDivider preservation
    - Minimum 100 iterations

- [x] 12. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at ViewModel, UI, and integration levels
- Property tests validate universal correctness properties from the design document
- The design uses segmented ticket text (header/items/footer) to avoid ESC/POS command injection mid-parse
- Double Height only affects the internal ticket; client ticket remains normal size
- Color tokens follow the existing project pattern for theming
- `CartItem.isDivider` defaults to false, ensuring backward compatibility

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["5.1", "5.2", "6.1", "7.1", "7.2", "7.3", "7.4"] },
    { "id": 4, "tasks": ["5.3", "7.5"] },
    { "id": 5, "tasks": ["9.1", "9.2"] },
    { "id": 6, "tasks": ["9.3"] },
    { "id": 7, "tasks": ["9.4"] },
    { "id": 8, "tasks": ["11.1", "11.2", "11.3", "11.4", "11.5"] }
  ]
}
```
