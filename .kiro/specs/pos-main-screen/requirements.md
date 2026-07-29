# Requirements Document

## Introduction

This feature implements the main POS (Point of Sale) screen for the PuntoDeVenta application. It provides a two-panel layout where staff can browse the product catalog, add items to an in-memory cart with customizations, and complete sales that are persisted to a local Room database. The left panel shows category-filtered product grids, while the right panel displays the current ticket/cart with a pay button. A product detail modal allows quantity selection, customization options, and extra comments before adding items to the cart.

## Glossary

- **POS_Screen**: The main point-of-sale composable screen containing the catalog panel and cart panel
- **Catalog_Panel**: The left 70%-width panel displaying category tabs and the product grid
- **Cart_Panel**: The right 30%-width panel displaying the current in-memory order items and total button
- **Product_Grid**: A LazyVerticalGrid showing active product cards filtered by the selected category
- **Category_Tab_Bar**: A horizontally scrollable row of category tabs at the top of the Catalog_Panel
- **Product_Modal**: A dialog displayed when a product card is tapped, allowing customization and quantity selection before adding to cart
- **Cart_Item**: An in-memory representation of a product added to the current order, including quantity, selected customizations, and extra notes
- **OrderEntity**: A Room database entity representing a completed and paid order
- **OrderItemEntity**: A Room database entity representing a single line item within a persisted order
- **OrderItemCustomizationEntity**: A Room database entity representing a selected customization option for a persisted order item
- **PosViewModel**: The ViewModel responsible for managing the in-memory cart state, category/product loading, and order persistence
- **TODO_Tab**: A special category tab that is always the first tab and shows all active products regardless of category

## Requirements

### Requirement 1: Order Persistence Schema

**User Story:** As a business owner, I want completed orders to be saved in a local database, so that I can review sales history and generate statistics.

#### Acceptance Criteria

1. THE OrderEntity SHALL store the fields: id (String, primary key), timestamp (Long, epoch millis), totalAmount (Double, range 0.00 to 999,999,999.99), and status (String, one of: "COMPLETED", "CANCELLED", "REFUNDED")
2. THE OrderItemEntity SHALL store the fields: id (String, primary key), orderId (String, foreign key to OrderEntity, indexed), productId (String), productName (String, max 120 characters), quantity (Int, minimum 1), basePrice (Double, range 0.00 to 999,999.99), totalPrice (Double, range 0.00 to 999,999,999.99), and extraNotes (String, nullable, max 500 characters)
3. THE OrderItemCustomizationEntity SHALL store the fields: id (String, primary key), orderItemId (String, foreign key to OrderItemEntity, indexed), optionName (String, max 120 characters), and extraPrice (Double, minimum 0.00)
4. WHEN an OrderEntity is deleted, THE database SHALL cascade-delete all associated OrderItemEntity records
5. WHEN an OrderItemEntity is deleted, THE database SHALL cascade-delete all associated OrderItemCustomizationEntity records
6. THE AppDatabase SHALL set its version number to 3 and include OrderEntity, OrderItemEntity, and OrderItemCustomizationEntity in the entities list alongside the existing entities

### Requirement 2: POS Screen Two-Panel Layout

**User Story:** As a cashier, I want a split-screen layout with the catalog on the left and the cart on the right, so that I can browse products and see my current order simultaneously.

#### Acceptance Criteria

1. THE POS_Screen SHALL display the Catalog_Panel and the Cart_Panel simultaneously, with the Catalog_Panel occupying 70% of the available content width (the horizontal space remaining after the navigation rail) and the Cart_Panel occupying the remaining 30%
2. THE Catalog_Panel SHALL be positioned to the left of the Cart_Panel, and both panels SHALL fill the full available content height
3. THE POS_Screen SHALL preserve the existing left navigation rail as the first child in the layout row, keeping it visible and functional without modification to its width or behavior
4. WHEN the POS_Screen is displayed, THE POS_Screen SHALL render both panels within a horizontal row that starts immediately to the right of the navigation rail, with no overlapping content between the two panels

### Requirement 3: Category Tab Bar

**User Story:** As a cashier, I want to filter products by category using tabs, so that I can quickly find the product I need.

#### Acceptance Criteria

1. THE Category_Tab_Bar SHALL display a "TODO" tab as the first tab that shows all active products
2. THE Category_Tab_Bar SHALL display one tab for each category returned by CategoryRepository, ordered alphabetically by category name after the "TODO" tab
3. THE Category_Tab_Bar SHALL be horizontally scrollable when tabs exceed the available width
4. WHEN a category tab is selected, THE Product_Grid SHALL display only active products belonging to that category
5. WHEN the "TODO" tab is selected, THE Product_Grid SHALL display all active products regardless of category
6. THE Category_Tab_Bar SHALL display a search icon (magnifying glass) and a split-bill icon (scissors) as fixed elements to the right of the scrollable tab area, remaining visible regardless of scroll position
7. WHEN the Category_Tab_Bar is first displayed, THE Category_Tab_Bar SHALL select the "TODO" tab by default
8. THE Category_Tab_Bar SHALL visually distinguish the selected tab from unselected tabs by applying a distinct background color and bold text weight to the selected tab
9. IF CategoryRepository returns an empty list, THEN THE Category_Tab_Bar SHALL display only the "TODO" tab as the sole selectable tab

### Requirement 4: Product Grid Display

**User Story:** As a cashier, I want to see product cards in a grid, so that I can quickly identify and select products to add to the order.

#### Acceptance Criteria

1. THE Product_Grid SHALL display active products (isActive = true) in a LazyVerticalGrid layout using GridCells.Adaptive with a minimum cell size of 200 dp
2. WHEN the "TODO" tab is selected, THE Product_Grid SHALL show all active products from all categories, sorted by name ascending (case-insensitive)
3. WHEN a specific category tab is selected, THE Product_Grid SHALL show only active products whose categoryId matches the selected category, sorted by name ascending (case-insensitive)
4. THE Product_Grid SHALL display each product card with a white background containing the product emoji (max 8 characters), the product name (max 120 characters, truncated with ellipsis if it exceeds 2 lines), and the product base price formatted as currency
5. WHEN a product card is tapped, THE POS_Screen SHALL open the Product_Modal for that product, passing the selected product's identifier
6. IF the Product_Grid has no active products to display for the current filter (selected tab), THEN THE Product_Grid SHALL display an empty-state message indicating no products are available

### Requirement 5: Cart Panel Display

**User Story:** As a cashier, I want to see the items I have added to the current order in a list, so that I can review the ticket before completing the sale.

#### Acceptance Criteria

1. THE Cart_Panel SHALL display a scrollable list (LazyColumn) of Cart_Items with a white background on the list container
2. THE Cart_Panel SHALL display for each Cart_Item: the quantity, the product name, the names of selected customization options, and the total row price formatted as "$X.XX" with exactly 2 decimal places, where total row price equals (basePrice + sum of selected customization extraPrices) multiplied by quantity
3. THE Cart_Panel SHALL display a green bottom button showing "TOTAL: $X.XX" where X.XX is the sum of all Cart_Item total row prices formatted with exactly 2 decimal places
4. WHILE the cart contains zero items, THE Cart_Panel SHALL display the total button text as "TOTAL: $0.00"
5. WHEN a Cart_Item is added or removed from the cart, THE Cart_Panel SHALL recalculate and update the displayed total within the same frame of recomposition
6. WHEN a Cart_Item in the list is swiped or its delete action is triggered, THE PosViewModel SHALL remove that Cart_Item from the in-memory cart and THE Cart_Panel SHALL update the list and total accordingly
7. THE Cart_Panel SHALL display Cart_Items in the order they were added to the cart, with the most recently added item appearing at the bottom of the list

### Requirement 6: Order Completion and Persistence

**User Story:** As a cashier, I want to press the total button to complete the sale and save the order, so that the transaction is recorded.

#### Acceptance Criteria

1. WHEN the total button is pressed and the cart contains at least one item, THE PosViewModel SHALL persist the current cart as an OrderEntity with status "PAID", the current timestamp, and a totalAmount equal to the sum of all Cart_Item total prices
2. WHEN the total button is pressed and the cart contains at least one item, THE PosViewModel SHALL persist each Cart_Item as an OrderItemEntity linked to the created OrderEntity
3. WHEN the total button is pressed and the cart contains at least one item, THE PosViewModel SHALL persist each selected customization as an OrderItemCustomizationEntity linked to its OrderItemEntity
4. WHEN the total button is pressed and the cart contains at least one item, THE PosViewModel SHALL persist the OrderEntity, all OrderItemEntities, and all OrderItemCustomizationEntities within a single database transaction so that either all records are written or none are
5. WHEN the order is successfully persisted, THE PosViewModel SHALL clear the in-memory cart and reset the displayed total to $0.00
6. IF the database transaction fails during order persistence, THEN THE PosViewModel SHALL retain all Cart_Items in the in-memory cart unchanged and display an error message indicating the order could not be saved
7. IF the total button is pressed and the cart is empty, THEN THE POS_Screen SHALL not persist any order and shall not modify the cart state

### Requirement 7: Product Modal Display

**User Story:** As a cashier, I want to see product details and customization options when I tap a product, so that I can configure the item before adding it to the order.

#### Acceptance Criteria

1. WHEN a product card is tapped, THE Product_Modal SHALL display the product emoji, name, and base price (formatted as currency) on the left side
2. WHEN the Product_Modal is displayed, THE Product_Modal SHALL display on the right side the list of CustomizationGroupEntity records associated with that product, showing each group's groupName as a header followed by its CustomizationOptionEntity options displaying optionName and extraPrice (if extraPrice > 0)
3. WHEN a customization group has selectionType "multiple_checkboxes", THE Product_Modal SHALL render its options as checkboxes allowing zero or more selections
4. WHEN a customization group has selectionType "single_option", THE Product_Modal SHALL render its options as radio buttons with no option pre-selected
5. WHEN the Product_Modal is displayed, THE Product_Modal SHALL display a green text field labeled "Comentario extra" that accepts free-text input up to 200 characters
6. WHEN the Product_Modal is displayed, THE Product_Modal SHALL display a quantity selector with a decrement button, the current quantity value, and an increment button, supporting a quantity range from 1 to 99
7. THE Product_Modal SHALL set the initial quantity to 1
8. IF the decrement button is tapped while the quantity is 1, THEN THE Product_Modal SHALL keep the quantity at 1 and disable the decrement button
9. IF the product has no associated CustomizationGroupEntity records, THEN THE Product_Modal SHALL display only the product details, comment field, and quantity selector without a customization section

### Requirement 8: Product Modal Quantity Control

**User Story:** As a cashier, I want to adjust the quantity of a product before adding it to the order, so that I can handle orders for multiple units.

#### Acceptance Criteria

1. WHEN the Product_Modal is displayed, THE Product_Modal SHALL set the quantity to 1
2. WHEN the increment button is pressed and quantity is less than 99, THE Product_Modal SHALL increase the quantity by 1
3. IF the decrement button is pressed and quantity is greater than 1, THEN THE Product_Modal SHALL decrease the quantity by 1
4. IF the decrement button is pressed and quantity equals 1, THEN THE Product_Modal SHALL keep the quantity at 1
5. IF the increment button is pressed and quantity equals 99, THEN THE Product_Modal SHALL keep the quantity at 99
6. THE Product_Modal SHALL display the current quantity as an integer between the decrement and increment buttons

### Requirement 9: Add Item to Cart from Modal

**User Story:** As a cashier, I want to press "Agregar" to add the configured product to the cart, so that the item appears in the current ticket.

#### Acceptance Criteria

1. WHEN the "Agregar" button is pressed, THE PosViewModel SHALL create a new Cart_Item in the in-memory cart containing the selected product reference, the entered quantity (integer, 1 to 99), the list of selected customization options (which may be empty), and the extra notes text (0 to 280 characters), regardless of whether an identical product already exists in the cart (always a separate line item)
2. WHEN the "Agregar" button is pressed, THE Cart_Item total price SHALL be calculated as (basePrice + sum of selected customization option extraPrices) multiplied by quantity, rounded to two decimal places
3. WHEN the "Agregar" button is pressed, THE Product_Modal SHALL close and the newly created Cart_Item SHALL appear as the last entry in the current ticket's item list
4. WHEN the "Cancelar" button is pressed, THE Product_Modal SHALL close without modifying the cart
5. IF the quantity field contains a value less than 1 or greater than 99 when "Agregar" is pressed, THEN THE Product_Modal SHALL keep the modal open and display an inline error message indicating the valid quantity range (1–99)
6. IF the extra notes text exceeds 280 characters, THEN THE Product_Modal SHALL prevent further character input

### Requirement 10: PosViewModel State Management

**User Story:** As a developer, I want a single ViewModel managing the POS screen state, so that the UI remains consistent and testable.

#### Acceptance Criteria

1. THE PosViewModel SHALL hold the in-memory cart as a list of CartItem objects (each containing at minimum: productId, productName, unitPrice, and quantity) exposed via StateFlow, initialized as an empty list
2. THE PosViewModel SHALL load categories from CategoryRepository for the current menu and expose them via StateFlow
3. WHEN the PosViewModel is initialized and categories are loaded, THE PosViewModel SHALL auto-select the first category and load its active products into the products StateFlow
4. WHEN a category is selected, THE PosViewModel SHALL replace the current product list with active products belonging to the selected category
5. THE PosViewModel SHALL expose the cart total as a StateFlow derived by summing (unitPrice × quantity) for each CartItem in the cart list
6. IF loading categories or products from the repository fails, THEN THE PosViewModel SHALL expose an error state indicating which operation failed while preserving the current cart contents
7. WHEN the selected category contains zero products, THE PosViewModel SHALL emit an empty list through the products StateFlow
