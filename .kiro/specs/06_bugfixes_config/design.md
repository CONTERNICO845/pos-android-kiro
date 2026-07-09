# 06 Bugfixes Config — Design

## Overview

This document formalizes the fix approach for four bugs found in the Configuration Screen
and New Product Modal (Phases 2 and 3). Each bug is treated independently with its own bug
condition, root cause analysis, implementation plan, and testing strategy.

| # | Bug | Primary File(s) Changed |
|---|-----|------------------------|
| 1 | Modal Blinking (excessive recomposition) | `NewProductModal.kt` |
| 2 | Inactive "Editar" button | `ConfigurationScreen.kt`, `NewProductViewModel.kt`, `NewProductModal.kt` |
| 3 | Duplication shared IDs | `ProductRepository.kt`, `AppDatabase.kt` (new DAO query), `ConfigurationViewModel.kt` |
| 4 | Missing top padding on LazyColumn | `ConfigurationScreen.kt` |

---

## Glossary

- **Bug_Condition (C)**: The predicate that identifies inputs/states that trigger the defect.
- **Property (P)**: The correct observable behavior that must hold for all C(X) inputs after the fix.
- **Preservation (¬C)**: All inputs/states where the bug condition does NOT hold; these must produce identical output before and after the fix.
- **F / F'**: The original (unfixed) and fixed functions, respectively.
- **ModalBottomSheet container**: The `ModalBottomSheet { … }` call site and its remembered state objects (`sheetState`, `scrollState`).
- **Leaf TextField**: An `OutlinedTextField` that handles a single form field; should be the only composable that re-enters composition on its own `onValueChange`.
- **loadForEdit(product)**: New `NewProductViewModel` function that populates all `NewProductUiState` fields from an existing `Product` domain object, switching the modal to edit mode.
- **editMode**: A new boolean flag (`isEditMode: Boolean`) added to `NewProductUiState` to distinguish create vs. update.
- **deep copy transaction**: A Room `@Transaction` that generates fresh UUIDs for a product and all its children before inserting, leaving the original rows untouched.
- **PaddingValues**: The `PaddingValues` object provided by `Scaffold` (or derived from `WindowInsets`) that encodes the top inset beneath the status bar and app bars.


---

## Bug 1 — Modal Blinking (Excessive Recomposition)

### Bug Condition

The blinking occurs because `rememberModalBottomSheetState` and `rememberScrollState` are
called inside the same composable scope that receives the `uiState` parameter. Any change to
`uiState` (e.g., a `name` field update) causes the entire `NewProductModal` function body to
re-execute, re-evaluating `rememberModalBottomSheetState` and `rememberScrollState` on every
keystroke. Even though `remember` returns the cached object, the Compose runtime still
re-enters the `ModalBottomSheet` lambda and the `Column` lambda, producing a visible blink.

**Formal Specification:**
```
FUNCTION isBugCondition_1(event)
  INPUT: event — onValueChange callback fired by any OutlinedTextField inside NewProductModal
  OUTPUT: boolean

  RETURN event.source = OutlinedTextField
         AND event.container = NewProductModal   // same composable that holds ModalBottomSheet
         AND ModalBottomSheet.recompositionCount > 0  // container re-composed on this event
END FUNCTION
```

**Concrete examples:**
- User types "P" in the Nombre field → `uiState.name` changes → `NewProductModal` recomposes
  → `ModalBottomSheet` re-enters composition → visible blink on the sheet background.
- User types "1" in the Precio field → same cascade.
- User selects an emoji → same cascade (because `emojiPickerExpanded` also lives in `uiState`).

### Root Cause Analysis

**Hypothesized Root Cause:** State objects that should be stable are created inside the
composable that holds the volatile `uiState` parameter.

1. **`rememberModalBottomSheetState` placement**: Declared directly inside `NewProductModal`,
   which is the composable that receives `uiState: NewProductUiState` as a parameter. Any
   recomposition of `NewProductModal` caused by `uiState` changes re-evaluates this call,
   even though `remember` returns the cached state. The Compose diffing algorithm still
   re-enters the `ModalBottomSheet` block.

2. **`rememberScrollState` placement**: Same issue — declared at the same level as `uiState`.
   The scroll position is preserved by `remember`, but the lambda containing the scrollable
   `Column` is re-executed.

3. **Single-composable scope**: The entire form (header, all fields, group cards, action row)
   lives in one large `NewProductModal` function. There is no sub-composable boundary that
   would let Compose skip re-composing the stable sheet container when only a leaf field
   changes.

### Fix Implementation

**File:** `ui/newproduct/NewProductModal.kt`

**Strategy:** Hoist `ModalBottomSheet` state objects so they are stable with respect to
`uiState` changes. Introduce a thin outer composable that holds the sheet state and scroll
state, and delegates form content to a stateless inner composable that receives only the
fields it needs.

**Specific Changes:**

1. **Split `NewProductModal` into two composables:**
   - `NewProductModal` (outer, keeps its current signature): holds `sheetState` and
     `scrollState` via `remember`/`rememberScrollState`. Passes them to `ModalBottomSheet`
     and to the inner composable. Does NOT receive fine-grained field values directly —
     only `uiState` and `viewModel` references, but crucially the `ModalBottomSheet` call
     site is **not** inside a block that reads individual `uiState` fields.
   - `NewProductFormContent` (inner, private): receives individual fields or the full
     `uiState` and contains all `OutlinedTextField` composables. Because this composable
     does not contain `rememberModalBottomSheetState`, a recomposition here does not
     propagate upward to the sheet container.

2. **Correct `remember` scope:** Move `val sheetState = rememberModalBottomSheetState(…)`
   and `val scrollState = rememberScrollState()` to the outermost lambda of `NewProductModal`,
   before the `ModalBottomSheet(…)` call. This ensures they are remembered relative to
   `NewProductModal`'s composition node, not re-created on each `uiState` emission.

3. **Pass `scrollState` explicitly:** `NewProductFormContent` receives `scrollState` as a
   parameter and applies it with `Modifier.verticalScroll(scrollState)`. This keeps the
   scroll position stable across recompositions of the content.

4. **`LaunchedEffect` and `confirmValueChange` remain in the outer composable** because they
   observe `uiState.saveResult` and `uiState.isSaving` respectively — both are coarse-grained
   state changes that legitimately cause a sheet-level side-effect.

**Component diagram after fix:**

```
NewProductModal (outer)
  remember: sheetState, scrollState
  LaunchedEffect(uiState.saveResult)
  ModalBottomSheet(sheetState = sheetState)
    NewProductFormContent(uiState, viewModel, scrollState)   ← recomposes on field changes
      Modifier.verticalScroll(scrollState)
      OutlinedTextField (name)        ← leaf recompose
      OutlinedTextField (description) ← leaf recompose
      OutlinedTextField (price)       ← leaf recompose
      ...
```

**Data flow (unchanged):** `NewProductViewModel._uiState` → `uiState: StateFlow` →
`collectAsStateWithLifecycle()` in `ConfigurationScreen` → passed into `NewProductModal` →
passed into `NewProductFormContent`. The only change is the composable boundary.


---

## Bug 2 — Inactive "Editar" Button

### Bug Condition

When the user taps "Editar" in a `ProductActionMenu`, the current `onEditar` lambda in
`ConfigurationScreen` only logs and dismisses the menu. `NewProductViewModel.loadForEdit`
does not exist yet, so no product data reaches the modal and `showModal` is never set to
`true`.

**Formal Specification:**
```
FUNCTION isBugCondition_2(action)
  INPUT: action — DropdownMenuItem click with label "Editar"
  OUTPUT: boolean

  RETURN action.label = "Editar"
         AND showModal = false after action   // modal did not open
         AND newProductUiState unchanged      // no fields were pre-populated
END FUNCTION
```

**Concrete examples:**
- User taps ⚙ on "Tacos al Pastor" → taps "Editar" → menu closes → nothing else happens.
- `newProductViewModel.uiState.name` remains `""` (not `"Tacos al Pastor"`).
- `ConfigurationScreen.showModal` remains `false`.

### Root Cause Analysis

1. **Missing `loadForEdit` in `NewProductViewModel`**: There is no function that accepts a
   `Product` and populates `NewProductUiState` with its data. The modal has no way to enter
   edit mode.

2. **Missing `isEditMode` flag in `NewProductUiState`**: The modal's "Crear producto" button
   label and the `save()` transaction logic (which always generates a new UUID) are not
   conditioned on whether we are editing an existing record. An edit save must reuse the
   original product `id` and delete pre-existing customization children before re-inserting.

3. **Disconnected `onEditar` lambda in `ConfigurationScreen`**: The lambda currently receives
   only `productId: String` but has no reference to the full `Product` domain object needed
   to call `loadForEdit`. The `ProductCard` composable passes `onEditar = { onEditar(product.id) }`,
   discarding the rest of the product data.

4. **`showModal` not set to `true` in the Editar path**: Even if `loadForEdit` existed, the
   current code path does not set `showModal = true`.

5. **No cleanup of stale customization data on edit-save**: `save()` always inserts new
   entities. For an edit, previously saved groups and options with the same `productId` must
   be deleted inside the transaction before re-inserting to prevent orphans (Req 2.6).

### Fix Implementation

**Files changed:**

| File | Change |
|------|--------|
| `ui/newproduct/NewProductViewModel.kt` | Add `isEditMode`, `editProductId` to `NewProductUiState`; add `loadForEdit(product)`; update `save()` to handle edit path; update `dismiss()` to clear edit fields |
| `ui/newproduct/NewProductModal.kt` | Change "Crear producto" button label to "Guardar" when `isEditMode = true` |
| `ui/configuration/ConfigurationScreen.kt` | Change `onEditar` lambda to accept `Product`; call `newProductViewModel.loadForEdit(product)` then set `showModal = true` |
| `data/repository/ProductRepository.kt` | Add `deleteGroupsByProductId(productId)` helper that deletes all groups (cascading to options) for a product; used inside the edit-save transaction |

**`NewProductUiState` additions:**

```kotlin
data class NewProductUiState(
    // … existing fields …
    val isEditMode: Boolean = false,
    val editProductId: String? = null   // non-null only in edit mode
)
```

**`loadForEdit(product: Product)` — new ViewModel function:**

```
FUNCTION loadForEdit(product: Product)
  // Loads customization groups for this product from the DB
  // (one-shot suspend query, not a Flow, to avoid overwriting draft changes)
  SET isEditMode     = true
  SET editProductId  = product.id
  SET emoji          = product.emoji
  SET name           = product.name
  SET description    = product.description
  SET priceText      = formatPrice(product.basePrice)
  SET selectedCategory from categories list where id = product.categoryId
  SET groups         = [] (load asynchronously from DB — see below)
  SET isSaving       = false
  SET saveResult     = null
  SET all error fields = null
END FUNCTION
```

Loading groups: `loadForEdit` launches a coroutine that calls a one-shot suspend query
`groupDao.getGroupsByProductOnce(productId)` and for each group calls
`optionDao.getOptionsByGroupOnce(groupId)`, then converts each entity into a `GroupDraft` /
`OptionDraft` with existing `draftId = entity.id` (reuse entity ID as the draft key for
stability). A new one-shot query method must be added to each DAO:

```kotlin
// CustomizationGroupDao — new method
@Query("SELECT * FROM customization_groups WHERE productId = :productId")
suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity>

// CustomizationOptionDao — new method
@Query("SELECT * FROM customization_options WHERE groupId = :groupId")
suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity>
```

**`save()` edit path — changes inside the existing `database.withTransaction` block:**

```
IF isEditMode AND editProductId != null THEN
  // Step 1: Delete all pre-existing groups (CASCADE removes their options)
  FOR ALL group IN getGroupsByProductOnce(editProductId) DO
    groupDao.deleteById(group.id)
  END FOR

  // Step 2: Insert product with the SAME id (REPLACE upserts the ProductEntity)
  productRepository.insert(product.copy(id = editProductId))

  // Step 3: Insert new groups + options (same as create path)
  ...
ELSE
  // Existing create path — generate new UUID
  ...
END IF
```

**`ConfigurationScreen.kt` — `onEditar` lambda change:**

The `onEditar` callback on `ProductCard` is currently typed as `(String) -> Unit` (receives
`productId`). The fix changes it to `(Product) -> Unit` at the `ConfigurationScreen` level.
The `ProductCard` composable signature and `ProductActionMenu` routing are updated accordingly:

```
onEditar = { product ->
    newProductViewModel.loadForEdit(product)
    showModal = true
    viewModel.setExpandedProductMenu(null)
}
```

Because `filteredProducts` already contains the full `Product` object, no additional
repository call is needed at the `ConfigurationScreen` level.

**Data flow — Edit path:**

```
User taps "Editar" on ProductCard
  → onEditar(product) in ConfigurationScreen
  → newProductViewModel.loadForEdit(product)      // populates uiState fields
  → showModal = true                              // triggers NewProductModal render
  → User edits fields
  → User taps "Guardar"
  → newProductViewModel.save()                    // edit path: delete old groups, upsert product
  → LaunchedEffect(saveResult) → onDismiss()
  → newProductViewModel.dismiss()                 // clears editMode
```


---

## Bug 3 — Duplication Shared IDs

### Bug Condition

The current `duplicateProduct` in `ConfigurationViewModel` calls:
```kotlin
productRepository.insert(product.copy(id = UUID.randomUUID().toString()))
```
This creates a new `ProductEntity` row but does NOT copy the `CustomizationGroupEntity` or
`CustomizationOptionEntity` rows. The duplicate product ends up with no customization data,
and the original product's groups/options are completely absent from the duplicate — there is
no sharing in the DB, but the duplicate is incomplete. If the schema were to allow shared FKs
(which Room's cascade delete prevents by design), deleting the original would remove the
shared children. In the current schema the duplicate simply has no children at all, which is
also incorrect per the spec.

**Formal Specification:**
```
FUNCTION isBugCondition_3(product)
  INPUT: product — a Product domain object
  OUTPUT: boolean

  origGroups ← getGroupsByProductOnce(product.id)
  dupProduct ← duplicateProduct(product)   // current broken implementation
  dupGroups  ← getGroupsByProductOnce(dupProduct.id)

  RETURN origGroups.size > 0 AND dupGroups.size ≠ origGroups.size
         // duplicate has fewer (or zero) groups than the original
END FUNCTION
```

**Concrete examples:**
- "Tacos al Pastor" has 2 groups ("Salsa", "Tamaño"), each with 3 options.
  After duplicate: original still has 2 groups × 3 options; duplicate has 0 groups.
- "Agua Fresca" has no customization groups. After duplicate: both products have 0 groups
  (this case is NOT a bug — the duplicate is correctly empty).

### Root Cause Analysis

1. **`duplicateProduct` only calls `productRepository.insert(product.copy(id = newId))`**:
   This inserts only the `ProductEntity`. There is no code that reads the original product's
   `CustomizationGroupEntity` rows, generates new IDs for them, or inserts copies.

2. **No transaction wraps the duplication**: Even if group/option copying were added, doing
   it outside a transaction leaves the DB in a partially-copied state if any step fails.

3. **`ProductRepository` has no deep-copy method**: The repository exposes `insert`,
   `deleteById`, `insertGroup`, and `insertOption` individually. It has no method that
   atomically reads and re-inserts an entire product tree.

4. **`ConfigurationViewModel.duplicateProduct` has no access to the `AppDatabase`** for
   running a transaction. The ViewModel only holds `ProductRepository`.

### Fix Implementation

**Files changed:**

| File | Change |
|------|--------|
| `data/local/CustomizationGroupDao.kt` | Add `getGroupsByProductOnce(productId): List<CustomizationGroupEntity>` |
| `data/local/CustomizationOptionDao.kt` | Add `getOptionsByGroupOnce(groupId): List<CustomizationOptionEntity>` |
| `data/repository/ProductRepository.kt` | Add `deepCopyProduct(product: Product)` — the `@Transaction`-annotated function |
| `ui/configuration/ConfigurationViewModel.kt` | Change `duplicateProduct` to call `productRepository.deepCopyProduct(product)` |
| `MainActivity.kt` | Pass `database` to `ProductRepository` constructor (or inject via a new parameter) so the repository can call `database.withTransaction` |

**DAO additions (suspend, one-shot):**

```kotlin
// CustomizationGroupDao
@Query("SELECT * FROM customization_groups WHERE productId = :productId")
suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity>

// CustomizationOptionDao
@Query("SELECT * FROM customization_options WHERE groupId = :groupId")
suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity>
```

**`ProductRepository.deepCopyProduct` — Room `@Transaction` design:**

The function is annotated with `@Transaction` (or wraps the body in `database.withTransaction`)
so all inserts are committed atomically. The algorithm:

```
FUNCTION deepCopyProduct(original: Product)
  newProductId ← UUID.randomUUID()

  BEGIN TRANSACTION
    // 1. Insert the new ProductEntity with a fresh UUID
    productDao.insert(original.toEntity().copy(id = newProductId))

    // 2. Read the original's groups (one-shot, inside the transaction)
    origGroups ← groupDao.getGroupsByProductOnce(original.id)

    FOR EACH origGroup IN origGroups DO
      newGroupId ← UUID.randomUUID()

      // 3. Insert a copy of the group with new IDs
      groupDao.insertInternal(
        origGroup.copy(id = newGroupId, productId = newProductId)
      )

      // 4. Read the original group's options
      origOptions ← optionDao.getOptionsByGroupOnce(origGroup.id)

      FOR EACH origOption IN origOptions DO
        // 5. Insert a copy of the option with a new ID
        optionDao.insert(
          origOption.copy(id = UUID.randomUUID(), groupId = newGroupId)
        )
      END FOR
    END FOR
  END TRANSACTION
END FUNCTION
```

**Why `@Transaction` / `withTransaction`:**
- `getGroupsByProductOnce` and `getOptionsByGroupOnce` are reads inside the same transaction,
  so they see a consistent snapshot.
- If any insert fails (e.g., a constraint violation), Room rolls back all inserts — the
  database is left in a clean state with neither the new product nor any of its partial
  children.
- The original product and its children are never modified; only new rows are inserted.

**`ConfigurationViewModel` change:**
The ViewModel must have access to the repository's `deepCopyProduct`. Since `ProductRepository`
already holds `groupDao` and `optionDao`, and `AppDatabase` is passed to `NewProductViewModel`
(for its transaction), the cleanest approach is to also pass `AppDatabase` to
`ProductRepository`:

```kotlin
class ProductRepository(
    private val productDao: ProductDao,
    private val groupDao: CustomizationGroupDao,
    private val optionDao: CustomizationOptionDao,
    private val database: AppDatabase          // NEW parameter
)
```

`MainActivity` already constructs `ProductRepository` with three parameters; adding `database`
is a one-line change. The `NewProductViewModel.Factory` already holds `database` and will
still pass it when constructing the repository.

`ConfigurationViewModel.duplicateProduct` becomes:

```kotlin
fun duplicateProduct(product: Product) {
    viewModelScope.launch {
        try {
            productRepository.deepCopyProduct(product)
            _expandedMenuId.value = null
        } catch (e: Exception) {
            _error.value = e.message ?: "Error desconocido"
            _expandedMenuId.value = null
        }
    }
}
```

**Independence guarantee:** After the transaction commits, `origProduct.id` and
`newProduct.id` are distinct primary keys. The FK chain is:
- `newProduct.id` → new group IDs → new option IDs (all fresh UUIDs)
- `origProduct.id` → original group IDs → original option IDs (untouched)

Cascading `DELETE` on the original will only remove the original's children. Toggling
`isActive` on either product only touches the `ProductEntity` row for that product's ID.


---

## Bug 4 — Missing Top Padding

### Bug Condition

The `LazyColumn` in `ConfigurationScreen` is placed inside a `Box(modifier = Modifier.fillMaxSize())`
with `Modifier.fillMaxSize().padding(horizontal = 8.dp)` but no top padding. The `Column`
wrapping `CategoryTabsRow`, `ActionBarRow`, and the `Box` uses `fillMaxSize()`, which means
the `Box` starts immediately after the `ActionBarRow` with no gap.

**Formal Specification:**
```
FUNCTION isBugCondition_4(screen)
  INPUT: screen — ConfigurationScreen rendered in the activity window
  OUTPUT: boolean

  RETURN LazyColumn.contentPadding.top = 0.dp
         AND firstProductCard.topEdge touches ActionBarRow.bottomEdge
END FUNCTION
```

**Concrete examples:**
- Device with any product list: the first `ProductCard` card starts at `y = 0` relative to
  the `LazyColumn` viewport, i.e., immediately flush against the bottom of `ActionBarRow`.
- On a device running edge-to-edge: the status bar inset propagates into the `Column`'s
  layout, but the `LazyColumn` itself still has no intra-screen breathing room.

### Root Cause Analysis

1. **No `contentPadding` on the `LazyColumn`**: The current code is:
   ```kotlin
   LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp))
   ```
   There is no `contentPadding` argument and no `Modifier.padding(top = …)`.

2. **The `Column` wrapper does not apply `PaddingValues`**: `ConfigurationScreen` is called
   from `MainActivity` inside a `when(currentDestination)` block that is not wrapped by a
   `Scaffold`. No `PaddingValues` are threaded through. The outer `Column` in
   `ConfigurationScreen` uses `fillMaxSize()` with no padding modifier.

3. **`ActionBarRow` has `padding(8.dp)` on its container `Row`**, providing internal spacing
   for the action bar's own content, but this does not add space below the bar for the list.

### Fix Implementation

**File:** `ui/configuration/ConfigurationScreen.kt`

**Strategy:** Apply `contentPadding` to the `LazyColumn` and a matching top padding to the
empty-state `Text` composables. Use a consistent `8.dp` top spacing that provides visual
breathing room between the `ActionBarRow` and the first card.

**Specific Changes:**

1. **Add `contentPadding` to `LazyColumn`:**
   ```kotlin
   LazyColumn(
       modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
       contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
   )
   ```
   `contentPadding` is preferred over `Modifier.padding(top = …)` because it adds space
   inside the scrollable area — the first card is inset by 8 dp from the top of the
   viewport, and the bottom spacing ensures the last card is not obscured by a navigation bar.

2. **Add `Modifier.padding(top = 8.dp)` to the `Box` that contains all the state-machine
   branches** (loading spinner, error text, empty-state texts), so those centered views also
   have the same breathing room:
   ```kotlin
   Box(modifier = Modifier
       .fillMaxSize()
       .padding(top = 8.dp)
   )
   ```
   This ensures that when the list transitions to an empty state, the centered text does not
   jump position.

3. **No `Scaffold` change required**: Because the `ConfigurationScreen` `Column` already
   starts below the `ActionBarRow` (which is drawn by the `Column` layout above the `Box`),
   the status bar inset is handled at the `MainActivity` / `AppNavRail` level. The fix is
   purely intra-screen spacing between the action bar and the scrollable content area.

**Before vs. after layout:**

```
Before:
  Column
    CategoryTabsRow       (height ~48dp)
    ActionBarRow          (height ~56dp)
    Box (fillMaxSize)
      LazyColumn ← item 0 starts at y=0, touching the Box top edge

After:
  Column
    CategoryTabsRow       (height ~48dp)
    ActionBarRow          (height ~56dp)
    Box (fillMaxSize, padding(top=8.dp))
      LazyColumn (contentPadding top=8.dp) ← item 0 starts 8dp below Box top
```


---

## Correctness Properties

### Property 1: Bug 1 — Sheet Container Stability

_For any_ `onValueChange` callback fired by an `OutlinedTextField` inside `NewProductModal`,
the fixed `NewProductModal` composable SHALL NOT cause the `ModalBottomSheet` container
(its remembered `sheetState` and `scrollState`) to re-enter composition; only the affected
leaf `OutlinedTextField` (and its immediate parent `Row`) SHALL recompose.

**Validates: Requirements 2.1, 2.2**

---

### Property 2: Bug 1 — Preservation of Modal Lifecycle Behavior

_For any_ input that does NOT involve a `TextField` value change (e.g., "Cancelar" tap,
"X" tap, `saveResult = Success`, back-press swipe), the fixed code SHALL produce exactly
the same behavior as the original: modal dismissal, `isSaving` suppression of swipe, and
scroll state continuity are all unaffected.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

---

### Property 3: Bug 2 — Editar Opens Pre-Populated Modal

_For any_ "Editar" tap on a `ProductCard` where the product has non-empty `name`, `emoji`,
`description`, and `basePrice`, the fixed `ConfigurationScreen` SHALL set `showModal = true`
AND the resulting `NewProductUiState` SHALL satisfy:
`name = product.name AND emoji = product.emoji AND priceText ≈ product.basePrice AND isEditMode = true`.

**Validates: Requirements 2.3, 2.4**

---

### Property 4: Bug 2 — Edit Save Upserts with Same ID

_For any_ save of an edited product where `isEditMode = true`, the fixed `save()` transaction
SHALL insert a `ProductEntity` with `id = editProductId` (not a fresh UUID), and SHALL delete
all pre-existing `CustomizationGroupEntity` rows for that `productId` before inserting the
updated set, leaving no orphaned rows.

**Validates: Requirements 2.5, 2.6**

---

### Property 5: Bug 2 — Preservation of Create-Mode Behavior

_For any_ "Nuevo Producto" tap (i.e., `isEditMode = false`), the fixed code SHALL produce
a new `ProductEntity` with a freshly generated UUID, identical to the original create-mode
behavior.

**Validates: Requirements 3.5, 3.6, 3.7**

---

### Property 6: Bug 3 — Deep Copy ID Independence

_For any_ product P with N ≥ 0 `CustomizationGroupEntity` rows (each with M_i ≥ 0 options),
the fixed `deepCopyProduct(P)` SHALL produce a duplicate P' such that:
- `P'.id ≠ P.id`
- For each group G_i of P, the corresponding copy G'_i satisfies `G'_i.id ≠ G_i.id`
- For each option O_ij of G_i, the copy O'_ij satisfies `O'_ij.id ≠ O_ij.id`
- All new IDs are pairwise distinct across all levels

**Validates: Requirements 2.7, 2.9**

---

### Property 7: Bug 3 — Deep Copy Atomicity

_For any_ invocation of `deepCopyProduct(P)` that fails at any step (e.g., a constraint
violation during option insertion), the fixed transaction SHALL roll back all inserts so
that no partial product, group, or option row exists in the database for the new product ID.

**Validates: Requirement 2.8**

---

### Property 8: Bug 3 — Preservation of Original After Duplication

_For any_ product P and its deep copy P', the fixed code SHALL leave all of P's
`ProductEntity`, `CustomizationGroupEntity`, and `CustomizationOptionEntity` rows
untouched. Toggling `isActive` on P SHALL NOT affect P', and deleting P SHALL NOT
cascade-delete any row belonging to P'.

**Validates: Requirements 3.8, 3.9, 3.10**

---

### Property 9: Bug 4 — Non-Zero Top Padding

_For any_ rendering of `ConfigurationScreen` with at least one product in `filteredProducts`,
the fixed `LazyColumn` SHALL have `contentPadding.calculateTopPadding() > 0.dp`, so the
first `ProductCard`'s top edge does not touch the bottom edge of the `ActionBarRow`.

**Validates: Requirements 2.10, 2.11**

---

### Property 10: Bug 4 — Preservation of Empty-State and Scroll Behavior

_For any_ rendering of `ConfigurationScreen` where `filteredProducts` is empty OR where the
user scrolls the list, the fixed code SHALL produce identical empty-state text placement
(centered in the remaining area) and identical scrolling behavior (all cards reachable) as
the original, with `CategoryTabsRow` and `ActionBarRow` remaining pinned at the top.

**Validates: Requirements 3.11, 3.12**


---

## Testing Strategy

### Validation Approach

Each bug has two test phases:
1. **Exploratory**: Run tests on the unfixed code to confirm the bug manifests as expected
   (counterexample discovery).
2. **Fix + Preservation**: Run tests on the fixed code to confirm the property holds and
   regressions are absent.

---

### Bug 1 Testing

#### Exploratory Bug Condition Checking

**Goal**: Confirm that `ModalBottomSheet` recomposes on every `TextField` change on unfixed code.

**Test Cases:**
1. **Recomposition counter test** (unit, unfixed code): Use Compose test rule with a
   `SemanticsModifier`-based recomposition counter. Set the name field → assert the
   `ModalBottomSheet` node's recomposition count > 0. (Expected to pass on unfixed code,
   demonstrating the bug.)
2. **Scroll state stability test** (unit, unfixed code): Record `scrollState.value` before
   and after a name field update; if `ModalBottomSheet` blinks it may reset scroll position.

#### Fix Checking

**Goal**: Verify sheet container stability after the composable split.

```
FOR ALL event WHERE isBugCondition_1(event) DO
  Before: ModalBottomSheet.recompositionCount > 0
  After:  ModalBottomSheet.recompositionCount = 0
          AND affectedTextField.recompositionCount = 1
END FOR
```

#### Preservation Checking

**Test Cases:**
1. **Cancelar dismissal**: Tap "Cancelar" → `onDismiss()` is called, `showModal = false`.
2. **isSaving swipe suppression**: Set `isSaving = true` → swipe sheet down → sheet stays open.
3. **SaveResult.Success auto-dismiss**: Emit `SaveResult.Success` → `LaunchedEffect` fires → `onDismiss()` called.
4. **Scroll continuity**: Scroll to bottom of form → type in name field → scroll position unchanged.

#### Unit Tests
- Verify `NewProductFormContent` is a separate composable with no `remember` calls for sheet state.
- Verify `rememberModalBottomSheetState` is called exactly once per `NewProductModal` composition node.

#### Property-Based Tests
- Generate random sequences of field updates; assert `ModalBottomSheet` recomposition count remains 0.

#### Integration Tests
- Full modal open → type in all fields → verify no visible blink (screenshot diff or composable test).

---

### Bug 2 Testing

#### Exploratory Bug Condition Checking

**Goal**: Confirm "Editar" tap has no effect on unfixed code.

**Test Cases:**
1. **Modal not opened** (unit, unfixed code): Tap "Editar" → assert `showModal = false`.
2. **Fields not populated** (unit, unfixed code): Tap "Editar" → assert `newProductUiState.name = ""`.

#### Fix Checking

```
FOR ALL action WHERE isBugCondition_2(action) DO
  result ← onEditar'(product)
  ASSERT showModal = true
  ASSERT newProductUiState.name = product.name
  ASSERT newProductUiState.emoji = product.emoji
  ASSERT newProductUiState.isEditMode = true
END FOR
```

#### Preservation Checking

**Test Cases:**
1. **Create mode still works**: Tap "+ Nuevo Producto" → `isEditMode = false`, `name = ""`.
2. **Create mode save generates new UUID**: Save in create mode → new `ProductEntity.id` in DB.
3. **Cancelar in both modes**: Tap "Cancelar" → modal dismissed, state reset.

#### Unit Tests
- `loadForEdit(product)` populates all `NewProductUiState` fields correctly.
- `save()` in edit mode: upserts with existing ID, deletes old groups first.
- `save()` in create mode: generates new UUID (existing behavior).
- `dismiss()` clears `isEditMode` and `editProductId`.

#### Property-Based Tests
- For any `Product` with random fields, `loadForEdit(product)` followed by reading state
  produces fields matching the product.
- For any edit save, the DB contains exactly one `ProductEntity` with the original ID.

#### Integration Tests
- Open edit modal → modify name → save → observe updated name in product list.
- Open edit modal → remove one group → save → verify old group row deleted from DB.

---

### Bug 3 Testing

#### Exploratory Bug Condition Checking

**Goal**: Confirm duplicate has zero groups on unfixed code when original has groups.

**Test Cases:**
1. **Group count mismatch** (instrumented, unfixed code): Insert product with 2 groups → duplicate → query DB for duplicate's groups → assert count = 0 (bug present).
2. **Option sharing** (instrumented, unfixed code): Confirm original's group IDs exist in DB but are not owned by the duplicate.

#### Fix Checking

```
FOR ALL product WHERE isBugCondition_3(product) DO
  duplicate ← deepCopyProduct'(product)
  ASSERT duplicate.id ≠ product.id
  FOR ALL group IN getGroupsByProductOnce(duplicate.id) DO
    ASSERT group.id ∉ original.groupIds
    FOR ALL option IN getOptionsByGroupOnce(group.id) DO
      ASSERT option.id ∉ original.allOptionIds
    END FOR
  END FOR
  ASSERT getGroupsByProductOnce(duplicate.id).size = getGroupsByProductOnce(product.id).size
END FOR
```

#### Preservation Checking

**Test Cases:**
1. **Zero-group product duplicate**: Product with no groups → duplicate → duplicate also has no groups (unchanged).
2. **Original untouched after duplicate**: Duplicate → delete duplicate → original's groups still present.
3. **Toggle independence**: Duplicate → toggle `isActive` on original → duplicate `isActive` unchanged.
4. **Cascade delete independence**: Delete original → duplicate rows still in DB.

#### Unit Tests (instrumented DAO tests)
- `deepCopyProduct` with 0 groups → only 1 new `ProductEntity` row inserted.
- `deepCopyProduct` with 2 groups × 3 options → 1 new product + 2 new groups + 6 new options; all IDs unique.
- Transaction rollback: mock DAO to throw on 2nd group insert → assert no rows were committed.

#### Property-Based Tests
- For N random groups each with M random options: `deepCopyProduct` always produces N groups and sum(M_i) options under the duplicate ID.
- All generated IDs across the entire tree are pairwise distinct.

#### Integration Tests
- Insert product with groups → duplicate → delete original → observe duplicate and its groups in DB.
- Insert product → duplicate → modify original's group name → observe duplicate's group name unchanged.

---

### Bug 4 Testing

#### Exploratory Bug Condition Checking

**Goal**: Confirm zero top padding on unfixed code.

**Test Cases:**
1. **Padding assertion** (Compose test, unfixed code): Find `LazyColumn` node → assert
   `contentPadding.calculateTopPadding() == 0.dp` (bug present).
2. **Visual overlap** (screenshot, unfixed code): Capture screenshot → verify first card
   top edge y-coordinate equals `ActionBarRow` bottom edge y-coordinate.

#### Fix Checking

```
FOR ALL screen WHERE isBugCondition_4(screen) DO
  ASSERT LazyColumn.contentPadding.calculateTopPadding() > 0.dp
  ASSERT firstProductCard.topEdge.y > ActionBarRow.bottomEdge.y
END FOR
```

#### Preservation Checking

**Test Cases:**
1. **Empty product list**: `filteredProducts = emptyList()` → empty-state `Text` still centered.
2. **Scrollable list**: 20 products → LazyColumn scrolls to show all → tabs and action bar stay pinned.
3. **Category switching**: Switch category → new product list appears with same top padding.

#### Unit Tests
- `ConfigurationScreen` composable test: with 1 product, assert `LazyColumn` content padding top ≥ `8.dp`.

#### Property-Based Tests
- For any non-empty `filteredProducts`, the `LazyColumn` first item offset from the top of the `Box` is always > 0.

#### Integration Tests
- Full screen render with products → screenshot comparison before/after to confirm visible gap.

