# Implementation Plan: New Product Modal

## Overview

Implement the `NewProductModal` feature as a `ModalBottomSheet` in Jetpack Compose. The work
is split into three main layers: the ViewModel + state classes, the composable UI components,
and the wiring into `ConfigurationScreen`. All data layer files already exist; this feature
only adds UI and ViewModel code in a new `ui/newproduct/` package.

---

## Tasks

- [x] 1. Create the `ui/newproduct` package with state classes and ViewModel skeleton
  - Create `app/src/main/java/com/example/puntodeventa/ui/newproduct/` directory
  - Define `SaveResult` sealed interface (`Success`, `Failure`) in `NewProductViewModel.kt`
  - Define `OptionDraft` data class with `draftId`, `optionName`, `extraPriceText`, `optionNameError`, `optionPriceError`, and computed `extraPrice`
  - Define `GroupDraft` data class with `draftId`, `groupName`, `selectionType`, `options`, `groupNameError`
  - Define `NewProductUiState` data class with all fields from the design (all `val`, no `MutableList`)
  - Define the `NewProductViewModel` class skeleton with constructor parameters and `Factory`; leave function bodies empty for now
  - _Requirements: 10.1, 10.3, 10.6_

- [x] 2. Implement ViewModel — menus, categories, and emoji
  - [x] 2.1 Implement `init` block: collect `MenuRepository.menuItems`, auto-select first menu, call `loadCategories`
    - Implement `loadCategories(menuId)` private function
    - Implement `selectMenu(menu)` (clears categories and selectedCategory, reloads categories)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 2.2 Implement `updateEmoji` (ignores blank strings) and `toggleEmojiPicker`
    - _Requirements: 2.4, 2.5, 2.6_

- [x] 3. Implement ViewModel — basic product fields and dismiss
  - [x] 3.1 Implement `updateName`, `updateDescription`, `updatePriceText` with character limits and `sanitizePriceInput` helper
    - `updateName`: max 120, clears `nameError` on non-empty input
    - `updateDescription`: max 500, silently drops excess
    - `updatePriceText`: delegates to `sanitizePriceInput`; keeps only digits + one `.` + ≤2 decimal places
    - _Requirements: 3.1, 3.2, 3.3, 3.6_
  - [x] 3.2 Implement `dismiss()`: resets all draft fields to Initial Empty State, preserves `menus`/`categories`
    - _Requirements: 1.3, 9.8_

- [x] 4. Implement ViewModel — category inline creation
  - [x] 4.1 Implement `selectCategory`, `startInlineCategoryCreation`, `updateInlineCategoryName`, `cancelNewCategory`
    - `selectCategory`: sets `selectedCategory`, clears `categoryError`
    - `updateInlineCategoryName`: max 80, clears `newCategoryNameError` on non-empty input
    - `cancelNewCategory`: hides inline UI, clears draft name and error
    - _Requirements: 5.1, 5.7, 5.8_
  - [x] 4.2 Implement `submitNewCategory()` with validation, `CategoryRepository.insert`, success/failure state updates
    - Blank name → set `newCategoryNameError`, do not call repository
    - On success: auto-select new category, hide inline UI, clear draft fields
    - On failure: set `error` message, keep inline UI visible
    - _Requirements: 5.3, 5.4, 5.5, 5.6, 5.9_

- [x] 5. Implement ViewModel — group and option management
  - [x] 5.1 Implement `addGroup`, `removeGroup`, `updateGroupName`, `updateGroupSelectionType`
    - `addGroup`: appends `GroupDraft()` with default `selectionType = MULTIPLE_CHECKBOXES` and one default `OptionDraft`
    - `removeGroup(index)`: no-op if out of bounds
    - `updateGroupName`: max 120, clears `groupNameError` on non-whitespace input, preserves sibling `GroupDraft` referential identity
    - `updateGroupSelectionType`: updates only `selectionType` of the target group
    - _Requirements: 6.2, 6.3, 6.4, 6.7, 7.3, 10.1, 10.4, 10.7, 10.8_
  - [ ]* 5.2 Write property test — Property 1: addGroup size invariant
    - **Property 1: addGroup size invariant**
    - For N ≥ 0 calls to `addGroup()` from empty state, `groups.size == N`
    - **Validates: Requirements 11.1**
  - [ ]* 5.3 Write property test — Property 2: addGroup / removeGroup inverse
    - **Property 2: addGroup / removeGroup inverse**
    - Add N groups then remove all in descending index order → `groups` is empty
    - **Validates: Requirements 11.2**
  - [ ]* 5.4 Write property test — Property 3: GroupDraft draftId global uniqueness
    - **Property 3: GroupDraft draftId global uniqueness**
    - After any sequence of `addGroup()` calls, all `draftId` values are pairwise distinct
    - **Validates: Requirements 11.3, 10.1**
  - [x] 5.5 Implement `addOption`, `removeOption`, `updateOptionName`, `updateOptionExtraPrice`
    - `addOption(groupIndex)`: appends `OptionDraft()` only to the target group
    - `removeOption(groupIndex, optionIndex)`: no-op if either index is out of bounds
    - `updateOptionName`: max 120, clears `optionNameError` on non-whitespace input, preserves sibling `OptionDraft` referential identity
    - `updateOptionExtraPrice`: delegates to `sanitizePriceInput`, clears `optionPriceError` when parsed value ≥ 0
    - _Requirements: 8.2, 8.3, 8.4, 8.7, 8.8, 10.3, 10.5, 10.7, 10.8_
  - [ ]* 5.6 Write property test — Property 4: addOption size invariant
    - **Property 4: addOption size invariant**
    - For M ≥ 0 calls to `addOption(groupIndex)` on a newly created group, `group.options.size == M + 1`
    - **Validates: Requirements 11.4**
  - [ ]* 5.7 Write property test — Property 5: updateGroupName preserves all draftIds
    - **Property 5: updateGroupName preserves all draftIds**
    - Calling `updateGroupName(index, newName)` leaves every `GroupDraft.draftId` unchanged
    - **Validates: Requirements 11.5, 10.4**
  - [ ]* 5.8 Write property test — Property 6: updateOptionName preserves all option draftIds
    - **Property 6: updateOptionName preserves all option draftIds within group**
    - Calling `updateOptionName(g, o, name)` leaves every `OptionDraft.draftId` in the group unchanged
    - **Validates: Requirements 11.6, 10.5**
  - [ ]* 5.9 Write property test — Property 7: OptionDraft draftId uniqueness within group
    - **Property 7: OptionDraft draftId uniqueness within a group**
    - After any sequence of `addOption(groupIndex)` calls, all `draftId` values within that group are pairwise distinct
    - **Validates: Requirements 11.7, 10.3**

- [x] 6. Implement ViewModel — save transaction and validation
  - [x] 6.1 Implement `save()`: full validation logic (name, category, group names, option names, option prices) with simultaneous multi-field error setting
    - When validation fails: update all error fields simultaneously, do NOT start the transaction
    - _Requirements: 3.5, 5.10, 6.6, 8.5, 8.6, 9.2_
  - [x] 6.2 Implement the `AppDatabase.withTransaction` block: insert `ProductEntity`, then for each `GroupDraft` insert `CustomizationGroupEntity` + its `CustomizationOptionEntity`s
    - On success: `isSaving = false`, `saveResult = SaveResult.Success`
    - On failure: `isSaving = false`, set `error` message; Room auto-rolls back
    - _Requirements: 9.2, 9.3, 9.4, 9.6_

- [ ] 7. Checkpoint — Ensure all ViewModel tests pass
  - Ensure all unit and property tests for `NewProductViewModel` pass, ask the user if questions arise.

- [x] 8. Implement `SelectionTypeDropdown` composable
  - Create `app/src/main/java/com/example/puntodeventa/ui/newproduct/SelectionTypeDropdown.kt`
  - `ExposedDropdownMenuBox` with two entries: "Casillas (múltiple)" → `MULTIPLE_CHECKBOXES`, "Opción única" → `SINGLE_OPTION`
  - Apply `groupFieldColors()` helper for consistent styling
  - _Requirements: 7.1, 7.2, 7.4_

- [x] 9. Implement `EmojiPickerButton` composable
  - Create `app/src/main/java/com/example/puntodeventa/ui/newproduct/EmojiPicker.kt`
  - `OutlinedButton` showing the current emoji + chevron icon (up/down based on `expanded`)
  - `AnimatedVisibility` + `LazyVerticalGrid(GridCells.Fixed(5))` with ≥ 50 emoji entries from `EMOJI_LIST`
  - `onEmojiSelected` callback wires back to `NewProductViewModel.updateEmoji`
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 10. Implement `GroupCard` and `OptionRow` composables
  - Create `app/src/main/java/com/example/puntodeventa/ui/newproduct/GroupCard.kt`
  - `GroupCard`: `Card` with group name `OutlinedTextField` (max 120, shows `groupNameError`), `SelectionTypeDropdown`, trash `IconButton`, list of `OptionRow`s using `key(option.draftId)`, and "+ Agregar opción" `TextButton`
  - `OptionRow` (private): option name `OutlinedTextField` (max 120, shows `optionNameError`), extra price `OutlinedTextField` (Decimal, shows `optionPriceError`, width = 120dp), "X" `IconButton`
  - Apply `groupFieldColors()` and `CardBackground`/`CardText` tokens
  - _Requirements: 6.5, 7.1, 8.1, 10.2_

- [x] 11. Implement `NewProductModal` composable
  - Create `app/src/main/java/com/example/puntodeventa/ui/newproduct/NewProductModal.kt`
  - `ModalBottomSheet` with `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)`, `dragHandle = null`
  - Header row: "Nuevo Producto" title + "X" `IconButton` (disabled when `isSaving`)
  - `LaunchedEffect(uiState.saveResult)` → call `onDismiss()` when `SaveResult.Success`
  - `confirmValueChange` lambda suppresses swipe-dismiss while `isSaving = true`
  - Scrollable `Column` containing: `EmojiPickerButton`, name/description/price `OutlinedTextField`s, `MenuDropdown`, `CategoryDropdown` (with inline creation block), "Personalizaciones" header, group list using `key(group.draftId)` + `GroupCard`, "+ Grupo" `OutlinedButton`, global error text, bottom action row ("Cancelar" + "Crear producto" with `CircularProgressIndicator` when saving)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 3.1, 3.2, 3.3, 4.6, 4.7, 5.2, 5.3, 5.9, 9.1, 9.5, 9.6, 9.7, 9.8_

- [x] 12. Wire `NewProductModal` into `ConfigurationScreen` and `MainActivity`
  - [x] 12.1 Add `newProductViewModel: NewProductViewModel` parameter to `ConfigurationScreen` composable; add `showModal` local state; add "+ Nuevo Producto" lambda setting `showModal = true`; render `NewProductModal` when `showModal = true`
    - _Requirements: 1.1, 1.3_
  - [x] 12.2 Construct `NewProductViewModel.Factory` in `MainActivity` alongside `ConfigurationViewModel.Factory`, passing `productRepository`, `categoryRepository`, `menuRepository`, and `database`; pass `newProductViewModel` to `ConfigurationScreen`
    - _Requirements: 9.2_

- [ ] 13. Final checkpoint — Ensure all tests pass
  - Ensure all unit tests, property tests, and instrumented DAO tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Property tests use [Kotest Property Testing](https://kotest.io/docs/proptest/property-based-testing.html) with `TestScope` and fake repositories; no Android framework needed
- All ViewModel state mutations use `_uiState.update { ... }` + immutable `data class copy` — never `MutableList`
- `GroupDraft.draftId` and `OptionDraft.draftId` are stable Compose keys; never reassigned
- `sanitizePriceInput` is shared by both the base price field and all option extra-price fields
- The `showModal` boolean is hoisted locally in `ConfigurationScreen`, not in `ConfigurationViewModel`
- No data layer (DAO, Entity, Repository) files are modified by this feature

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "3.1", "3.2"] },
    { "id": 2, "tasks": ["4.1", "4.2", "5.1", "5.5"] },
    { "id": 3, "tasks": ["5.2", "5.3", "5.4", "5.6", "5.7", "5.8", "5.9", "6.1"] },
    { "id": 4, "tasks": ["6.2"] },
    { "id": 5, "tasks": ["8", "9", "10"] },
    { "id": 6, "tasks": ["11"] },
    { "id": 7, "tasks": ["12.1", "12.2"] }
  ]
}
```
