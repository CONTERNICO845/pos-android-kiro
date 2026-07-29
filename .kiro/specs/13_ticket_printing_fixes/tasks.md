# Implementation Plan

## Overview

Fix three interrelated ticket printing bugs: (1) UUID displayed as folio instead of sequential number, (2) only client ticket printed while internal ticket is never sent to printer, and (3) incorrect prefixes and separator placement in both ticket templates. The fix uses the bug condition methodology — exploration tests first to confirm bugs, preservation tests to lock existing behavior, then targeted implementation.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Ticket Formatting and Folio Defects
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bugs exist
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the three bugs exist (folio, format prefixes, internal header)
  - **Scoped PBT Approach**: Scope the property to concrete failing cases:
    - Call `formatClientTicket(ticketId = "test-uuid-value", ...)` and assert output does NOT contain "test-uuid-value" as folio (tests folio bug)
    - Call `formatClientTicket(...)` and assert output does NOT contain `"Fecha:"` or `"Estado:"` prefixes
    - Call `formatClientTicket(...)` and assert output does NOT start with a separator before "LOS TACOS"
    - Call `formatClientTicket(...)` and assert output does NOT end with a separator after footer
    - Call `formatInternalTicket(...)` and assert column header matches `"CANT  DESCRIPCION                         IMPORTE"`
    - Call `formatInternalTicket(...)` and assert no separator between last item and "Total: N Artículos"
  - Generate random `List<TicketLineItem>` (non-empty) and random customer names
  - For all generated inputs: assert `formatClientTicket()` never contains `"Fecha:"`, `"Estado:"`, no leading/trailing separators
  - For all generated inputs: assert `formatInternalTicket()` has correct column header and no `"$"` in item lines
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bugs exist)
  - Document counterexamples found (e.g., output contains `"Fecha: 01/06/2025 14:30:00"` instead of `"01/06/2025 14:30:00"`)
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Tax Calculation and Format Invariants
  - **IMPORTANT**: Follow observation-first methodology
  - Observe: `calculateSubtotal(100.0) + calculateIva(100.0) == 100.0` on unfixed code
  - Observe: `formatCurrency(49.99)` returns `"$49.99"` on unfixed code
  - Observe: `formatClientTicket(...)` contains `"Nombre: {customerName}"` on unfixed code
  - Observe: All separator lines are exactly 48 characters on unfixed code
  - Observe: `formatInternalTicket(...)` item lines contain no `"$"` character on unfixed code
  - Write property-based tests:
    - For random `totalAmount` in (0.01..99999.99): `calculateSubtotal(t) + calculateIva(t) == t` (to 2 decimal places)
    - For random `amount` in (0.01..99999.99): `formatCurrency(amount)` matches `\$\d+\.\d{2}` pattern
    - For random customer names and items: `"Nombre: {name}"` appears in both `formatClientTicket()` and `formatInternalTicket()`
    - For random items: all separators in output are exactly 48 chars wide
    - For random items: `formatInternalTicket()` item lines contain no `"$"` character
    - For random items: total article count equals `items.sumOf { it.quantity }`
  - Verify tests PASS on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.6, 3.7, 3.10_

- [x] 3. Fix for ticket printing defects (folio, double print, format)

  - [x] 3.1 Add order count query to OrderDao and OrderRepository
    - Add `@Query("SELECT COUNT(*) FROM orders") suspend fun getOrderCount(): Int` to `OrderDao`
    - Add `suspend fun getOrderCount(): Int = orderDao.getOrderCount()` to `OrderRepository`
    - _Bug_Condition: isBugCondition(input) where confirmPayment() uses UUID as ticketId_
    - _Expected_Behavior: folio = (orderCount + 1).toString().padStart(3, '0')_
    - _Preservation: Room persistence with UUID PK unchanged_
    - _Requirements: 2.1_

  - [x] 3.2 Add printDoubleTicket method to EscPosPrinterLan
    - Add `suspend fun printDoubleTicket(ipAddress: String, clientTicketText: String, internalTicketText: String)`
    - Implementation: single TCP connection → ESC_INIT → client bytes → feed + CUT → ESC_INIT → internal bytes → feed + CUT → flush → close
    - Increase `OVERALL_TIMEOUT_MS` from `10_000L` to `15_000L`
    - _Bug_Condition: isBugCondition(input) where only printTicket(clientText) is called_
    - _Expected_Behavior: printDoubleTicket sends 2× ESC_INIT + 2× ESC_CUT over single socket_
    - _Preservation: TCP port 9100, charset Cp850, 5s connect timeout unchanged_
    - _Requirements: 2.2, 3.9_

  - [x] 3.3 Refactor TicketFormatter.formatClientTicket()
    - Remove leading `sb.appendLine(SEPARATOR)` before "LOS TACOS"
    - Change `"Fecha: $dateTime"` → `dateTime` (no prefix)
    - Change `"Estado: $paymentStatus"` → `paymentStatus` (no prefix)
    - Remove trailing `sb.appendLine(SEPARATOR)` after footer
    - Keep `"Nombre: $customerName"` unchanged
    - _Bug_Condition: isBugCondition(input) where output contains "Fecha:", "Estado:", leading/trailing separators_
    - _Expected_Behavior: no "Fecha:", no "Estado:", no leading separator before header, no trailing separator after footer_
    - _Preservation: "Nombre:" prefix retained, SUBTOTAL + IVA = TOTAL, $X.XX format, 48-char width_
    - _Requirements: 2.3, 2.4, 2.5, 3.3, 3.4, 3.5, 3.10_

  - [x] 3.4 Refactor TicketFormatter.formatInternalTicket()
    - Remove leading `sb.appendLine(SEPARATOR)` before "LOS TACOS"
    - Change `"Fecha: $dateTime"` → `dateTime` (no prefix)
    - Change `"Estado: $paymentStatus"` → `paymentStatus` (no prefix)
    - Change column header to: `"CANT ".padEnd(5) + "DESCRIPCION".padEnd(30) + "IMPORTE".padStart(13)` followed by separator
    - Item lines: `qty.padEnd(5) + productName.take(30).padEnd(30) + "".padStart(13)` (trailing spaces, no price)
    - Remove `sb.appendLine(SEPARATOR)` between items and "Total: N Artículos" line
    - Remove trailing `sb.appendLine(SEPARATOR)` after footer
    - _Bug_Condition: isBugCondition(input) where header is "CANT DESCRIPCION", has wrong prefixes and separators_
    - _Expected_Behavior: header = "CANT  DESCRIPCION                         IMPORTE", no prefixes, no extra separators_
    - _Preservation: article count = sum of quantities, no "$" in item lines, 48-char width_
    - _Requirements: 2.6, 2.7, 2.8, 2.9, 2.10, 3.6, 3.7, 3.10_

  - [x] 3.5 Wire sequential folio and double printing in PosViewModel.confirmPayment()
    - Before generating tickets: `val orderCount = orderRepository.getOrderCount()`
    - Compute `val folio = (orderCount + 1).toString().padStart(3, '0')`
    - Pass `folio` as `ticketId` to both `formatClientTicket()` and `formatInternalTicket()`
    - Replace `EscPosPrinterLan.printTicket(ipAddress, clientTicketText)` with `EscPosPrinterLan.printDoubleTicket(ipAddress, clientTicketText, internalTicketText)`
    - _Bug_Condition: isBugCondition(input) where UUID used as ticketId and only client ticket printed_
    - _Expected_Behavior: folio is sequential padded number, both tickets sent via printDoubleTicket_
    - _Preservation: retry logic (max 3 attempts), OrderEntity.id remains UUID, both ticket texts stored_
    - _Requirements: 2.1, 2.2, 3.1, 3.2_

  - [x] 3.6 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Ticket Formatting and Folio Defects
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bugs are fixed)
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

  - [x] 3.7 Verify preservation tests still pass
    - **Property 2: Preservation** - Tax Calculation and Format Invariants
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all preservation tests still pass after fix (no regressions)

- [x] 4. Checkpoint - Ensure all tests pass
  - Compile the project with `./gradlew assembleDebug`
  - Run all unit tests with `./gradlew testDebugUnitTest`
  - Verify no regressions in existing tests
  - Ensure all property-based tests (bug condition + preservation) pass
  - Ask the user if questions arise

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2"] },
    { "id": 1, "tasks": ["3.1", "3.2", "3.3", "3.4"] },
    { "id": 2, "tasks": ["3.5"] },
    { "id": 3, "tasks": ["3.6", "3.7"] },
    { "id": 4, "tasks": ["4"] }
  ]
}
```

- Tasks 1 and 2 are independent and can run in parallel (wave 0)
- Tasks 3.1, 3.2, 3.3, 3.4 are independent of each other (wave 1)
- Task 3.5 depends on 3.1, 3.2, 3.3, 3.4 — needs all pieces in place (wave 2)
- Tasks 3.6 and 3.7 depend on 3.5 — verify fix works (wave 3)
- Task 4 depends on all prior tasks (wave 4)

## Notes

- Property-based tests use random inputs (ticket line items, customer names, monetary amounts) to provide stronger guarantees than unit tests alone
- The exploration test (task 1) is expected to FAIL on unfixed code — this confirms the bugs exist
- The preservation tests (task 2) are expected to PASS on unfixed code — this locks baseline behavior
- After fix implementation, exploration tests should PASS and preservation tests should still PASS
- The `printDoubleTicket` timeout is increased to 15s to account for two sequential print jobs over one connection
