# Implementation Plan: POS Main Screen

## Overview

This plan implements the main Point of Sale screen with a two-panel layout (catalog + cart), product modal with customizations, and order persistence to Room. Tasks are ordered so that data layer foundations come first, followed by the ViewModel, then UI composables, and finally integration wiring.

## Tasks

- [x] 1. Set up data layer: Order entities and DAO
  - [x] 1.1 Create OrderEntity, OrderItemEntity, and OrderItemCustomizationEntity Room entities
    - Create `data/local/OrderEntity.kt` with fields: id (String PK), timestamp (Long), totalAmount (Double), status (String)
    - Create `data/local/OrderItemEntity.kt` with fields: id (String PK), orderId (FK to orders, indexed), productId, productName, quantity, basePrice, totalPrice, extraNotes (nullable)
    - Create `data/local/OrderItemCustomizationEntity.kt` with fields: id (String PK), orderItemId (FK to order_items, indexed), optionName, extraPrice
    - Define CASCADE foreign keys: OrderEntity → OrderItemEntity → OrderItemCustomizationEntity
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Create OrderDao interface
    - Create `data/local/OrderDao.kt` with @Insert methods: insertOrder, insertOrderItems, insertOrderItemCustomizations
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.3 Update AppDatabase to version 3
    - Add OrderEntity, OrderItemEntity, OrderItemCustomizationEntity to the @Database entities list
    - Bump version to 3
    - Add abstract fun for OrderDao accessor
    - Add destructive migration (fallbackToDestructiveMigration) or appropriate migration strategy
    - _Requirements: 1.6_

  - [x] 1.4 Create OrderRepository
    - Create `data/repository/OrderRepository.kt`
    - Implement `persistOrder(order, items, customizations)` using `database.withTransaction` to wrap all inserts atomically
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 2. Checkpoint - Ensure data layer compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Implement PosViewModel and in-memory cart logic
  - [x] 3.1 Create CartItem and SelectedCustomization data classes
    - Create `ui/pos/CartItem.kt` with fields: id (UUID), productId, productName, emoji, basePrice, quantity, selectedCustomizations, extraNotes, totalPrice
    - Include SelectedCustomization data class with optionId, optionName, extraPrice
    - _Requirements: 5.1, 5.2, 9.1_

  - [x] 3.2 Create PosUiState data class and PosViewModel
    - Create `ui/pos/PosViewModel.kt` with PosUiState data class (categories, selectedCategory, products, cartItems, cartTotal, isLoading, error, isModalOpen, selectedProduct)
    - Implement constructor receiving CategoryRepository, ProductRepository, OrderRepository, and menuId
    - Load categories via categoryRepository, auto-select "TODO" (null) on init
    - Use flatMapLatest on selectedCategory to filter and sort active products (case-insensitive name sort)
    - Combine all flows into a single PosUiState StateFlow
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

  - [x] 3.3 Implement cart mutation methods in PosViewModel
    - Implement `selectCategory(category)` to update selectedCategory flow
    - Implement `addToCart(cartItem)` appending to the MutableStateFlow cart list (always new line item)
    - Implement `removeFromCart(cartItemId)` filtering out the item by id
    - Implement `completeOrder()` that maps cart to entities, calls orderRepository.persistOrder in a transaction, clears cart on success, sets error on failure
    - Ensure completeOrder is a no-op when cart is empty
    - _Requirements: 5.5, 5.6, 5.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 9.1_

  - [x] 3.4 Extract pure helper functions for price calculation and sorting
    - Extract `calculateItemTotal(basePrice, extraPrices, quantity): Double` using BigDecimal HALF_UP rounding
    - Extract `calculateCartTotal(items): Double`
    - Extract `sortAndFilterProducts(products, selectedCategoryId): List<Product>`
    - Extract `buildTabOrder(categories): List` — "TODO" first, then alphabetical (case-insensitive)
    - Extract `clampQuantity(current, delta): Int` clamping to [1, 99]
    - Mark all as `internal` for testability
    - _Requirements: 5.2, 5.3, 3.1, 3.2, 3.4, 3.5, 8.2, 8.3, 8.4, 8.5_

  - [x] 3.5 Write property test: Cart Item Price Calculation (Property 3)
    - **Property 3: Cart Item Price Calculation**
    - Test that for any basePrice ≥ 0, extraPrices list ≥ 0, and quantity in [1,99], calculateItemTotal returns round((basePrice + sum(extraPrices)) × quantity, 2)
    - Use kotest-property with Arb generators
    - **Validates: Requirements 5.2, 9.2**

  - [x] 3.6 Write property test: Cart Total is Sum of Row Prices (Property 4)
    - **Property 4: Cart Total is Sum of Row Prices**
    - Test that calculateCartTotal returns the sum of all CartItem totalPrice values
    - **Validates: Requirements 5.3, 10.5**

  - [x] 3.7 Write property test: Category Tab Ordering (Property 1)
    - **Property 1: Category Tab Ordering**
    - Test that buildTabOrder always produces "TODO" first, followed by categories sorted alphabetically (case-insensitive)
    - **Validates: Requirements 3.2, 3.1**

  - [x] 3.8 Write property test: Product Filtering and Sorting (Property 2)
    - **Property 2: Product Filtering and Sorting by Category**
    - Test that sortAndFilterProducts returns only active products matching the category (or all if null), sorted by name ascending (case-insensitive)
    - **Validates: Requirements 3.4, 3.5, 4.2, 4.3, 10.4**

  - [x] 3.9 Write property test: Quantity Clamped Within [1, 99] (Property 10)
    - **Property 10: Quantity Clamped Within [1, 99]**
    - Test that clampQuantity(current, +1) = min(current+1, 99) and clampQuantity(current, -1) = max(current-1, 1)
    - **Validates: Requirements 8.2, 8.3, 8.4, 8.5**

  - [x] 3.10 Write property test: Cart Item Removal Preserves Other Items (Property 5)
    - **Property 5: Cart Item Removal Preserves Other Items**
    - Test that removing an item by id results in N-1 items where all others remain unchanged in value and order
    - **Validates: Requirements 5.6**

  - [x] 3.11 Write property test: Cart Maintains Insertion Order (Property 6)
    - **Property 6: Cart Maintains Insertion Order**
    - Test that adding items preserves insertion order with the newest item at the end
    - **Validates: Requirements 5.7**

  - [x] 3.12 Write property test: Adding a Product Creates a New Line Item (Property 11)
    - **Property 11: Adding a Product Creates a New Line Item**
    - Test that addToCart always appends a new CartItem with a unique id regardless of matching product configuration
    - **Validates: Requirements 9.1**

- [x] 4. Checkpoint - Ensure ViewModel logic compiles and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement UI composables: Catalog Panel
  - [x] 5.1 Create CategoryTabBar composable
    - Create `ui/pos/CategoryTabBar.kt`
    - Render horizontally scrollable row of tabs: "TODO" first, then categories alphabetically
    - Highlight selected tab with distinct background and bold text
    - Include search icon and split-bill icon as fixed trailing elements
    - Accept onCategorySelected callback
    - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7, 3.8, 3.9_

  - [x] 5.2 Create ProductGrid composable
    - Create `ui/pos/ProductGrid.kt`
    - Render LazyVerticalGrid with GridCells.Adaptive(200.dp)
    - Each card: white background, emoji, product name (max 2 lines, ellipsis), base price as currency
    - Show empty-state message when no products match current filter
    - Accept onProductTapped callback
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 5.3 Create CatalogPanel composable
    - Create `ui/pos/CatalogPanel.kt`
    - Compose CategoryTabBar at top + ProductGrid below
    - Wire category selection to ViewModel's selectCategory
    - _Requirements: 2.1, 2.2_

- [x] 6. Implement UI composables: Cart Panel and Product Modal
  - [x] 6.1 Create CartPanel composable
    - Create `ui/pos/CartPanel.kt`
    - Render LazyColumn of cart items (quantity, name, customization names, row total "$X.XX")
    - White background on list container
    - Green bottom button showing "TOTAL: $X.XX"
    - Show "TOTAL: $0.00" when cart is empty
    - Support swipe-to-delete or delete action on items
    - Items displayed in insertion order
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [x] 6.2 Create ProductModal dialog composable
    - Create `ui/pos/ProductModal.kt`
    - Left side: product emoji, name, base price
    - Right side: customization groups (checkboxes for "multiple_checkboxes", radio buttons for "single_option")
    - Green "Comentario extra" text field (max 200 chars)
    - Quantity selector: decrement button, quantity display, increment button (range 1–99)
    - Initial quantity = 1; decrement disabled at 1; increment disabled at 99
    - "Agregar" button: builds CartItem, calls addToCart, closes modal
    - "Cancelar" button: closes modal without cart modification
    - Handle no-customization case (hide customization section)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [x] 7. Wire POS screen together and integrate navigation
  - [x] 7.1 Create PosScreen composable
    - Create `ui/pos/PosScreen.kt`
    - Row layout: navigation rail (existing), CatalogPanel (70% of remaining width), CartPanel (30%)
    - Both panels fill full available content height
    - Wire PosViewModel: pass uiState, category selection, cart actions, modal open/close
    - Connect total button press to completeOrder()
    - Show error via Snackbar on persistence failure
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 6.5, 6.6_

  - [x] 7.2 Add NavDestination.Pos and wire in MainActivity
    - Add `Pos` to `NavDestination.kt` enum/sealed class
    - In `MainActivity.kt`, add composable destination for Pos route pointing to PosScreen
    - Pass required dependencies (menuId, repositories) to PosViewModel
    - _Requirements: 2.3_

- [x] 8. Checkpoint - Ensure full app compiles and screens render
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Write integration and unit tests
  - [x] 9.1 Write unit tests for PosViewModel
    - Test initialization: empty cart, categories loaded, TODO tab selected
    - Test selectCategory updates product list
    - Test addToCart adds item with correct price
    - Test removeFromCart removes item and updates total
    - Test completeOrder success: cart cleared, repository called
    - Test completeOrder failure: cart preserved, error state set
    - Test empty cart completeOrder: no-op
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 6.5, 6.6, 6.7_

  - [x] 9.2 Write property test: Order Persistence Maps Cart to Entities (Property 7)
    - **Property 7: Order Persistence Maps Cart to Entities**
    - Test that completeOrder maps cart items to correct OrderEntity, OrderItemEntity, and OrderItemCustomizationEntity fields
    - **Validates: Requirements 6.1, 6.2, 6.3**

  - [x] 9.3 Write property test: Successful Persistence Clears Cart (Property 8)
    - **Property 8: Successful Persistence Clears Cart**
    - Test that after successful completeOrder, cart is empty and total is 0.00
    - **Validates: Requirements 6.5**

  - [x] 9.4 Write property test: Non-Completion Operations Preserve Cart State (Property 9)
    - **Property 9: Non-Completion Operations Preserve Cart State**
    - Test that failed persistence or modal cancel preserves cart contents unchanged
    - **Validates: Requirements 6.6, 9.4, 10.6**

  - [x] 9.5 Write instrumented tests for OrderDao and cascade deletion
    - Test insert and query round-trip for all order entities
    - Test CASCADE deletion: deleting OrderEntity removes OrderItemEntities and OrderItemCustomizationEntities
    - Test transaction atomicity
    - _Requirements: 1.4, 1.5, 6.4_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties defined in the design
- Unit tests validate specific examples and edge cases
- The design uses Kotlin with Jetpack Compose and Room — all tasks target this stack
- Pure helper functions are extracted for testability without Android framework dependencies

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1"] },
    { "id": 1, "tasks": ["1.2", "3.2"] },
    { "id": 2, "tasks": ["1.3", "3.3", "3.4"] },
    { "id": 3, "tasks": ["1.4", "3.5", "3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "3.12"] },
    { "id": 4, "tasks": ["5.1", "5.2", "6.1", "6.2"] },
    { "id": 5, "tasks": ["5.3", "7.1"] },
    { "id": 6, "tasks": ["7.2"] },
    { "id": 7, "tasks": ["9.1", "9.2", "9.3", "9.4", "9.5"] }
  ]
}
```
