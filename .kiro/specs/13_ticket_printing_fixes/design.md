# Ticket Printing Fixes Bugfix Design

## Overview

Three interrelated bugs exist in the checkout printing flow: (1) the printed folio displays a raw UUID instead of a sequential zero-padded number, (2) only the client ticket is sent to the printer while the internal ticket is never printed, and (3) both ticket templates contain unwanted prefixes and incorrect separator placement. The fix strategy is minimal and targeted — modify `OrderDao`/`OrderRepository` for the folio count, add a `printDoubleTicket()` method to `EscPosPrinterLan`, and refactor `TicketFormatter` output formatting — while preserving all existing persistence, retry, and tax-calculation behavior.

## Glossary

- **Bug_Condition (C)**: The set of inputs/states that trigger one or more of the three bugs — any call to `confirmPayment()` with a non-empty cart and a configured printer IP
- **Property (P)**: The desired behavior for buggy inputs — sequential folio, both tickets printed, correct template formatting
- **Preservation**: Existing behaviors that must remain unchanged — Room persistence with UUID PK, retry logic, tax calculations, currency formatting, 48-char ticket width
- **TicketFormatter**: The pure formatting object in `ui/pos/TicketFormatter.kt` that generates client and internal ticket strings
- **EscPosPrinterLan**: The TCP printing object in `data/printer/EscPosPrinterLan.kt` that sends ESC/POS commands to port 9100
- **PosViewModel.confirmPayment()**: The function in `ui/pos/PosViewModel.kt` that orchestrates ticket generation, printing, and order persistence
- **OrderDao / OrderRepository**: The Room DAO and repository in `data/local/OrderDao.kt` / `data/repository/OrderRepository.kt`
- **Folio**: A sequential zero-padded display-only ticket number (e.g., `001`) distinct from the UUID primary key

## Bug Details

### Bug Condition

The bugs manifest on every successful checkout. Whenever `confirmPayment()` is invoked with a non-empty cart and a reachable printer, the system: (a) prints the UUID as folio, (b) sends only the client ticket, and (c) produces incorrectly formatted ticket text.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type ConfirmPaymentContext
         { cartItems: List<CartItem>, printerIp: String }
  OUTPUT: boolean

  RETURN input.cartItems.isNotEmpty()
         AND input.printerIp.isNotBlank()
         AND printerIsReachable(input.printerIp)
END FUNCTION
```

### Examples

- **Folio bug**: Order placed as the 5th sale of the day → ticket prints `a3f7b2c1-4e5d-6a7b-...` instead of `005`
- **Single print bug**: Kitchen staff never receives a physical ticket; only the client gets one
- **Format bug (client)**: Ticket starts with `---...---\n   LOS TACOS` instead of `   LOS TACOS`; date line reads `Fecha: 01/06/2025 14:30:00` instead of `01/06/2025 14:30:00`
- **Format bug (internal)**: Column header shows `CANT DESCRIPCION` instead of `CANT  DESCRIPCION                         IMPORTE`; items correctly omit prices but header is wrong

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Room persistence uses UUID as primary key (`OrderEntity.id`) — unaffected by display folio
- Print retry logic: up to 3 attempts, error messages shown on failure
- `Nombre: {customerName}` prefix retained on both tickets
- Tax calculations: `SUBTOTAL + IVA 16% = TOTAL` with HALF_UP rounding
- Currency format: `$X.XX` with no thousands separator
- Internal ticket item lines exclude price values
- Total article count = sum of all item quantities
- Both `clientTicketText` and `internalTicketText` stored in `OrderEntity`
- TCP port 9100, charset `Cp850`, 5s connect timeout
- 48-character ticket width for separators and alignment

**Scope:**
All inputs that do NOT involve the three defective code paths should be completely unaffected by this fix. This includes:
- Order persistence and Room transactions
- Cart state management and UI state transitions
- Printer connection testing (`testConnection()`)
- Payment status transitions and customer name handling

## Hypothesized Root Cause

Based on code analysis, the root causes are confirmed (not merely hypothesized):

1. **Folio uses UUID directly**: In `PosViewModel.confirmPayment()`, the `orderId` (a `UUID.randomUUID().toString()`) is passed as `ticketId` to `TicketFormatter`. No sequential counter exists in `OrderDao` or `OrderRepository`.

2. **Only client ticket printed**: `confirmPayment()` calls `EscPosPrinterLan.printTicket(ipAddress, clientTicketText)` only. No call exists for `internalTicketText`, and `EscPosPrinterLan` has no method to print two tickets in one session.

3. **Incorrect format prefixes**: `TicketFormatter.formatClientTicket()` explicitly outputs `"Fecha: $dateTime"` and `"Estado: $paymentStatus"`. The internal ticket uses `"Fecha: $dateTime"` and `"Estado: $paymentStatus"` as well.

4. **Incorrect separators**: Both `formatClientTicket()` and `formatInternalTicket()` begin with `sb.appendLine(SEPARATOR)` before "LOS TACOS" and end with `sb.appendLine(SEPARATOR)` after the footer.

5. **Internal ticket column header**: `formatInternalTicket()` uses `"CANT DESCRIPCION"` instead of the full `"CANT  DESCRIPCION                         IMPORTE"` header with trailing spaces on item lines.

## Correctness Properties

Property 1: Bug Condition - Sequential Folio Format

_For any_ call to `confirmPayment()` where the database contains N existing orders, the ticket text SHALL display the folio as `(N + 1).toString().padStart(3, '0')` — a strictly sequential, zero-padded, 3+ character numeric string — instead of a UUID.

**Validates: Requirements 2.1**

Property 2: Bug Condition - Double Ticket Printing

_For any_ call to `printDoubleTicket(ip, clientText, internalText)`, the byte sequence sent over the single TCP socket SHALL contain exactly two ESC_INIT commands (`0x1B 0x40`) and exactly two ESC_CUT commands (`0x1D 0x56 0x00`), with client ticket bytes preceding internal ticket bytes.

**Validates: Requirements 2.2**

Property 3: Bug Condition - Client Ticket No Forbidden Prefixes

_For any_ valid inputs to `formatClientTicket()`, the output SHALL NOT contain the substrings `"Fecha:"` or `"Estado:"`, SHALL NOT start with a separator line before "LOS TACOS", and SHALL NOT end with a separator line after the footer.

**Validates: Requirements 2.3, 2.4, 2.5**

Property 4: Bug Condition - Internal Ticket Column Header and No Prices

_For any_ valid inputs to `formatInternalTicket()`, the output SHALL use `"CANT  DESCRIPCION                         IMPORTE"` as the column header, SHALL NOT contain any `"$"` character in item lines, SHALL NOT contain `"Fecha:"` or `"Estado:"` prefixes, SHALL NOT start with a separator before "LOS TACOS", and SHALL place "Total: {N} Artículos" directly after the last item without an intervening separator.

**Validates: Requirements 2.6, 2.7, 2.8, 2.9, 2.10**

Property 5: Preservation - Tax Calculation Invariant

_For any_ `totalAmount > 0`, the relationship `calculateSubtotal(totalAmount) + calculateIva(totalAmount) == totalAmount` (to 2 decimal places) SHALL continue to hold, preserving the existing tax calculation behavior.

**Validates: Requirements 3.4, 3.5**

Property 6: Preservation - Room Persistence with UUID PK

_For any_ successful checkout, the `OrderEntity` persisted to Room SHALL continue to use a UUID string as its `id` primary key, and SHALL continue to store both `clientTicketText` and `internalTicketText` fields.

**Validates: Requirements 3.1, 3.8**

Property 7: Preservation - Retry Logic Unchanged

_For any_ print failure, the system SHALL continue to increment `printAttempts` and show retry UI up to 3 maximum attempts, producing the same error messages as before.

**Validates: Requirements 3.2**

## Fix Implementation

### Changes Required

**File**: `app/src/main/java/com/example/puntodeventa/data/local/OrderDao.kt`

**Change 1 — Add order count query**:
- Add `@Query("SELECT COUNT(*) FROM orders") suspend fun getOrderCount(): Int`

---

**File**: `app/src/main/java/com/example/puntodeventa/data/repository/OrderRepository.kt`

**Change 2 — Expose order count**:
- Add `suspend fun getOrderCount(): Int = orderDao.getOrderCount()`

---

**File**: `app/src/main/java/com/example/puntodeventa/data/printer/EscPosPrinterLan.kt`

**Change 3 — Add double-print method**:
- Add `suspend fun printDoubleTicket(ipAddress: String, clientTicketText: String, internalTicketText: String)`
- Implementation: single TCP connection → ESC_INIT → client bytes → feed + CUT → ESC_INIT → internal bytes → feed + CUT → flush → close
- Increase `OVERALL_TIMEOUT_MS` from `10_000L` to `15_000L`

---

**File**: `app/src/main/java/com/example/puntodeventa/ui/pos/PosViewModel.kt`

**Change 4 — Use sequential folio**:
- Before generating tickets, call `val orderCount = orderRepository.getOrderCount()`
- Compute `val folio = (orderCount + 1).toString().padStart(3, '0')`
- Pass `folio` as `ticketId` to both `formatClientTicket()` and `formatInternalTicket()`

**Change 5 — Print both tickets**:
- Replace `EscPosPrinterLan.printTicket(ipAddress, clientTicketText)` with `EscPosPrinterLan.printDoubleTicket(ipAddress, clientTicketText, internalTicketText)`

---

**File**: `app/src/main/java/com/example/puntodeventa/ui/pos/TicketFormatter.kt`

**Change 6 — Fix `formatClientTicket()` template**:
- Remove leading `sb.appendLine(SEPARATOR)` before "LOS TACOS"
- Change `"Fecha: $dateTime"` → `dateTime`
- Change `"Estado: $paymentStatus"` → `paymentStatus`
- Remove trailing `sb.appendLine(SEPARATOR)` after footer
- Keep `"Nombre: $customerName"` unchanged

**Change 7 — Fix `formatInternalTicket()` template**:
- Remove leading `sb.appendLine(SEPARATOR)` before "LOS TACOS"
- Change `"Fecha: $dateTime"` → `dateTime`
- Change `"Estado: $paymentStatus"` → `paymentStatus`
- Change column header from `"CANT DESCRIPCION"` to `"CANT ".padEnd(5) + "DESCRIPCION".padEnd(30) + "IMPORTE".padStart(13)` followed by separator
- Item lines: `qty.padEnd(5) + productName.take(30).padEnd(30) + "".padStart(13)` (trailing spaces, no price)
- Remove `sb.appendLine(SEPARATOR)` between items and "Total: N Artículos" line
- Remove trailing `sb.appendLine(SEPARATOR)` after footer

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bugs on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the three bugs BEFORE implementing the fix. Confirm the root cause analysis.

**Test Plan**: Write unit tests that call the existing `TicketFormatter` and `EscPosPrinterLan` methods and assert against the expected (correct) format. Run on UNFIXED code to observe failures.

**Test Cases**:
1. **Folio Format Test**: Call `formatClientTicket(ticketId = UUID, ...)` and assert output does NOT contain UUID pattern (will fail on unfixed code — UUID is present)
2. **Double Print Test**: Inspect `confirmPayment()` call chain and assert `printTicket` is called twice or `printDoubleTicket` exists (will fail on unfixed code — only one call)
3. **Client No-Prefix Test**: Call `formatClientTicket(...)` and assert output does not contain `"Fecha:"` (will fail on unfixed code)
4. **Internal Column Header Test**: Call `formatInternalTicket(...)` and assert column header matches `"CANT  DESCRIPCION                         IMPORTE"` (will fail on unfixed code)

**Expected Counterexamples**:
- `formatClientTicket()` output contains `"Fecha: "` substring
- `formatInternalTicket()` output contains `"CANT DESCRIPCION"` without IMPORTE
- Only `printTicket()` is called — no `printDoubleTicket()` method exists

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed functions produce the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  folio := (orderRepository.getOrderCount() + 1).padStart(3, '0')
  clientText := formatClientTicket_fixed(folio, dateTime, name, status, items, total)
  internalText := formatInternalTicket_fixed(folio, dateTime, name, status, items)

  ASSERT folio matches Regex("\\d{3,}")
  ASSERT clientText does NOT contain "Fecha:" or "Estado:"
  ASSERT clientText does NOT start with SEPARATOR
  ASSERT internalText contains "CANT  DESCRIPCION                         IMPORTE"
  ASSERT internalText does NOT contain "$"
  ASSERT printDoubleTicket sends 2x ESC_INIT + 2x ESC_CUT
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed functions produce the same result as the original functions.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT calculateSubtotal(total) + calculateIva(total) == total
  ASSERT formatCurrency(amount) matches "$X.XX" format
  ASSERT OrderEntity uses UUID as id (primary key)
  ASSERT printAttempts increment on failure (max 3)
  ASSERT testConnection() behavior is identical
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many random monetary amounts to verify `SUBTOTAL + IVA = TOTAL`
- It generates random product names and quantities to verify format invariants
- It catches rounding edge cases that manual unit tests miss

**Test Plan**: Observe behavior on UNFIXED code for tax calculations and currency formatting, then write property-based tests asserting those behaviors remain after the fix.

**Test Cases**:
1. **Tax Invariant Preservation**: For random totals, verify `calculateSubtotal(t) + calculateIva(t) == t`
2. **Currency Format Preservation**: For random amounts, verify output matches `$X.XX` regex
3. **Nombre Prefix Preservation**: For random customer names, verify `"Nombre: {name}"` appears in both tickets
4. **Ticket Width Preservation**: Verify all separator lines are exactly 48 characters

### Unit Tests

- Test `formatClientTicket()` produces correct output for known inputs (exact string match)
- Test `formatInternalTicket()` produces correct output for known inputs (exact string match)
- Test folio generation: `getOrderCount() = 0` → folio `"001"`, count `= 99` → folio `"100"`, count `= 999` → folio `"1000"`
- Test `printDoubleTicket()` byte sequence with a mock `OutputStream`
- Test edge case: empty customer name, single item, max quantity

### Property-Based Tests

- Generate random `totalAmount` values (0.01..99999.99) and verify `calculateSubtotal + calculateIva == total`
- Generate random `List<TicketLineItem>` and verify `formatClientTicket()` never contains `"Fecha:"`, `"Estado:"`, or leading/trailing separators
- Generate random `List<TicketLineItem>` with non-zero prices and verify `formatInternalTicket()` never contains `"$"`
- Generate random order counts (0..9999) and verify folio is always `(count+1).padStart(3,'0')`

### Integration Tests

- Full `confirmPayment()` flow with a mock printer: assert both ticket texts appear in the byte stream
- Full flow: verify `OrderEntity.id` is still a UUID after checkout while ticket text contains a numeric folio
- Full flow: verify print failure at attempt 1 triggers retry UI, attempt 3 triggers final error
