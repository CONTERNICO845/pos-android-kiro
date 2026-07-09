# Implementation Plan: 04 Configuration Screen UI

## Overview

Build the `ConfigurationScreen` composable and `ConfigurationViewModel` on top of the existing
Phase 1 data layer. The implementation proceeds in four layers: pure utility functions first
(testable in isolation), then the ViewModel pipeline, then the UI composables, and finally the
`MainActivity` wiring that replaces `SettingsScreen` with `ConfigurationScreen`.

---

## Tasks

- [x] 1. Add Kotest property-test dependencies and set up the test source set
  - Add `io.kotest:kotest-property-jvm` and `io.kotest:kotest-runner-junit5-jvm` to
    `testImplementation` in `app/build.gradle.kts`
  - Enable JUnit Platform in the `test` task block so Kotest's JUnit5 runner is discovered
  - Verify the build syncs without errors
  - _Requirements: (testing infrastructure — enables all PBT tasks below)_

- [x] 2. Implement pure utility functions and `ConfigurationUiState`
  - [x] 2.1 Create `ui/configuration/ConfigurationViewModel.kt` with `ConfigurationUiState` data class and the two internal top-level functions `applyFilter` and `clampQuery`
    - `ConfigurationUiState` fields: `categories`, `selectedCategory`, `products`,
      `filteredProducts`, `searchQuery`, `expandedProductMenuId`, `isLoading`, `error` — exact
      signatures as per design
    - `internal fun applyFilter(products: List<Product>, query: String): List<Product>` —
      trims query, takes first 100 chars, returns `products` if blank, otherwise filters by
      case-insensitive substring match on `Product.name`
    - `internal fun clampQuery(query: String): String` — returns `query.take(100)`
    - _Requirements: AC-04.2, AC-04.3, AC-06.3_

  - [ ]* 2.2 Write property tests for `applyFilter` and `clampQuery` (PBT-01 through PBT-03, PBT-07, PBT-08)
    - Create `src/test/.../ConfigurationViewModelFilterTest.kt` as a Kotest `PropSpec`
    - **PBT-01: Filter subset property** — `forAll(arbProductList, arbSearchQuery)` verifies
      `applyFilter` output matches reference implementation
      - **Validates: AC-04.2, Property 5**
    - **PBT-02: Filter idempotency** — applying the same filter twice yields the same result
      - **Validates: Property 5**
    - **PBT-03: Empty query returns full list** — `applyFilter(products, "") == products`
      - **Validates: AC-04.3**
    - **PBT-07: Search query clamped at 100 characters** — `clampQuery(query).length <= 100`
      - **Validates: AC-04.2**
    - **PBT-08: filteredProducts is always a subset of products** — every element in filtered
      result is present in the original list
      - **Validates: `filteredProducts ⊆ products` invariant**

  - [x] 2.3 Create `ui/configuration/ProductCard.kt` with the `formatPrice` private helper
    - File contains only the `formatPrice(price: Double): String` function at this stage
      (full `ProductCard` composable added in task 4)
    - `"$" + "%.2f".format(price)` — half-up rounding via `String.format`
    - _Requirements: AC-06.3_

  - [ ]* 2.4 Write property test for `formatPrice` (PBT-04)
    - Create `src/test/.../ProductCardFormatPriceTest.kt`
    - **PBT-04: Price format always starts with "$" and has exactly two decimal places**
    - `forAll(Arb.double(min = 0.0, max = 1_000_000.0))` verifies `startsWith("$")` and two
      decimal places
      - **Validates: AC-06.3, Property 11**

- [x] 3. Implement `ConfigurationViewModel`
  - [x] 3.1 Implement the reactive pipeline in `ConfigurationViewModel`
    - Constructor parameters: `categoryRepository`, `productRepository`, `menuId`
    - Internal `MutableStateFlow`s: `_selectedCategory`, `_searchQuery`, `_expandedMenuId`,
      `_error`
    - `rawProducts` via `_selectedCategory.flatMapLatest` — cancels previous category Flow on
      switch, emits `emptyList()` when null
    - `filteredProducts` via `combine(rawProducts, _searchQuery)` delegating to `applyFilter`
    - Category auto-selection side-effect via `onEach` launched in `viewModelScope`
    - `uiState: StateFlow<ConfigurationUiState>` built from `combine` of all streams, with
      `stateIn(WhileSubscribed(5_000), ConfigurationUiState(isLoading = true))`
    - `isLoading` transitions to `false` after the first category emission
    - _Requirements: AC-09.1, AC-09.2, AC-09.3, AC-09.4, AC-09.6, AC-02.5, AC-02.8_

  - [x] 3.2 Implement the public write functions in `ConfigurationViewModel`
    - `selectCategory(category)` — updates `_selectedCategory`, clears `_searchQuery` (AC-04.4)
    - `updateSearchQuery(query)` — clamps to 100 chars via `clampQuery`, updates `_searchQuery`
    - `toggleProductActive(product)` — launches coroutine calling
      `productRepository.insert(product.copy(isActive = !product.isActive))`, catches exception
      and sets `_error`
    - `duplicateProduct(product)` — launches coroutine calling `insert` with new UUID, sets
      `_expandedMenuId = null`; on exception sets `_error` and dismisses menu (AC-08.5, AC-08.6)
    - `deleteProduct(productId)` — sets `_expandedMenuId = null` immediately, then calls
      `productRepository.deleteById(productId)`; on exception sets `_error` (AC-08.7, AC-08.8)
    - `setExpandedProductMenu(productId)` — updates `_expandedMenuId` (AC-08.9)
    - `clearError()` — sets `_error = null`
    - _Requirements: AC-04.4, AC-07.4, AC-08.5, AC-08.6, AC-08.7, AC-08.8, AC-08.9_

  - [x] 3.3 Add `ConfigurationViewModel.Factory` inner class
    - Implements `ViewModelProvider.Factory`; constructs with `categoryRepository`,
      `productRepository`, `menuId`
    - _Requirements: AC-09.5_

  - [ ]* 3.4 Write unit tests for `ConfigurationViewModel`
    - Create `src/test/.../ConfigurationViewModelTest.kt` using `runTest` + `TestScope`
    - Implement `FakeCategoryRepository` and `FakeProductRepository` as described in the design
    - Cover all cases from the design's "Key unit test cases" table:
      `autoSelectsFirstCategory`, `autoSelectFallsBackOnDeletion`, `autoSelectNullOnEmpty`,
      `switchCategoryResetsSearch`, `filterByNameCaseInsensitive`, `emptyQueryShowsAll`,
      `searchQueryClampedAt100Chars`, `toggleFlipsIsActive`, `duplicateCreatesNewUUID`,
      `duplicateErrorSetsErrorState`, `deleteRemovesById`, `deleteErrorSetsErrorState`,
      `onlyOneMenuExpandedAtATime`, `loadingTrueUntilFirstEmission`,
      `loadingFalseAfterFirstEmission`
    - _Requirements: AC-02.5, AC-02.8, AC-04.2, AC-04.3, AC-04.4, AC-07.4, AC-08.5, AC-08.6,
      AC-08.7, AC-08.8, AC-08.9, AC-09.6_

  - [ ]* 3.5 Write remaining property tests for ViewModel-level properties (PBT-05, PBT-06)
    - Add to `ConfigurationViewModelFilterTest.kt` or a new `ConfigurationViewModelPbtTest.kt`
    - **PBT-05: Toggle flip is its own inverse** — `forAll(arbProduct)` verifies two consecutive
      toggles restore original `isActive`
      - **Validates: Property 7 (AC-07.4)**
    - **PBT-06: Duplicate preserves all non-id fields** — `forAll(arbProduct)` verifies
      duplicate has same `emoji`, `name`, `description`, `basePrice`, `isActive`, `categoryId`
      and a different `id`
      - **Validates: Property 8 (AC-08.5)**

- [x] 4. Checkpoint — Verify ViewModel builds and all JVM tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement UI composables
  - [x] 5.1 Complete `ProductCard.kt` with `ProductCard` and `ProductActionMenu` composables
    - `ProductCard` signature and full layout as per design: `Card(CardBackground, 8dp shape)`,
      single horizontal `Row` with emoji `Text`, name+price `Column(weight=1f)`, `Switch`,
      settings `IconButton` wrapping `Box` with anchored `ProductActionMenu`
    - `Switch` colors: `checkedThumbColor = CardText`, `checkedTrackColor = ButtonConfirm`,
      `uncheckedThumbColor = CardText`, `uncheckedTrackColor = ButtonCancel`
    - `ProductActionMenu` (private): `DropdownMenu` with exactly three `DropdownMenuItem`s in
      order: "Editar" → "Duplicar" → "Eliminar"; each item dismisses on click; "Eliminar" may
      use `ButtonDelete` color
    - No inline `Color(…)` literals — all colors via named tokens from `Color.kt`
    - _Requirements: AC-06.1 – AC-06.5, AC-07.1 – AC-07.3, AC-08.1 – AC-08.4, AC-10.1, AC-10.2_

  - [x] 5.2 Create `ui/configuration/ConfigurationScreen.kt` with `CategoryTabsRow` and `ActionBarRow`
    - `CategoryTabsRow` (private): `TabRow` when `categories.size ≤ 4`, `ScrollableTabRow`
      otherwise; bg = `CardBackground`; indicator suppressed (same color as container); selected
      tab label in `NavRailIconSelected` bold, unselected in `CardText` normal
    - `ActionBarRow` (private): `Row` with `bg = CardBackground`, 8dp padding; `OutlinedTextField`
      (`weight=1f`, label "Buscar Producto", clips to 100 chars, colors as per design); three
      `OutlinedButton`s (Modificar/Importar/Exportar JSON, `NavRailIconSelected` border+text);
      one filled `Button` ("+ Nuevo Producto", `NavRailIconSelected` bg, `CardText` label)
    - No inline `Color(…)` literals
    - _Requirements: AC-02.1, AC-02.6, AC-03.1, AC-03.3, AC-03.5, AC-04.1, AC-10.1, AC-10.2,
      AC-10.3_

  - [x] 5.3 Complete `ConfigurationScreen` composable with state-driven product list area
    - Top-level `@Composable fun ConfigurationScreen(viewModel: ConfigurationViewModel)`
    - Collect `uiState` via `collectAsStateWithLifecycle()`
    - Render `Column(fillMaxSize)` containing `CategoryTabsRow`, `ActionBarRow`, and product
      list area
    - Product list area state machine exactly as specified in design:
      `isLoading` → `CircularProgressIndicator`; `error != null` → error `Text`;
      `categories.isEmpty()` → "No hay categorías disponibles"; `filteredProducts.isEmpty() &&
      searchQuery.isNotBlank()` → "No se encontraron productos";
      `filteredProducts.isEmpty()` → "No hay productos en esta categoría";
      otherwise `LazyColumn` keyed by `product.id` with `ProductCard` per item
    - Button click handlers log messages per AC-03.2 and AC-03.4; "Editar" lambda logs
      `"Editar: <productId>"` per AC-08.4
    - `onToggleActive` calls `viewModel.toggleProductActive(product)`
    - `onDuplicar` calls `viewModel.duplicateProduct(product)`
    - `onEliminar` calls `viewModel.deleteProduct(id)`
    - `onMenuOpen/Dismiss` call `viewModel.setExpandedProductMenu`
    - `isMenuExpanded` per card = `uiState.expandedProductMenuId == product.id`
    - _Requirements: AC-01.3, AC-01.5, AC-02.1 – AC-02.8, AC-03.2, AC-03.4, AC-04.5, AC-05.1 –
      AC-05.4, AC-08.3, AC-08.4, AC-08.9_

- [x] 6. Wire `ConfigurationScreen` into `MainActivity`
  - [x] 6.1 Update `MainActivity.kt` to instantiate and wire `ConfigurationViewModel`
    - Hoist `HomeViewModel` to `MainActivity` scope (construct once with `viewModel()` factory)
    - Derive `activeMenuId` from `homeUiState.menuItems.firstOrNull()?.id ?: ""`
    - Replace the `NavDestination.Settings -> SettingsScreen()` branch with
      `NavDestination.Settings -> ConfigurationScreen(viewModel = viewModel(factory =
      ConfigurationViewModel.Factory(categoryRepo, productRepo, activeMenuId)))`
    - Build `categoryRepo` and `productRepo` from `AppDatabase` as per design's code snippet
    - Ensure `AppNavRail` remains the first child of the root `Row` (Property 1 / AC-01.2)
    - Delete or leave `SettingsScreen.kt` (housekeeping — not a correctness requirement)
    - _Requirements: AC-01.1, AC-01.2, AC-01.4, AC-09.3, AC-09.4, AC-09.5_

- [x] 7. Final checkpoint — Ensure all tests pass and the screen builds
  - Build the project (`./gradlew assembleDebug`) and confirm no compile errors
  - Run JVM unit tests (`./gradlew test`) and confirm all pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- `applyFilter` and `clampQuery` are `internal` top-level functions in
  `ConfigurationViewModel.kt` so they can be tested without instantiating the ViewModel
- The design document's Testing Strategy section provides the exact fake repository skeletons
  needed for unit tests (task 3.4)
- All color values must be resolved via named tokens from `Color.kt` or
  `MaterialTheme.colorScheme.*` — no inline `Color(0x…)` literals anywhere under
  `ui/configuration/` (AC-10.1)
- Phase 1 files (`CategoryRepository`, `ProductRepository`, DAOs, entities) must NOT be
  modified

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.3"] },
    { "id": 1, "tasks": ["2.2", "2.4", "3.1"] },
    { "id": 2, "tasks": ["3.2"] },
    { "id": 3, "tasks": ["3.3", "3.4"] },
    { "id": 4, "tasks": ["3.5", "5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3"] },
    { "id": 7, "tasks": ["6.1"] }
  ]
}
```
