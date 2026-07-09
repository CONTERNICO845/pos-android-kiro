# Implementation Plan — 06 Bugfixes Config

> Each bug is an independent commit. Tasks within a bug must be completed in order.
> Exploration and preservation tests are written BEFORE the fix is applied.

---

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "description": "Bug 1 exploration + preservation tests (on unfixed code)",
      "tasks": ["1", "2"]
    },
    {
      "wave": 2,
      "description": "Bug 1 implementation and verification",
      "tasks": ["3", "4"]
    },
    {
      "wave": 3,
      "description": "Bug 2 exploration + preservation tests (on unfixed code)",
      "tasks": ["5", "6"]
    },
    {
      "wave": 4,
      "description": "Bug 2 implementation and verification",
      "tasks": ["7", "8"]
    },
    {
      "wave": 5,
      "description": "Bug 3 exploration + preservation tests (on unfixed code)",
      "tasks": ["9", "10"]
    },
    {
      "wave": 6,
      "description": "Bug 3 implementation and verification",
      "tasks": ["11", "12"]
    },
    {
      "wave": 7,
      "description": "Bug 4 exploration + preservation tests (on unfixed code)",
      "tasks": ["13", "14"]
    },
    {
      "wave": 8,
      "description": "Bug 4 implementation and verification",
      "tasks": ["15", "16"]
    }
  ]
}
```

> Bugs 1–4 are independent of each other and their wave pairs can run in any order.
> Within each bug, the exploration test and preservation test (odd wave) must exist
> on unfixed code before the implementation wave (even wave) is applied.
>
> **Commit boundaries:** one commit per bug — at the checkpoint task (4, 8, 12, 16).
>
> **Cross-bug note (Bugs 2 & 3):** Both require the same one-shot DAO queries
> (`getGroupsByProductOnce`, `getOptionsByGroupOnce`). If committed in order,
> the Bug 2 commit adds them first; the Bug 3 task (11.1) should verify they exist
> rather than re-adding them.

---

---

## Bug 1 — Modal Blinking (Excessive Recomposition)

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** — ModalBottomSheet Recomposition On TextField Change
  - **CRITICAL**: Write this test BEFORE applying the fix. It must FAIL on unfixed code.
  - **GOAL**: Confirm that `ModalBottomSheet` re-enters composition on every `TextField` value change.
  - **Scoped PBT Approach**: Scope the property to the concrete trigger: any `onValueChange`
    callback fired by an `OutlinedTextField` inside `NewProductModal` (name, description, price fields).
  - Use Compose `TestRule` + `composeTestRule.setContent { NewProductModal(…) }`.
  - Type a single character into the "Nombre" field; assert that the recomposition count of
    the `ModalBottomSheet` node is 0 after the keystroke (i.e., only the leaf TextField recomposed).
  - On unfixed code the sheet container recomposes → test FAILS → confirms bug exists.
  - Document the counterexample: `"Typing 'P' in Nombre → ModalBottomSheet recomposed 1 time"`.
  - Mark complete when test is written, run, and failure is documented.
  - _Requirements: 1.1, 1.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** — Modal Lifecycle Behavior Unchanged
  - **IMPORTANT**: Follow observation-first methodology on unfixed code.
  - Observe on unfixed code:
    - Tapping "Cancelar" dismisses the modal and calls `onDismiss()`.
    - When `isSaving = true`, swipe-dismiss is suppressed and the close button is disabled.
    - When `saveResult = Success`, `LaunchedEffect` triggers `onDismiss()`.
    - `rememberScrollState()` survives recomposition (scroll position is preserved).
  - Write property-based tests: for all inputs that are NOT `TextField.onValueChange`
    (i.e., `!isBugCondition_1(event)`), the modal lifecycle behaves identically to the unfixed code.
  - Verify all tests PASS on unfixed code before touching the implementation.
  - _Requirements: 3.1, 3.2, 3.3, 3.4_


- [x] 3. Fix Bug 1 — Split NewProductModal into outer + inner composables

  - [x] 3.1 Refactor `NewProductModal.kt` — split into outer and inner composables
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductModal.kt`
    - Keep the existing `NewProductModal` function signature unchanged
      (`uiState`, `viewModel`, `onDismiss`).
    - Move `val sheetState = rememberModalBottomSheetState(…)` to the top of `NewProductModal`,
      before the `ModalBottomSheet(…)` call — it must NOT be inside a block that reads
      individual `uiState` fields.
    - Move `val scrollState = rememberScrollState()` to the same top-level scope in `NewProductModal`.
    - Keep `LaunchedEffect(uiState.saveResult)` and the `confirmValueChange` lambda in the
      outer `NewProductModal` (both are coarse-grained, legitimate sheet-level concerns).
    - Extract a new private composable `NewProductFormContent` that receives `uiState`,
      `viewModel`, and `scrollState` as parameters.
    - Move the scrollable `Column` (with `Modifier.verticalScroll(scrollState)`) and all
      `OutlinedTextField` composables, group cards, and action row into `NewProductFormContent`.
    - Inside `ModalBottomSheet { … }` call `NewProductFormContent(uiState, viewModel, scrollState)`.
    - Result: `ModalBottomSheet` container and its remembered state are in `NewProductModal`;
      all field-level recompositions are scoped to `NewProductFormContent`.
    - _Bug_Condition: `isBugCondition_1(event)` — any `onValueChange` inside `NewProductModal`
      that previously re-entered `ModalBottomSheet` composition_
    - _Expected_Behavior: only the affected leaf `OutlinedTextField` (and its immediate parent
      `Row`) recomposes; `ModalBottomSheet` recompositionCount = 0 per keystroke_
    - _Preservation: modal dismiss, `isSaving` swipe suppression, scroll state, and
      `LaunchedEffect(saveResult)` behavior are unchanged (Req 3.1–3.4)_
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** — ModalBottomSheet Recomposition On TextField Change
    - **IMPORTANT**: Re-run the SAME test written in task 1 — do NOT write a new test.
    - After the refactor, typing in any `TextField` must NOT cause `ModalBottomSheet` to recompose.
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed).
    - _Requirements: 2.1, 2.2_

  - [x] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** — Modal Lifecycle Behavior Unchanged
    - **IMPORTANT**: Re-run the SAME tests written in task 2 — do NOT write new tests.
    - **EXPECTED OUTCOME**: All tests PASS (confirms no regressions in dismiss, save, scroll behavior).

- [x] 4. Checkpoint Bug 1 — all tests pass
  - Run all Bug 1 tests (`./gradlew connectedAndroidTest` or the relevant test class).
  - Ensure tasks 1–3 are all checked off and all assertions green.
  - Commit with message: `fix(modal): hoist sheet state to outer composable to stop blinking`

---

## Bug 2 — Inactive "Editar" Button

- [x] 5. Write bug condition exploration test
  - **Property 1: Bug Condition** — Editar Does Not Open Pre-Populated Modal
  - **CRITICAL**: Write this test BEFORE applying the fix. It must FAIL on unfixed code.
  - **GOAL**: Confirm that tapping "Editar" leaves `showModal = false` and `uiState.name = ""`.
  - **Scoped PBT Approach**: For any product with non-empty `name`, tapping "Editar" on its
    `ProductCard` should result in `showModal = true` AND `newProductViewModel.uiState.name = product.name`.
  - Use Compose `TestRule`; inject a test product into the ViewModel; simulate the "Editar"
    `DropdownMenuItem` click; assert `showModal = true` and `npState.name = product.name`.
  - On unfixed code both assertions fail → counterexample:
    `"Editar on 'Tacos al Pastor' → showModal=false, uiState.name=''"`.
  - Mark complete when test is written, run, and failure is documented.
  - _Requirements: 1.3, 1.4_

- [x] 6. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** — Nuevo Producto and Cancel Still Work
  - **IMPORTANT**: Follow observation-first methodology on unfixed code.
  - Observe on unfixed code:
    - Tapping "+ Nuevo Producto" opens the modal with empty/default fields.
    - `save()` in create mode inserts a `ProductEntity` with a fresh UUID.
    - Tapping "Cancelar" or "X" dismisses the modal and resets form state.
  - Write property-based tests: for all inputs where `!isBugCondition_2(action)` (i.e., not
    the "Editar" tap), the modal open/close and save behavior are identical to the unfixed code.
  - Verify all tests PASS on unfixed code.
  - _Requirements: 3.5, 3.6, 3.7_


- [x] 7. Fix Bug 2 — Connect Editar to loadForEdit and open modal

  - [x] 7.1 Add one-shot DAO queries to `CustomizationGroupDao` and `CustomizationOptionDao`
    - **File**: `app/src/main/java/com/example/puntodeventa/data/local/CustomizationGroupDao.kt`
      - Add: `@Query("SELECT * FROM customization_groups WHERE productId = :productId")`
        `suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity>`
    - **File**: `app/src/main/java/com/example/puntodeventa/data/local/CustomizationOptionDao.kt`
      - Add: `@Query("SELECT * FROM customization_options WHERE groupId = :groupId")`
        `suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity>`
    - These are the same queries needed by Bug 3; if Bug 3 is committed first, verify they
      already exist before re-adding.
    - _Requirements: 2.4_

  - [x] 7.2 Add `isEditMode` and `editProductId` fields to `NewProductUiState`
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductViewModel.kt`
    - In `data class NewProductUiState`, add after the `error` field:
      ```
      val isEditMode: Boolean = false,
      val editProductId: String? = null
      ```
    - _Requirements: 2.3, 2.4_

  - [x] 7.3 Add `loadForEdit(product: Product)` to `NewProductViewModel`
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductViewModel.kt`
    - Add a new public function `fun loadForEdit(product: Product)` that:
      1. Immediately updates `_uiState` with `isEditMode = true`, `editProductId = product.id`,
         `emoji = product.emoji`, `name = product.name`, `description = product.description`,
         `priceText = product.basePrice.toString()`, `isSaving = false`, `saveResult = null`,
         all error fields = null.
      2. Finds the matching `Category` in `_uiState.value.categories` by `product.categoryId`
         and sets `selectedCategory`.
      3. Launches a coroutine that calls `database.customizationGroupDao().getGroupsByProductOnce(product.id)`,
         then for each group calls `database.customizationOptionDao().getOptionsByGroupOnce(group.id)`,
         converts each `CustomizationGroupEntity` / `CustomizationOptionEntity` pair into a
         `GroupDraft` / `OptionDraft` (reusing `entity.id` as `draftId`), and updates `_uiState.groups`.
    - _Bug_Condition: `isBugCondition_2(action)` — Editar tap with no `loadForEdit` call_
    - _Expected_Behavior: after `loadForEdit`, `uiState.name = product.name`,
      `uiState.isEditMode = true`, `uiState.editProductId = product.id`_
    - _Requirements: 2.3, 2.4_

  - [x] 7.4 Update `save()` in `NewProductViewModel` to handle the edit path
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductViewModel.kt`
    - Inside the `database.withTransaction { … }` block, branch on `s.isEditMode`:
      - **Edit path** (`isEditMode = true`, `editProductId != null`):
        1. Delete all pre-existing groups for `editProductId` via
           `database.customizationGroupDao().getGroupsByProductOnce(editProductId)` then
           `database.customizationGroupDao().deleteById(group.id)` for each
           (CASCADE removes their options automatically).
        2. Insert the `ProductEntity` using the same `editProductId` UUID
           (Room's `OnConflictStrategy.REPLACE` on `ProductDao.insert` handles the upsert).
        3. Insert new groups and options with fresh UUIDs (same loop as the create path).
      - **Create path** (`isEditMode = false`): unchanged — generate a new UUID for `productId`.
    - _Preservation: create-mode save still generates a fresh UUID (Req 3.5, 3.6)_
    - _Requirements: 2.5, 2.6_

  - [x] 7.5 Update `dismiss()` in `NewProductViewModel` to clear edit-mode fields
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductViewModel.kt`
    - In `dismiss()`, the existing reset to `NewProductUiState(menus = …, selectedMenu = …,
      categories = …)` already clears `isEditMode` and `editProductId` because they default
      to `false` / `null`. Verify this is correct — no additional change required unless the
      defaults change.
    - _Requirements: 3.7_

  - [x] 7.6 Update button label in `NewProductModal.kt` for edit mode
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductModal.kt`
    - Change the "Crear producto" / "Guardar" `Button` `Text` to:
      `if (uiState.isEditMode) "Guardar" else "Crear producto"`
    - Also change the header `Text` from `"Nuevo Producto"` to:
      `if (uiState.isEditMode) "Editar Producto" else "Nuevo Producto"`
    - _Requirements: 2.3_

  - [x] 7.7 Update `onEditar` lambda in `ConfigurationScreen.kt`
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationScreen.kt`
    - In `ProductCard`'s `onEditar` callback, change the lambda signature from
      `(productId: String) -> Unit` to `(product: Product) -> Unit` at the call site.
    - Replace the existing log-only body with:
      ```
      onEditar = { product ->
          newProductViewModel.loadForEdit(product)
          showModal = true
          viewModel.setExpandedProductMenu(null)
      }
      ```
    - Update the `ProductCard` composable signature and any intermediate lambdas in
      `ProductCard` / `ProductActionMenu` to pass the full `Product` object instead of
      just `productId`. The full `Product` is already available via `filteredProducts`.
    - Add import for `com.example.puntodeventa.data.model.Product` if not already present.
    - _Bug_Condition: `isBugCondition_2(action)` — Editar tap with no modal open_
    - _Expected_Behavior: `showModal = true`, `npState.name = product.name`_
    - _Requirements: 2.3, 2.4_

  - [x] 7.8 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** — Editar Opens Pre-Populated Modal
    - **IMPORTANT**: Re-run the SAME test written in task 5 — do NOT write a new test.
    - **EXPECTED OUTCOME**: Test PASSES (confirms modal opens pre-populated in edit mode).
    - _Requirements: 2.3, 2.4, 2.5, 2.6_

  - [x] 7.9 Verify preservation tests still pass
    - **Property 2: Preservation** — Nuevo Producto and Cancel Still Work
    - **IMPORTANT**: Re-run the SAME tests written in task 6 — do NOT write new tests.
    - **EXPECTED OUTCOME**: All tests PASS (no regressions in create mode or dismiss behavior).

- [-] 8. Checkpoint Bug 2 — all tests pass
  - Run all Bug 2 tests.
  - Ensure tasks 5–7 are all checked off and all assertions green.
  - Commit with message: `fix(editar): wire loadForEdit() and open modal on Editar tap`

---

## Bug 3 — Duplication Shared IDs (Incomplete Deep Copy)

- [ ] 9. Write bug condition exploration test
  - **Property 1: Bug Condition** — Duplicate Has Fewer Groups Than Original
  - **CRITICAL**: Write this test BEFORE applying the fix. It must FAIL on unfixed code.
  - **GOAL**: Confirm that duplicating a product with ≥1 groups produces a duplicate
    with 0 groups (missing deep copy).
  - **Scoped PBT Approach**: Seed the in-memory Room test database with a product that has
    2 `CustomizationGroupEntity` rows. Call `configurationViewModel.duplicateProduct(product)`.
    Assert that `groupDao.getGroupsByProductOnce(duplicate.id).size == 2`.
  - On unfixed code the duplicate has 0 groups → assertion fails → confirms bug exists.
  - Counterexample: `"Tacos al Pastor (2 groups) duplicated → duplicate.groups.size = 0"`.
  - Mark complete when test is written, run, and failure is documented.
  - _Requirements: 1.5, 1.6_

- [ ] 10. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** — Products With No Groups and Active-Toggle Independence
  - **IMPORTANT**: Follow observation-first methodology on unfixed code.
  - Observe on unfixed code:
    - Duplicating a product with 0 groups produces a copy with only a new product UUID
      and 0 groups (correct behavior — not a bug).
    - Toggling `isActive` on the original product only affects the original's `ProductEntity` row.
    - Deleting the original product does not delete the duplicate.
  - Write property-based tests:
    - For all products P where `isBugCondition_3(P)` is false (0 groups), the duplicate
      has only a new product UUID and 0 groups.
    - For all products P, `toggleProductActive(P)` does not change `P'.isActive`.
    - For all products P, deleting P does not remove P' from the database.
  - Verify all tests PASS on unfixed code.
  - _Requirements: 3.8, 3.9, 3.10_

- [ ] 11. Fix Bug 3 — Add deepCopyProduct() Room transaction to ProductRepository

  - [ ] 11.1 Add one-shot DAO queries (if not already added in task 7.1)
    - **File**: `app/src/main/java/com/example/puntodeventa/data/local/CustomizationGroupDao.kt`
      - Add: `@Query("SELECT * FROM customization_groups WHERE productId = :productId")`
        `suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity>`
    - **File**: `app/src/main/java/com/example/puntodeventa/data/local/CustomizationOptionDao.kt`
      - Add: `@Query("SELECT * FROM customization_options WHERE groupId = :groupId")`
        `suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity>`
    - If these were already added for Bug 2, verify they are present and skip duplication.
    - _Requirements: 2.7_

  - [ ] 11.2 Add `deepCopyProduct(product: Product)` to `ProductRepository`
    - **File**: `app/src/main/java/com/example/puntodeventa/data/repository/ProductRepository.kt`
    - Add a `private val database: AppDatabase` constructor parameter (see task 11.3).
    - Add the function:
      ```kotlin
      suspend fun deepCopyProduct(original: Product) {
          val newProductId = UUID.randomUUID().toString()
          database.withTransaction {
              productDao.insert(original.toEntity().copy(id = newProductId))
              val origGroups = groupDao.getGroupsByProductOnce(original.id)
              for (origGroup in origGroups) {
                  val newGroupId = UUID.randomUUID().toString()
                  groupDao.insertInternal(
                      origGroup.copy(id = newGroupId, productId = newProductId)
                  )
                  val origOptions = optionDao.getOptionsByGroupOnce(origGroup.id)
                  for (origOption in origOptions) {
                      optionDao.insert(
                          origOption.copy(id = UUID.randomUUID().toString(), groupId = newGroupId)
                      )
                  }
              }
          }
      }
      ```
    - Add `import java.util.UUID` and `import androidx.room.withTransaction` if not present.
    - _Bug_Condition: `isBugCondition_3(product)` — duplicate has fewer groups than original_
    - _Expected_Behavior: duplicate has all groups with new UUIDs, all options with new UUIDs,
      all IDs pairwise distinct across both levels_
    - _Preservation: original product, groups, options are never modified; transaction rolls
      back on any failure leaving no partial rows (Req 2.8, 2.9, 3.8–3.10)_
    - _Requirements: 2.7, 2.8, 2.9_

  - [ ] 11.3 Add `database: AppDatabase` parameter to `ProductRepository` constructor
    - **File**: `app/src/main/java/com/example/puntodeventa/data/repository/ProductRepository.kt`
    - Change class declaration to:
      ```kotlin
      class ProductRepository(
          private val productDao: ProductDao,
          private val groupDao: CustomizationGroupDao,
          private val optionDao: CustomizationOptionDao,
          private val database: AppDatabase
      )
      ```
    - _Requirements: 2.7, 2.8_

  - [ ] 11.4 Update `ConfigurationViewModel.duplicateProduct()` to call `deepCopyProduct()`
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationViewModel.kt`
    - Replace the existing `productRepository.insert(product.copy(id = UUID.randomUUID().toString()))`
      with `productRepository.deepCopyProduct(product)`.
    - The `try/catch` block, `_expandedMenuId.value = null`, and error handling remain unchanged.
    - Remove `import java.util.UUID` from `ConfigurationViewModel` if it is no longer used there.
    - _Requirements: 2.7_

  - [ ] 11.5 Update `MainActivity` to pass `database` to `ProductRepository` constructor
    - **File**: `app/src/main/java/com/example/puntodeventa/MainActivity.kt`
    - Change the `ProductRepository` construction to:
      ```kotlin
      val productRepo = ProductRepository(
          productDao = db.productDao(),
          groupDao   = db.customizationGroupDao(),
          optionDao  = db.customizationOptionDao(),
          database   = db
      )
      ```
    - No other changes to `MainActivity` are required.
    - _Requirements: 2.7_

  - [ ] 11.6 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** — Duplicate Has Same Number of Groups With New IDs
    - **IMPORTANT**: Re-run the SAME test written in task 9 — do NOT write a new test.
    - Also assert: `duplicate.id ≠ original.id`, all group IDs differ, all option IDs differ.
    - **EXPECTED OUTCOME**: Test PASSES (confirms deep copy is complete and IDs are independent).
    - _Requirements: 2.7, 2.8, 2.9_

  - [ ] 11.7 Verify preservation tests still pass
    - **Property 2: Preservation** — No-Group Products, Active Toggle, Delete Independence
    - **IMPORTANT**: Re-run the SAME tests written in task 10 — do NOT write new tests.
    - **EXPECTED OUTCOME**: All tests PASS (no regressions in zero-group duplication,
      active toggle, or cascade delete isolation).

- [ ] 12. Checkpoint Bug 3 — all tests pass
  - Run all Bug 3 tests.
  - Ensure tasks 9–11 are all checked off and all assertions green.
  - Commit with message: `fix(duplicate): deep copy groups and options in a Room transaction`

---

## Bug 4 — Missing Top Padding on LazyColumn

- [ ] 13. Write bug condition exploration test
  - **Property 1: Bug Condition** — LazyColumn Has No Top Padding
  - **CRITICAL**: Write this test BEFORE applying the fix. It must FAIL on unfixed code.
  - **GOAL**: Confirm that the first `ProductCard` top edge is flush against the bottom of
    `ActionBarRow` (zero gap).
  - **Scoped PBT Approach**: Render `ConfigurationScreen` in a `composeTestRule` with at
    least one product. Measure the pixel offset of the first `ProductCard` relative to the
    `Box` container. Assert that `firstCard.topOffset > 0.dp`.
  - On unfixed code `topOffset = 0` → assertion fails → confirms bug exists.
  - Counterexample: `"ConfigurationScreen with 1 product → first ProductCard.y == Box.y (gap = 0)"`.
  - Mark complete when test is written, run, and failure is documented.
  - _Requirements: 1.7, 1.8_

- [ ] 14. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** — Empty State and Scroll Behavior Unchanged
  - **IMPORTANT**: Follow observation-first methodology on unfixed code.
  - Observe on unfixed code:
    - When `filteredProducts` is empty, an empty-state `Text` is shown centered in the `Box`.
    - `CategoryTabsRow` and `ActionBarRow` remain pinned at the top regardless of scroll.
    - Scrolling the `LazyColumn` reveals all products.
  - Write property-based tests: for all states where `!isBugCondition_4(screen)` would hold
    after the fix (empty list, scroll actions), the layout behavior is identical to the unfixed code.
  - Verify all tests PASS on unfixed code.
  - _Requirements: 3.11, 3.12_

- [ ] 15. Fix Bug 4 — Apply contentPadding to LazyColumn and top padding to Box

  - [ ] 15.1 Add `contentPadding` to the `LazyColumn` in `ConfigurationScreen`
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationScreen.kt`
    - Locate the `LazyColumn` inside the `else` branch of the state-machine `when` block.
    - Add `contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)` as a named argument.
    - Add `import androidx.compose.foundation.layout.PaddingValues` if not present.
    - The `modifier` line stays unchanged: `Modifier.fillMaxSize().padding(horizontal = 8.dp)`.
    - _Bug_Condition: `LazyColumn.contentPadding.top = 0.dp`_
    - _Expected_Behavior: `LazyColumn.contentPadding.calculateTopPadding() = 8.dp`,
      first `ProductCard` inset 8 dp below the `Box` top edge_
    - _Requirements: 2.10, 2.11_

  - [ ] 15.2 Add `Modifier.padding(top = 8.dp)` to the `Box` container
    - **File**: `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationScreen.kt`
    - Change the `Box` modifier from:
      `Box(modifier = Modifier.fillMaxSize())`
      to:
      `Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp))`
    - This ensures empty-state `Text` composables (loading spinner, error, empty messages)
      are also inset 8 dp from the top and do not jump position when transitioning from
      empty to populated list.
    - _Preservation: empty-state text remains centered in the reduced content area (Req 3.11)_
    - _Requirements: 2.10, 2.11_

  - [ ] 15.3 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** — First ProductCard Has Top Gap
    - **IMPORTANT**: Re-run the SAME test written in task 13 — do NOT write a new test.
    - **EXPECTED OUTCOME**: Test PASSES (`firstCard.topOffset > 0.dp` is satisfied).
    - _Requirements: 2.10, 2.11_

  - [ ] 15.4 Verify preservation tests still pass
    - **Property 2: Preservation** — Empty State and Scroll Behavior Unchanged
    - **IMPORTANT**: Re-run the SAME tests written in task 14 — do NOT write new tests.
    - **EXPECTED OUTCOME**: All tests PASS (no regressions in empty state or scroll behavior).

- [ ] 16. Checkpoint Bug 4 — all tests pass
  - Run all Bug 4 tests.
  - Ensure tasks 13–15 are all checked off and all assertions green.
  - Commit with message: `fix(padding): add contentPadding to LazyColumn and top padding to Box`
