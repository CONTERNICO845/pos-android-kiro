# Requirements Document

## Introduction

This feature enhances the POS system in two areas: (1) printing selected customizations on both client and internal tickets so that kitchen staff and customers can see item modifications, and (2) enabling in-place editing of cart items so that cashiers can correct quantities, customizations, or notes without removing and re-adding items.

## Glossary

- **TicketFormatter**: The pure utility object responsible for formatting client-facing and internal ticket strings from structured data.
- **TicketLineItem**: The data class representing a single line item on a ticket, including quantity, product name, line total, and customization names.
- **CartPanel**: The Composable that displays the current in-memory order items and total button on the right side of the POS screen.
- **CartItemRow**: The Composable rendering a single row within CartPanel, supporting swipe-to-delete and tap-to-edit interactions.
- **CartItem**: The in-memory data class representing a product added to the current order, identified by a UUID.
- **ProductModal**: The dialog Composable for configuring a product (quantity, customizations, notes) before adding or updating a cart item.
- **PosViewModel**: The ViewModel managing POS screen state including cart items, product selection, and modal visibility.
- **Edit_Mode**: The application state in which ProductModal is pre-filled with an existing CartItem's data and the confirm button reads "Actualizar" instead of "Agregar".
- **Customization_Line**: A formatted string printed on a ticket representing a single selected customization option, indented below its parent item line.

## Requirements

### Requirement 1: TicketLineItem Customizations Data

**User Story:** As a developer, I want TicketLineItem to carry customization option names, so that ticket formatting functions can print them below each item.

#### Acceptance Criteria

1. THE TicketLineItem SHALL include a `customizations` field of type `List<String>` representing the display names of selected customization options for that item.
2. WHEN a TicketLineItem is constructed with no customizations, THE TicketLineItem SHALL default the `customizations` field to an empty list.
3. THE TicketLineItem SHALL preserve the order of customization names as provided during construction.

### Requirement 2: Client Ticket Customization Printing

**User Story:** As a customer, I want my ticket to show which customizations I selected for each item, so that I can verify my order is correct.

#### Acceptance Criteria

1. WHEN a TicketLineItem has one or more entries in the `customizations` list, THE TicketFormatter SHALL print each customization as a Customization_Line immediately below the item's main line in the client ticket.
2. THE TicketFormatter SHALL format each Customization_Line as six space characters followed by a dash, a space, and the customization option name (pattern: `"      - {optionName}"`).
3. WHEN a TicketLineItem has an empty `customizations` list, THE TicketFormatter SHALL print no Customization_Lines below that item in the client ticket.
4. THE TicketFormatter SHALL print Customization_Lines in the same order as they appear in the `customizations` list of the TicketLineItem.

### Requirement 3: Internal Ticket Customization Printing

**User Story:** As kitchen staff, I want the internal ticket to show customizations for each item, so that I can prepare orders with the correct modifications.

#### Acceptance Criteria

1. WHEN a TicketLineItem has one or more entries in the `customizations` list, THE TicketFormatter SHALL print each customization as a Customization_Line immediately below the item's main line in the internal ticket.
2. THE TicketFormatter SHALL format each Customization_Line on the internal ticket using the same pattern as the client ticket: six space characters followed by a dash, a space, and the customization option name.
3. WHEN a TicketLineItem has an empty `customizations` list, THE TicketFormatter SHALL print no Customization_Lines below that item in the internal ticket.
4. THE TicketFormatter SHALL print Customization_Lines in the same order as they appear in the `customizations` list of the TicketLineItem.

### Requirement 4: Cart Row Tap-to-Edit Interaction

**User Story:** As a cashier, I want to tap a cart row to edit that item, so that I can fix mistakes without removing and re-adding the item.

#### Acceptance Criteria

1. THE CartPanel SHALL accept an `onItemClick` callback parameter of type `(CartItem) -> Unit`.
2. WHEN a user taps anywhere on a CartItemRow, THE CartItemRow SHALL invoke the `onItemClick` callback with the corresponding CartItem.
3. THE CartItemRow SHALL continue to support swipe-to-delete alongside the tap-to-edit interaction without one gesture interfering with the other.
4. THE CartItemRow SHALL provide an accessible click action label so that screen readers can announce the edit functionality.

### Requirement 5: Edit Mode State in PosViewModel

**User Story:** As a developer, I want the PosViewModel to track which cart item is being edited, so that the UI can differentiate between add and edit flows.

#### Acceptance Criteria

1. THE PosViewModel SHALL maintain an `_editingCartItem` state of type `MutableStateFlow<CartItem?>`, initialized to null.
2. WHEN `startEditingItem(cartItemId: String)` is called with a valid cart item ID, THE PosViewModel SHALL set `_editingCartItem` to the matching CartItem from the current cart and open the ProductModal.
3. IF `startEditingItem` is called with an ID that does not match any item in the current cart, THEN THE PosViewModel SHALL leave `_editingCartItem` as null and not open the ProductModal.
4. WHEN the ProductModal is dismissed or an update is confirmed, THE PosViewModel SHALL reset `_editingCartItem` to null.

### Requirement 6: Update Cart Item In-Place

**User Story:** As a cashier, I want changes to an existing cart item to replace the original entry, so that the cart reflects my corrections without duplicating items.

#### Acceptance Criteria

1. THE PosViewModel SHALL expose an `updateCartItem(cartItem: CartItem)` function that replaces the existing cart item matching the provided CartItem's `id`.
2. WHEN `updateCartItem` is called, THE PosViewModel SHALL replace the cart item at the same position in the list, preserving the original list order.
3. WHEN `updateCartItem` is called, THE PosViewModel SHALL recalculate `cartTotal` to reflect the updated item's `totalPrice`.
4. IF `updateCartItem` is called with a CartItem whose `id` does not match any existing cart item, THEN THE PosViewModel SHALL leave the cart unchanged.

### Requirement 7: ProductModal Edit Mode

**User Story:** As a cashier, I want the product modal to show my previous selections when editing a cart item, so that I only need to change what is different.

#### Acceptance Criteria

1. WHEN ProductModal opens in Edit_Mode, THE ProductModal SHALL pre-fill the quantity field with the CartItem's current quantity value.
2. WHEN ProductModal opens in Edit_Mode, THE ProductModal SHALL pre-select all customization options that match the CartItem's `selectedCustomizations` list by `optionId`.
3. WHEN ProductModal opens in Edit_Mode, THE ProductModal SHALL pre-fill the extra notes field with the CartItem's current `extraNotes` value.
4. WHEN ProductModal is in Edit_Mode, THE ProductModal SHALL display the confirm button text as "Actualizar" instead of "Agregar".
5. WHEN the user presses "Actualizar" in Edit_Mode, THE ProductModal SHALL invoke a callback with the updated CartItem using the same `id` as the original item.

### Requirement 8: Ticket Line Item Construction from Cart

**User Story:** As a developer, I want the checkout flow to build TicketLineItems that include customization names from CartItems, so that printed tickets reflect the full order.

#### Acceptance Criteria

1. WHEN the PosViewModel constructs TicketLineItem objects from the current cart for ticket printing, THE PosViewModel SHALL populate each TicketLineItem's `customizations` field with the `optionName` values from the corresponding CartItem's `selectedCustomizations` list.
2. WHEN a CartItem has no selected customizations, THE PosViewModel SHALL construct a TicketLineItem with an empty `customizations` list.
3. THE PosViewModel SHALL preserve the order of `optionName` values from CartItem's `selectedCustomizations` when populating the TicketLineItem's `customizations` field.
