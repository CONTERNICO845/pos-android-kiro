# Implementation Plan: 07 Category Improvements

## Overview

Two independent, surgical changes to `ConfigurationScreen`:

1. **Deterministic product ordering** — Add `ORDER BY name COLLATE NOCASE ASC, id ASC` to `ProductDao.getProductsByCategory()` and extend the instrumented DAO tests to assert the ordering invariant.
2. **Category deletion with confirmation** — Add `showDeleteCategoryDialog` to `ConfigurationUiState`, three new public functions to `ConfigurationViewModel`, a trash `IconButton` in the category tabs row, and a `DeleteCategoryDialog` composable with an `AlertDialog`.

Both changes build on the existing MVVM + Room + reactive pipeline without restructuring it.

---

## Tasks

- [x] 1. Fix deterministic product ordering in `ProductDao`
  - [x] 1.1 Add `ORDER BY name COLLATE NOCASE ASC, id ASC` to `ProductDao.getProductsByCategory()`
    - Edit `app/src/main/java/com/example/puntodeventa/data/local/ProductDao.kt`
    - Replace the current `@Query("SELECT * FROM products WHERE categoryId = :categoryId")` with `@Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC, id ASC")`
    - Leave `getActiveProductsByCategory` unchanged (out of scope)
    - _Requirements: 1.1, 1.4_

  - [x]* 1.2 Write instrumented property tests for stable alphabetical ordering (Property 1 & 2)
    - Add test class `ProductDaoOrderingTest` in `app/src/androidTest/java/com/example/puntodeventa/data/local/`
    - **Property 1: Stable alphabetical ordering** — Insert a list of products with mixed-case names under one `categoryId`, collect the first emission of `getProductsByCategory`, assert the result equals the list sorted by `name COLLATE NOCASE ASC, id ASC`
    - **Property 2: Order invariance after isActive toggle** — Insert N products, collect initial order, update `isActive` on one product via `productDao.insert(product.copy(isActive = !product.isActive))`, collect new emission, assert relative order of all products is unchanged
    - Use `runBlocking` + `flow.first()` / `flow.drop(1).first()` following the pattern in `ProductDaoTest.kt`
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. Add `showDeleteCategoryDialog` to `ConfigurationUiState` and extend the reactive pipeline in `ConfigurationViewModel`
  - [x] 2.1 Add `showDeleteCategoryDialog: Boolean = false` field to `ConfigurationUiState` data class
    - Edit `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationViewModel.kt`
    - Append the field to `ConfigurationUiState`
    - _Requirements: 2.3_

  - [x] 2.2 Add `_showDeleteCategoryDialog` `MutableStateFlow` and wire it into the `combine` block
    - In `ConfigurationViewModel`, declare `private val _showDeleteCategoryDialog = MutableStateFlow(false)`
    - Extend the existing 8-argument `combine(...)` call to a 9-argument form using the `Array<*>` lambda overload already in use: add `_showDeleteCategoryDialog` as the 9th stream and `args[8] as Boolean` mapped to `showDeleteCategoryDialog` in the `ConfigurationUiState` constructor call
    - _Requirements: 2.3, 2.8_

  - [x] 2.3 Implement `requestDeleteCategory()`, `dismissDeleteCategoryDialog()`, and `confirmDeleteCategory()` in `ConfigurationViewModel`
    - `requestDeleteCategory()`: sets `_showDeleteCategoryDialog.value = true`
    - `dismissDeleteCategoryDialog()`: sets `_showDeleteCategoryDialog.value = false`
    - `confirmDeleteCategory()`: reads `_selectedCategory.value ?: return`; sets `_showDeleteCategoryDialog.value = false`; launches a coroutine that calls `categoryRepository.deleteById(categoryToDelete.id)`; on success does nothing (the `categoriesFlow` `onEach` handles auto-selecting the next category via `_selectedCategory.value = cats.firstOrNull()`); catches any `Exception`, sets `_error.value = e.message ?: "Error desconocido"`
    - _Requirements: 2.3, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11_

  - [x] 2.4 Write unit tests for the three new ViewModel functions (Properties 4, 5, 6, 8)
    - Add test class `ConfigurationViewModelDeleteCategoryTest` in `app/src/test/java/com/example/puntodeventa/ui/configuration/`
    - Use `TestCoroutineDispatcher` / `UnconfinedTestDispatcher` + MockK to mock `CategoryRepository`
    - **Property 4: Cancel preserves all state** — Call `requestDeleteCategory()` then `dismissDeleteCategoryDialog()`; assert `showDeleteCategoryDialog == false`, `selectedCategory` unchanged, `deleteById` never called
    - **Property 5: Confirm calls deleteById with correct id** — Seed `_selectedCategory` with a known category; call `confirmDeleteCategory()`; verify `deleteById(category.id)` called exactly once
    - **Property 6: Successful deletion clears dialog flag** — Mock `deleteById` to succeed; call `confirmDeleteCategory()`; assert `showDeleteCategoryDialog == false` immediately after call
    - **Property 8: Error handling preserves selectedCategory** — Mock `deleteById` to throw `RuntimeException("error msg")`; call `confirmDeleteCategory()`; assert `selectedCategory` unchanged, `error == "error msg"`, `showDeleteCategoryDialog == false`
    - _Requirements: 2.6, 2.7, 2.8, 2.10, 2.11_

- [x] 3. Checkpoint — Ensure all unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Add trash `IconButton` to `ConfigurationScreen` and wrap `CategoryTabsRow` in a `Row`
  - [x] 4.1 Wrap the `CategoryTabsRow` call in a `Row` and add the conditional trash `IconButton`
    - Edit `app/src/main/java/com/example/puntodeventa/ui/configuration/ConfigurationScreen.kt`
    - Add imports: `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Delete`, `androidx.compose.material3.Icon`, `androidx.compose.material3.IconButton`
    - Replace the standalone `CategoryTabsRow(...)` call (Row 1 comment block) with a `Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically)` that contains `CategoryTabsRow(..., modifier = Modifier.weight(1f))` and an `if (uiState.selectedCategory != null)` guarded `IconButton` calling `viewModel.requestDeleteCategory()` with `Icons.Default.Delete` and `contentDescription = "Eliminar categoría"`
    - _Requirements: 2.1, 2.2_

  - [x]* 4.2 Write instrumented Compose test for trash button visibility (Property 3)
    - Add test class `DeleteCategoryButtonVisibilityTest` in `app/src/androidTest/java/com/example/puntodeventa/ui/configuration/`
    - **Property 3: Trash button visibility matches selectedCategory** — Render `ConfigurationScreen` with a `FakeConfigurationViewModel` (or direct state injection) for two cases: `selectedCategory = null` (assert button not displayed) and `selectedCategory = someCategory` (assert button displayed and enabled)
    - Follow the pattern established in `SimpleComposeTest.kt` / `DuplicarPreservationTest.kt`
    - _Requirements: 2.1, 2.2_

- [x] 5. Implement `DeleteCategoryDialog` composable and wire it into `ConfigurationScreen`
  - [x] 5.1 Add the private `DeleteCategoryDialog` composable to `ConfigurationScreen.kt`
    - Add imports: `androidx.compose.material3.AlertDialog`, `androidx.compose.material3.TextButton`
    - Implement the composable below the `ActionBarRow` composable in the same file:
      ```kotlin
      @Composable
      private fun DeleteCategoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
          AlertDialog(
              onDismissRequest = onDismiss,
              title = { Text("Eliminar categoría") },
              text = { Text("¿Estás seguro? Eliminar esta categoría eliminará permanentemente todos los productos dentro de ella.") },
              confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar") } },
              dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
          )
      }
      ```
    - _Requirements: 2.4, 2.5_

  - [x] 5.2 Show `DeleteCategoryDialog` conditionally in `ConfigurationScreen`
    - Inside `ConfigurationScreen`, after the `if (showModal)` block, add:
      ```kotlin
      if (uiState.showDeleteCategoryDialog) {
          DeleteCategoryDialog(
              onConfirm = { viewModel.confirmDeleteCategory() },
              onDismiss = { viewModel.dismissDeleteCategoryDialog() }
          )
      }
      ```
    - _Requirements: 2.3, 2.6, 2.7_

  - [x] 5.3 Write instrumented Compose tests for `DeleteCategoryDialog` content and error display
    - Add test class `DeleteCategoryDialogTest` in `app/src/androidTest/java/com/example/puntodeventa/ui/configuration/`
    - Test 1: Render `DeleteCategoryDialog` standalone; assert the text "¿Estás seguro? Eliminar esta categoría eliminará permanentemente todos los productos dentro de ella." is displayed, and exactly two buttons with labels "Eliminar" and "Cancelar" are present
    - Test 2: Simulate a failed deletion (error set in `ConfigurationUiState.error`); assert the error message is displayed on screen and the screen remains interactive (Requirement 2.12)
    - _Requirements: 2.4, 2.5, 2.11, 2.12_

- [x] 6. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Task 1.1 is the single most impactful change: one SQL keyword addition fixes the entire reordering bug
- The `combine` extension in Task 2.2 uses the existing `args[N]` array pattern already present in `ConfigurationViewModel`; no architectural change is needed
- `confirmDeleteCategory()` intentionally closes the dialog **before** launching the coroutine (design spec note) so the UI always transitions out of the dialog state even if the coroutine is slow
- The auto-select of the first remaining category after deletion is handled by the existing `categoriesFlow.onEach` block — no additional code needed
- Property tests 1 & 2 are instrumented (Android) tests because they require a real Room in-memory database
- Property tests 3–8 for the ViewModel can be local JUnit tests using MockK and Turbine

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["2.2"] },
    { "id": 2, "tasks": ["1.2", "2.3"] },
    { "id": 3, "tasks": ["2.4", "4.1"] },
    { "id": 4, "tasks": ["4.2", "5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3"] }
  ]
}
```
