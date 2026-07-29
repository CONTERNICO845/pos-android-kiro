# Implementation Plan: Cart Edit and Ticket Options

## Overview

Implement two additive features: (1) print selected customization names on both client and internal tickets below each item line, and (2) enable in-place editing of cart items via tap-to-edit on CartItemRow, reusing ProductModal in edit mode. Changes touch TicketLineItem, TicketFormatter, CartPanel, ProductModal, PosViewModel, and PosScreen.

## Tasks

- [x] 1. Add customizations field to TicketLineItem and update TicketFormatter
  - [x] 1.1 Add `customizations: List<String> = emptyList()` field to TicketLineItem data class
    - Modify `TicketLineItem` in `TicketFormatter.kt` to include the new field with default empty list
    - Ensure backward compatibility (existing callers passing no customizations still compile)
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Add customization printing logic to `formatClientTicket`
    - After each item line in the `for (item in items)` loop, iterate `item.customizations`
    - For each customization, append `"      - $customization"` (6 spaces + dash + space + name)
    - Items with empty customizations produce no extra lines
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 1.3 Add customization printing logic to `formatInternalTicket`
    - After each item line in the `for (item in items)` loop, iterate `item.customizations`
    - For each customization, append `"      - $customization"` (same format as client ticket)
    - Items with empty customizations produce no extra lines
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x]* 1.4 Write property tests for ticket customization formatting (Properties 1, 2, 3)
    - **Property 1: Customization lines follow the format pattern**
    - **Property 2: Empty customizations produce no customization lines**
    - **Property 3: Customization order preservation in formatted output**
    - **Validates: Requirements 2.2, 2.3, 2.4, 3.2, 3.3, 3.4, 1.3**
    - Use Kotest property testing with generated `List<TicketLineItem>` (random customizations)
    - Assert format pattern `"      - {name}"` for each non-empty customization
    - Assert no pattern match for items with empty customizations
    - Assert relative order of customization lines matches input order

- [x] 2. Checkpoint - Verify ticket formatting
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Add edit state management to PosViewModel
  - [x] 3.1 Add `_editingCartItem` state and `startEditingItem` function
    - Add `private val _editingCartItem = MutableStateFlow<CartItem?>(null)`
    - Add `val editingCartItem: StateFlow<CartItem?> = _editingCartItem`
    - Implement `startEditingItem(cartItemId: String)` — finds item by ID, sets state or no-ops
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 3.2 Add `updateCartItem` function to PosViewModel
    - Implement `updateCartItem(cartItem: CartItem)` — replaces item at same index by ID
    - Recalculate `cartTotal` automatically via existing `cartTotalFlow`
    - If ID not found, leave cart unchanged (no-op)
    - Reset `_editingCartItem` to null after update
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 5.4_

  - [x]* 3.3 Write property tests for edit state management (Properties 4, 5, 6, 7)
    - **Property 4: startEditingItem correctly resolves to cart item or null**
    - **Property 5: updateCartItem replaces in-place preserving list order**
    - **Property 6: Cart total equals sum of item prices after update**
    - **Property 7: updateCartItem is a no-op for non-existent IDs**
    - **Validates: Requirements 5.2, 5.3, 6.1, 6.2, 6.3, 6.4**
    - Use Kotest property testing with generated cart item lists and random IDs/updates

- [x] 4. Add tap-to-edit interaction to CartPanel
  - [x] 4.1 Add `onItemClick` callback parameter to CartPanel
    - Add `onItemClick: (CartItem) -> Unit` parameter to `CartPanel` composable
    - Pass through to each `CartItemRow`
    - _Requirements: 4.1_

  - [x] 4.2 Make CartItemRow clickable with accessible label
    - Add `onClick: () -> Unit` parameter to `CartItemRow`
    - Apply `.clickable(onClickLabel = "Editar artículo", onClick = onClick)` to the Row inside SwipeToDismissBox
    - Ensure swipe-to-delete and tap coexist without interference
    - _Requirements: 4.2, 4.3, 4.4_

- [x] 5. Add edit mode to ProductModal
  - [x] 5.1 Add `editingCartItem` parameter and pre-fill logic
    - Add optional `editingCartItem: CartItem? = null` parameter to `ProductModal`
    - When non-null: initialize `quantity` from `editingCartItem.quantity`
    - When non-null: initialize `extraNotes` from `editingCartItem.extraNotes`
    - When non-null: pre-populate `checkboxSelections` from `editingCartItem.selectedCustomizations` by `optionId`
    - When non-null: pre-populate `radioSelections` from `editingCartItem.selectedCustomizations` by `optionId`
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 5.2 Update confirm button text and callback for edit mode
    - Change confirm button text to `"Actualizar"` when `editingCartItem != null`
    - On confirm in edit mode: build CartItem preserving `editingCartItem.id` instead of generating new UUID
    - Call `onAddToCart` (or a new `onUpdate` callback) with the updated CartItem
    - _Requirements: 7.4, 7.5_

- [x] 6. Wire edit flow in PosScreen
  - [x] 6.1 Update PosScreen to observe `editingCartItem` and pass `onItemClick` to CartPanel
    - Collect `viewModel.editingCartItem` as state
    - Pass `onItemClick = { viewModel.startEditingItem(it.id) }` to CartPanel
    - When `editingCartItem` is non-null, open ProductModal in edit mode with corresponding product data
    - _Requirements: 4.1, 5.2_

  - [x] 6.2 Handle edit mode dismiss and update callbacks in PosScreen
    - On modal dismiss: reset editing state (call a `cancelEditing()` function or set null)
    - On update confirm: call `viewModel.updateCartItem(updatedItem)` instead of `addToCart`
    - Ensure modal closes after update
    - _Requirements: 5.4, 6.1_

- [x] 7. Update TicketLineItem construction in confirmPayment to include customizations
  - [x] 7.1 Map CartItem customizations to TicketLineItem in PosViewModel.confirmPayment()
    - In the `ticketLineItems = cartItems.map { ... }` block, populate `customizations` field
    - Set `customizations = item.selectedCustomizations.map { it.optionName }`
    - Preserve order from `selectedCustomizations`
    - _Requirements: 8.1, 8.2, 8.3_

  - [x]* 7.2 Write property test for CartItem-to-TicketLineItem mapping (Property 8)
    - **Property 8: CartItem-to-TicketLineItem mapping preserves customization names and order**
    - **Validates: Requirements 8.1, 8.3**
    - Generate random CartItems with varying customization lists
    - Assert constructed TicketLineItem.customizations matches optionName values in same order

- [x] 8. Final checkpoint - Ensure all tests pass
  - Compile the project with `./gradlew assembleDebug`
  - Run all unit tests with `./gradlew testDebugUnitTest`
  - Verify no regressions in existing ticket formatting tests
  - Ensure property-based tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The design uses Kotlin throughout — all implementations use existing Kotlin/Compose patterns
- TicketFormatter changes are backward-compatible due to default `emptyList()` on the new field
- ProductModal reuses existing state management with conditional initialization for edit mode
- No database schema changes required — all modifications are in-memory UI state and formatting logic

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "3.2"] },
    { "id": 2, "tasks": ["1.4", "3.3", "4.1"] },
    { "id": 3, "tasks": ["4.2", "5.1"] },
    { "id": 4, "tasks": ["5.2", "6.1"] },
    { "id": 5, "tasks": ["6.2", "7.1"] },
    { "id": 6, "tasks": ["7.2"] }
  ]
}
```
