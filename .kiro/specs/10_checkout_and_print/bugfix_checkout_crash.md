# Bugfix: Checkout Panel Crash (Nested Scrolling Violation)

## Crash Location

**File:** `app/src/main/java/com/example/puntodeventa/ui/pos/CashKeypad.kt`  
**Component:** `CashKeypad` composable — specifically the `LazyVerticalGrid` usage (previously lines 64-74)

## Root Cause

`CheckoutPanel.kt` wraps its content in a `Column` with `.verticalScroll(rememberScrollState())`. Inside this scrollable column, `CashKeypad.kt` used a `LazyVerticalGrid` which requires **infinite vertical measurement** by design.

This combination is **illegal in Jetpack Compose**: a lazily-measured component (LazyVerticalGrid, LazyColumn, etc.) cannot be placed inside a vertically-scrollable container without an explicit fixed height constraint. At composition time, Compose attempts to measure the LazyVerticalGrid with infinity max height, triggering:

```
java.lang.IllegalStateException: Vertically scrollable component was measured with 
an infinity maximum height constraints, which is disallowed.
```

The crash occurred immediately when `isCheckoutVisible` became `true` (user presses TOTAL button), causing `CheckoutPanel` to compose and attempt to measure `CashKeypad`.

## Fix Applied

Replaced the `LazyVerticalGrid` in `CashKeypad.kt` with a static `Column` + `Row` layout:

- **Row 1:** $1000, $500, $200, $100
- **Row 2:** $50, $20, $10, $5
- **Row 3:** $2, $1 (+ 2 weighted spacers for alignment)

Since there are only 10 fixed denomination buttons, a lazy layout was never necessary. The static layout:
1. Avoids the nested scrolling crash entirely
2. Has no performance penalty (10 items doesn't benefit from lazy recycling)
3. Preserves all visual appearance (denomination values, badge counts, button styling)
4. The `CashKeypad` Column does NOT have its own `verticalScroll` — the parent `CheckoutPanel` handles all scrolling

## Regression Prevention

- The `DenominationButton` composable was left unchanged
- A ViewModel-level integration test (`CheckoutTransitionCrashTest.kt`) validates the state transition works correctly
- Any future attempt to re-introduce a `LazyVerticalGrid` inside `CashKeypad` would immediately reproduce the crash since `CheckoutPanel` still uses `.verticalScroll()`
- If `CheckoutPanel` ever removes its `verticalScroll`, lazy layouts could be safe again — but for 10 fixed items, the static approach remains preferred

## Related Files

| File | Role |
|------|------|
| `CashKeypad.kt` | **Fixed** — replaced LazyVerticalGrid with Column+Row |
| `CheckoutPanel.kt` | Parent scrollable container (unchanged) |
| `CheckoutTransitionCrashTest.kt` | Integration test validating ViewModel transition |
