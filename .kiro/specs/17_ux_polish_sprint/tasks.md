# Implementation Plan: UX Polish Sprint (17_ux_polish_sprint)

## Overview

Implementación incremental de seis mejoras UX/UI: (1) Navegación directa desde HomeScreen al POS al tocar un MenuItemCard, (2) Lógica inteligente de toggle para el botón de tijeras (prevención de dividers consecutivos), (3) Eliminación del componente muerto ExactPaymentInput, (4) Ajuste del botón "Limpiar" para reset selectivo de campos cash, (5) Botón de regreso en el CheckoutPanel, (6) Estado visual glow para botones de completar orden. El código se organiza en capas: ViewModel logic → UI composables → wiring → cleanup.

## Tasks

- [x] 1. ViewModel logic: Smart scissors toggle and selective clear
  - [x] 1.1 Implement `toggleDivider()` in PosViewModel
    - Add `fun toggleDivider()` to `PosViewModel` with the following logic:
      - If `_editingCartItem.value` is a divider → remove that specific divider by id, clear editing state, return
      - If `_cartItems.value` is empty → no-op, return
      - If last item in `_cartItems` has `isDivider == true` → remove last item (toggle off)
      - Otherwise → append a new CartItem with `isDivider = true`, `productName = "--- DIVISOR ---"`, `productId = ""`, `emoji = ""`, `basePrice = 0.0`, `quantity = 1`, `selectedCustomizations = emptyList()`, `extraNotes = ""`, `totalPrice = 0.0`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 1.2 Modify `clearCashReceived()` in PosViewModel for selective reset
    - Change `clearCashReceived()` to use `_checkoutState.value.copy(denominationCounts = emptyMap(), customAmounts = emptyList(), cashReceived = 0.0)` — preserving `customerName`, `paymentStatus`, `printAttempts`, and `isPrinting`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x]* 1.3 Write property tests for scissors toggle (Properties 1–4)
    - **Property 1: Scissors toggle appends or removes last divider**
    - **Property 2: No consecutive dividers invariant**
    - **Property 3: Scissors preserves non-divider items**
    - **Property 4: Scissors removes editing divider at any position**
    - **Validates: Requirements 2.1, 2.2, 2.4, 2.5, 2.6**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/ScissorsTogglePropertyTest.kt`
    - Use Kotest property testing with `Arb.cartItem()`, `Arb.dividerItem()`, and `Arb.validCart()` generators
    - Minimum 100 iterations per property

  - [x]* 1.4 Write property tests for clearCashReceived (Properties 5–6)
    - **Property 5: ClearCashReceived resets all cash-related fields**
    - **Property 6: ClearCashReceived preserves non-cash fields**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
    - Create `app/src/test/java/com/example/puntodeventa/ui/pos/ClearCashReceivedPropertyTest.kt`
    - Use Kotest property testing with `Arb.checkoutState()` generator
    - Minimum 100 iterations per property

- [x] 2. Checkpoint - Verify ViewModel logic compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Navigation: HomeScreen to POS
  - [x] 3.1 Add `onClick` parameter to MenuItemCard
    - In `ui/home/MenuItemCard.kt`, add `onClick: () -> Unit` parameter to the composable
    - Wrap the Card content in a `clickable` modifier with Material 3 ripple indication
    - _Requirements: 1.3_

  - [x] 3.2 Add `onNavigateToPOS` callback to HomeScreen
    - In `ui/home/HomeScreen.kt`, add `onNavigateToPOS: (String) -> Unit` parameter
    - Wire each `MenuItemCard`'s `onClick` to call `onNavigateToPOS(menuItem.id)`
    - When zero Menu_Cards exist, render only the AddMenuCard with no navigation targets
    - _Requirements: 1.1, 1.2, 1.4_

  - [x] 3.3 Wire navigation in MainActivity
    - In `MainActivity.kt`, pass an `onNavigateToPOS` lambda to `HomeScreen` that sets `currentDestination = NavDestination.Pos` and updates `activeMenuId` to the received menuId
    - Ensure tapping the same menu card that is already active re-navigates without error
    - _Requirements: 1.1, 1.2, 1.5_

- [x] 4. UI: CategoryTabBar scissors wiring
  - [x] 4.1 Wire scissors button to `toggleDivider()`
    - In `ui/pos/CategoryTabBar.kt`, change the `onDividerClick` callback to invoke `viewModel.toggleDivider()` instead of the previous `addDivider()`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 5. UI: CheckoutPanel cleanup and back button
  - [x] 5.1 Remove ExactPaymentInput from CheckoutPanel
    - In `ui/pos/CheckoutPanel.kt`, remove the `ExactPaymentInput` composable call
    - Remove the `onAddCustomAmount` parameter from the `CheckoutPanel` function signature
    - Position the "Limpiar" button as a standalone full-width button between the BillsGrid section and the ChangeAssistant section, retaining `onClearCashReceived` callback and `ButtonCancel` color styling
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 5.2 Add back arrow IconButton to CheckoutPanel
    - Add an `onBack: () -> Unit` parameter to the `CheckoutPanel` composable signature
    - Place an `IconButton` with `Icons.Default.ArrowBack` as the first element before the scrollable content area (outside the `verticalScroll` Column)
    - Set `contentDescription = "Regresar al catálogo"`
    - Wire `onBack` to set `isCheckoutVisible = false` in PosViewModel
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 5.3 Delete ExactPaymentInput.kt source file
    - Delete `ui/pos/ExactPaymentInput.kt` from the codebase
    - Remove any imports referencing ExactPaymentInput in other files
    - _Requirements: 3.4_

- [x] 6. UI: Glow state for completion buttons
  - [x] 6.1 Create `glowWhenEnabled` modifier extension in PosHelpers.kt
    - Create or append to `ui/pos/PosHelpers.kt`
    - Implement `fun Modifier.glowWhenEnabled(enabled: Boolean): Modifier` that applies `alpha(1f)` + `shadow(6.dp, RoundedCornerShape(50))` when enabled, and `alpha(0.38f)` with no shadow when disabled
    - Use color values from `Color.kt` (ButtonConfirm) — no hardcoded color literals in the modifier
    - _Requirements: 6.1, 6.2, 6.5_

  - [x] 6.2 Apply glow modifier to Completar Orden button in CheckoutPanel
    - Add `isCompletarEnabled: Boolean` parameter to CheckoutPanel (if not already present)
    - Apply `.glowWhenEnabled(isCompletarEnabled)` to the "Completar Orden" button
    - When enabled: full opacity, ButtonConfirm background, elevation ≥ 6dp
    - When disabled: alpha 0.38, elevation 0dp
    - _Requirements: 6.1, 6.2, 6.6_

  - [x] 6.3 Apply glow modifier to TOTAL button in CartPanel
    - Add `isCompletarEnabled: Boolean` parameter to `CartPanel`
    - Apply `.glowWhenEnabled(isCompletarEnabled)` to the TOTAL button
    - Visual treatment matches the Completar Orden button states
    - _Requirements: 6.3, 6.4, 6.6_

- [x] 7. Wiring and integration
  - [x] 7.1 Wire all new parameters in PosScreen
    - Pass `isCompletarEnabled` from PosViewModel's `isCompletarOrdenEnabled()` to both CartPanel and CheckoutPanel
    - Pass `onBack` lambda to CheckoutPanel that calls `viewModel.hideCheckout()` (or sets `_isCheckoutVisible.value = false`)
    - Remove `onAddCustomAmount` param from the CheckoutPanel call site
    - Ensure `toggleDivider()` is called from CategoryTabBar
    - _Requirements: 1.1, 2.1, 3.2, 5.2, 6.1, 6.3_

- [x] 8. Checkpoint - Full integration verification
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Final cleanup and verification
  - [x] 9.1 Verify no remaining references to ExactPaymentInput
    - Search codebase for any import or usage of `ExactPaymentInput` and remove them
    - Verify CheckoutPanel compiles without the removed parameter
    - _Requirements: 3.1, 3.4_

  - [x]* 9.2 Write unit tests for navigation and glow behavior
    - Test: Tapping MenuItemCard navigates to POS with correct menuId
    - Test: Empty HomeScreen shows only AddMenuCard
    - Test: Re-tapping same menu card is idempotent
    - Test: Back button hides checkout and preserves cart
    - Test: Enabled button has alpha 1.0 and elevation ≥ 6dp
    - Test: Disabled button has alpha 0.38 and elevation 0dp
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 5.2, 5.4, 6.1, 6.2, 6.3, 6.4_

- [x] 10. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at ViewModel, UI, and integration levels
- Property tests validate universal correctness properties from the design document (Properties 1–6)
- The scissors toggle replaces the previous `addDivider()` function with smart toggle behavior
- `clearCashReceived()` is modified in-place (not a new function) to preserve non-cash fields
- The `glowWhenEnabled` modifier uses instant state snapping (no animation) to avoid jitter on fast state changes
- ExactPaymentInput deletion is deferred until after CheckoutPanel is updated to avoid compilation errors

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4", "3.1"] },
    { "id": 2, "tasks": ["3.2", "4.1"] },
    { "id": 3, "tasks": ["3.3", "5.1", "6.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "6.2", "6.3"] },
    { "id": 5, "tasks": ["7.1"] },
    { "id": 6, "tasks": ["9.1", "9.2"] }
  ]
}
```
