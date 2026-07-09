# Bugfix Requirements Document

## Introduction

This document covers four bugs identified in the Configuration Screen and New Product Modal
(Phases 2 and 3 of the POS Android app). The bugs affect visual stability during text input,
action connectivity for the "Editar" menu item, product isolation when duplicating, and visual
spacing between the system top bar and the product list. All four bugs must be fixed atomically
per-bug in their own commits and must follow Jetpack Compose and Room best practices.

---

## Bug Analysis

### Current Behavior (Defect)

**Bug 1 — Modal Blinking (Excessive Recomposition)**

1.1 WHEN the user types any character into a `TextField` inside `NewProductModal` THEN the
    system causes the entire `ModalBottomSheet` composable to recompose, producing a visible
    blink or flicker on every keystroke.

1.2 WHEN `NewProductModal` is rendered THEN the system re-evaluates the `ModalBottomSheet`
    container (including `rememberModalBottomSheetState` and `rememberScrollState`) on each
    text-field update because the composable that holds the `ModalBottomSheet` re-enters
    composition.

**Bug 2 — Inactive "Editar" Button**

1.3 WHEN the user taps "Editar" in a product's `DropdownMenu` THEN the system only logs
    `"Editar: <productId>"` and calls `viewModel.setExpandedProductMenu(null)`, but takes no
    further action; the modal does not open and no product data is loaded for editing.

1.4 WHEN the "Editar" action fires THEN the system does not pass the selected `Product` data
    to `NewProductViewModel` in any form, so the modal cannot pre-populate its fields.

**Bug 3 — Duplication Error (Shared IDs)**

1.5 WHEN the user taps "Duplicar" on a product THEN the system calls
    `productRepository.insert(product.copy(id = UUID.randomUUID().toString()))`, which only
    generates a new top-level product UUID but does NOT duplicate the associated
    `CustomizationGroupEntity` and `CustomizationOptionEntity` records.

1.6 WHEN a duplicated product shares the same customization group and option IDs as the
    original THEN the system causes toggling the active state of one product to appear to
    affect shared data, and deleting one product cascades and removes the shared groups/options
    from the other product.

**Bug 4 — Missing Top Padding**

1.7 WHEN `ConfigurationScreen` is displayed THEN the system renders the product `LazyColumn`
    with no top spacing, causing the first `ProductCard` to sit flush against the bottom edge
    of the `ActionBarRow` / system top bar with no breathing room.

1.8 WHEN the `Scaffold` (or the parent `Row` with `enableEdgeToEdge`) provides
    `PaddingValues` THEN the system does not apply those values to the `LazyColumn` content
    area inside `ConfigurationScreen`.

---

### Expected Behavior (Correct)

**Bug 1 — Modal Blinking (Excessive Recomposition)**

2.1 WHEN the user types any character into a `TextField` inside `NewProductModal` THEN the
    system SHALL recompose only the affected leaf `TextField` composable (and its immediate
    parent row), leaving the `ModalBottomSheet` container, the sheet state, and the scroll
    state stable and unaffected.

2.2 WHEN `NewProductModal` is open THEN the system SHALL hoist `ModalBottomSheet` state
    objects (`rememberModalBottomSheetState`, `rememberScrollState`) at the level that wraps
    all text fields, ensuring they are `remember`ed once and do not regenerate on field updates.

**Bug 2 — Inactive "Editar" Button**

2.3 WHEN the user taps "Editar" for a product THEN the system SHALL open `NewProductModal`
    pre-populated with that product's existing data (`emoji`, `name`, `description`,
    `basePrice`, `selectedCategory`, matching `selectedMenu`) so the user can review and
    modify them.

2.4 WHEN the user taps "Editar" for a product THEN the system SHALL pass the target
    `Product` domain object to `NewProductViewModel` via a dedicated `loadForEdit(product)`
    function (or equivalent), which populates all editable `NewProductUiState` fields.

2.5 WHEN the user saves an edited product THEN the system SHALL persist the changes using the
    same product `id` (i.e., an upsert via `OnConflictStrategy.REPLACE`), replacing the
    original record in Room.

2.6 WHEN the user saves an edited product THEN the system SHALL delete all pre-existing
    customization groups and options for that product before inserting the updated set, so
    removed groups/options are not orphaned in the database.

**Bug 3 — Duplication Error (Shared IDs)**

2.7 WHEN the user taps "Duplicar" on a product THEN the system SHALL perform a deep copy that
    generates a new UUID for the duplicated `ProductEntity` AND a new UUID for each
    `CustomizationGroupEntity` AND a new UUID for each `CustomizationOptionEntity` belonging
    to the original product.

2.8 WHEN performing the deep copy THEN the system SHALL persist all three levels
    (`ProductEntity`, `CustomizationGroupEntity`, `CustomizationOptionEntity`) inside a single
    Room transaction, so the duplicate is either fully created or not created at all.

2.9 WHEN the deep copy succeeds THEN the system SHALL result in two fully independent Room
    records where modifying or deleting one product has no effect on the other.

**Bug 4 — Missing Top Padding**

2.10 WHEN `ConfigurationScreen` is displayed THEN the system SHALL apply a top padding
     (sourced from the Scaffold's `PaddingValues` or an explicit `Modifier.padding(top = X.dp)`
     matching the visual specification) to the `LazyColumn` so there is visible breathing room
     between the action bar and the first product card.

2.11 WHEN the screen is displayed on a device with a system status bar THEN the system SHALL
     ensure the `LazyColumn` content starts below both the status bar inset and the
     `ActionBarRow`, with no content obscured.

---

### Unchanged Behavior (Regression Prevention)

**Bug 1 — Modal Blinking (Excessive Recomposition)**

3.1 WHEN the user taps "Cancelar" or the "X" close button THEN the system SHALL CONTINUE TO
    dismiss the modal and reset form state via `NewProductViewModel.dismiss()`.

3.2 WHEN `isSaving` is `true` THEN the system SHALL CONTINUE TO suppress swipe-dismiss and
    disable the close button.

3.3 WHEN `saveResult` transitions to `SaveResult.Success` THEN the system SHALL CONTINUE TO
    auto-dismiss the modal via `LaunchedEffect`.

3.4 WHEN the user scrolls the modal content THEN the system SHALL CONTINUE TO scroll the
    inner `Column` through all fields and group cards.

**Bug 2 — Inactive "Editar" Button**

3.5 WHEN the user taps "+ Nuevo Producto" THEN the system SHALL CONTINUE TO open the modal
    in creation mode with empty/default fields.

3.6 WHEN the user saves a new product (creation mode) THEN the system SHALL CONTINUE TO
    insert a fresh `ProductEntity` with a new UUID.

3.7 WHEN the user taps "Cancelar" or "X" in either creation or edit mode THEN the system
    SHALL CONTINUE TO dismiss the modal and reset form state.

**Bug 3 — Duplication Error (Shared IDs)**

3.8 WHEN the user duplicates a product that has no customization groups THEN the system SHALL
    CONTINUE TO create a copy with only a new product UUID and no group records.

3.9 WHEN the user toggles the active state of the original product after duplication THEN the
    system SHALL CONTINUE TO affect only the original product's `isActive` flag.

3.10 WHEN the user deletes the original product after duplication THEN the system SHALL
     CONTINUE TO leave the duplicated product and all its groups/options untouched.

**Bug 4 — Missing Top Padding**

3.11 WHEN the product list is empty THEN the system SHALL CONTINUE TO show the appropriate
     empty-state text centered in the remaining content area.

3.12 WHEN the user scrolls the product list THEN the system SHALL CONTINUE TO scroll the
     `LazyColumn` to reveal all products, with the `CategoryTabsRow` and `ActionBarRow`
     remaining pinned at the top.

---

## Bug Condition Summary

### Bug Condition Functions

```pascal
// Bug 1 — Modal Blinking
FUNCTION isBugCondition_1(event)
  INPUT: event — any text-field value-change callback inside NewProductModal
  OUTPUT: boolean
  RETURN event.source = TextField AND event.container = NewProductModal
END FUNCTION

// Bug 2 — Inactive Editar
FUNCTION isBugCondition_2(action)
  INPUT: action — DropdownMenuItem click inside ProductActionMenu
  OUTPUT: boolean
  RETURN action.label = "Editar"
END FUNCTION

// Bug 3 — Duplication Shared IDs
FUNCTION isBugCondition_3(product)
  INPUT: product — a ProductEntity that has ≥1 CustomizationGroupEntity
  OUTPUT: boolean
  RETURN duplicateProduct(product) produces groups where group.id ∈ original.groupIds
END FUNCTION

// Bug 4 — Missing Top Padding
FUNCTION isBugCondition_4(screen)
  INPUT: screen — ConfigurationScreen rendered in the activity window
  OUTPUT: boolean
  RETURN LazyColumn.topPadding = 0
END FUNCTION
```

### Fix Properties

```pascal
// Property: Fix Checking — Bug 1
FOR ALL event WHERE isBugCondition_1(event) DO
  result ← handleTextChange'(event)
  ASSERT ModalBottomSheet.recompositionCount = 0
  ASSERT onlyAffectedTextField.recomposed = true
END FOR

// Property: Fix Checking — Bug 2
FOR ALL action WHERE isBugCondition_2(action) DO
  result ← onEditar'(productId)
  ASSERT showModal = true
  ASSERT newProductUiState.name = product.name
  ASSERT newProductUiState.emoji = product.emoji
END FOR

// Property: Fix Checking — Bug 3
FOR ALL product WHERE isBugCondition_3(product) DO
  duplicate ← duplicateProduct'(product)
  ASSERT duplicate.id ≠ product.id
  FOR ALL group IN duplicate.groups DO
    ASSERT group.id ∉ product.groupIds
    FOR ALL option IN group.options DO
      ASSERT option.id ∉ product.allOptionIds
    END FOR
  END FOR
END FOR

// Property: Fix Checking — Bug 4
FOR ALL screen WHERE isBugCondition_4(screen) DO
  ASSERT LazyColumn.topPadding > 0
END FOR

// Property: Preservation Checking (all bugs)
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT F(X) = F'(X)
END FOR
```
