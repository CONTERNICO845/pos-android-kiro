# Requirements Document

## Introduction

This feature adds the **New Product Modal** to the PuntoDeVenta Android POS app. It is a
`ModalBottomSheet` (Jetpack Compose + Material 3) that allows a cashier or manager to create a
new product with its basic data (emoji, name, description, price), relational assignments
(menu, category), and an unlimited number of customization groups (e.g. "Ingredientes"), each
containing an unlimited number of option rows (e.g. "Sin cebolla", "$2 extra queso").

On confirmation the `NewProductViewModel` validates all fields and persists the complete product
tree — `ProductEntity`, `CustomizationGroupEntity`s, and `CustomizationOptionEntity`s — inside
a single Room transaction so the database is never left in a partial state.

This feature sits entirely on top of the existing Phase 1 data layer
(`ProductRepository`, `CategoryRepository`, `MenuRepository`, `CustomizationGroupDao`,
`CustomizationOptionDao`) and does not modify any existing entity, DAO, or repository.

---

## Glossary

- **Modal**: The `ModalBottomSheet` composable that hosts the entire "Nuevo Producto" form.
- **NewProductViewModel**: The `ViewModel` that owns all transient UI state for the modal and
  executes the save transaction.
- **NewProductUiState**: The single `data class` exposed as a `StateFlow` by
  `NewProductViewModel`; it is the sole source of truth for the modal's UI.
- **GroupDraft**: An in-memory representation of one customization group being edited inside
  the modal. It is never persisted until the user presses "Crear producto".
- **OptionDraft**: An in-memory representation of one option row inside a `GroupDraft`.
- **EmojiPicker**: The composable that surfaces an emoji selection grid when the user taps the
  emoji button.
- **ProductRepository**: Existing repository wrapping `ProductDao`, `CustomizationGroupDao`,
  and `CustomizationOptionDao`; exposes `insert`, `insertGroup`, and `insertOption`.
- **CategoryRepository**: Existing repository wrapping `CategoryDao`; exposes
  `getCategoriesByMenu`, `insert`, and `deleteById`.
- **MenuRepository**: Existing repository wrapping `MenuItemDao`; exposes `menuItems` flow.
- **AppDatabase**: Existing Room singleton; the `withTransaction` extension is called on it to
  wrap the multi-table save in a single atomic transaction.
- **SelectionType**: Existing enum with values `MULTIPLE_CHECKBOXES` and `SINGLE_OPTION`.
- **SaveResult**: A sealed interface returned by the ViewModel's save coroutine: `Success` or
  `Failure(message: String)`.
- **Initial Empty State**: A `NewProductUiState` where `name = ""`, `description = ""`,
  `priceText = ""`, `emoji = "🛒"`, `selectedMenu = null`, `selectedCategory = null`,
  `categories = emptyList()`, `groups = emptyList()`, all error fields are `null`, and
  `saveResult = null`.

---

## Requirements

### Requirement 1: Modal Container and Navigation

**User Story:** As a cashier, I want the "Nuevo Producto" form to appear as a bottom sheet with
a close button, so that I can open and dismiss it without leaving the Configuration screen.

#### Acceptance Criteria

1. WHEN the user presses the "+ Nuevo Producto" button on the `ConfigurationScreen`, THE
   `Modal` SHALL appear as a `ModalBottomSheet` anchored to the bottom of the screen with
   rounded top corners (`topStart = 16.dp`, `topEnd = 16.dp`) and square bottom corners
   (`bottomStart = 0.dp`, `bottomEnd = 0.dp`).
2. THE `Modal` SHALL display a header row containing a title label ("Nuevo Producto") aligned
   to the start and an "X" `IconButton` aligned to the end.
3. WHEN the user taps the "X" `IconButton` or performs a downward swipe gesture on the sheet,
   THE `Modal` SHALL dismiss and THE `NewProductViewModel` SHALL reset `NewProductUiState`
   to the Initial Empty State.
4. WHILE `NewProductUiState.isSaving = true`, THE `Modal` SHALL disable the "X" `IconButton`
   and the downward swipe gesture SHALL be suppressed so that the sheet cannot be dismissed
   during a save operation.
5. WHEN `NewProductUiState.isSaving` transitions to `false`, THE "X" `IconButton` and the
   swipe gesture SHALL become enabled again.
6. THE `Modal` content area SHALL be vertically scrollable so that all fields remain visible
   and interactable on small screens and when the software keyboard is raised.

---

### Requirement 2: Emoji Picker

**User Story:** As a cashier, I want to pick an emoji for the new product, so that products
are visually identifiable in the POS grid.

#### Acceptance Criteria

1. THE `Modal` SHALL display an emoji picker button that shows the currently selected emoji
   (default `"🛒"`) inside a rounded container.
2. WHEN the user taps the emoji picker button and `NewProductUiState.emojiPickerExpanded =
   false`, THE `EmojiPicker` SHALL expand inline below the button, showing a scrollable grid
   of at least 40 common food and object emojis arranged in 5 columns.
3. WHEN the user taps the emoji picker button while the `EmojiPicker` is already expanded,
   THE `EmojiPicker` SHALL collapse without changing the selected emoji.
4. WHEN the user taps an emoji in the grid, THE `NewProductViewModel` SHALL update
   `NewProductUiState.emoji` to the selected emoji.
5. WHEN `NewProductUiState.emoji` is updated to a non-empty value, THE `EmojiPicker` SHALL
   collapse.
6. IF a call to update `NewProductUiState.emoji` would set it to an empty string, THEN THE
   `NewProductViewModel` SHALL retain the previous non-empty emoji value unchanged.

---

### Requirement 3: Basic Product Fields

**User Story:** As a cashier, I want to enter the name, description, and price of a new
product, so that the product is fully described in the catalogue.

#### Acceptance Criteria

1. THE `Modal` SHALL display an `OutlinedTextField` for the product name labelled "Nombre"
   with a maximum input length of 120 characters; any input that would exceed 120 characters
   SHALL be silently discarded.
2. THE `Modal` SHALL display an `OutlinedTextField` for the product description labelled
   "Descripción" with a maximum input length of 500 characters; any input that would exceed
   500 characters SHALL be silently discarded.
3. THE `Modal` SHALL display an `OutlinedTextField` for the base price labelled "Precio" with
   `KeyboardType.Decimal`; any character that is not a digit or a single decimal separator
   SHALL be silently discarded, and input that would result in more than two decimal places
   SHALL be silently discarded.
4. WHEN the user clears the price field or leaves it blank, THE `NewProductViewModel` SHALL
   treat the price as `0.0` and SHALL NOT show a validation error for the price field, either
   during editing or at submit time.
5. WHEN the user attempts to submit and `NewProductUiState.name` is empty, THE
   `NewProductViewModel` SHALL set `NewProductUiState.nameError` to a non-null error message
   and SHALL NOT begin the save transaction; no validation error SHALL be shown for a blank
   description or a zero price.
6. WHEN the user types at least one character in the name field while
   `NewProductUiState.nameError` is non-null, THE `NewProductViewModel` SHALL set
   `NewProductUiState.nameError` to `null` immediately.

---

### Requirement 4: Menu Dropdown

**User Story:** As a cashier, I want to select the menu that the new product's category
belongs to, so that the product is placed in the correct menu hierarchy.

#### Acceptance Criteria

1. THE `Modal` SHALL display an `ExposedDropdownMenuBox` labelled "Menú" whose entries each
   display the `name` field of a `MenuItem`, loaded from `MenuRepository.menuItems`.
2. WHEN `MenuRepository.menuItems` emits a non-empty list for the first time, THE
   `NewProductViewModel` SHALL auto-select the first item, set
   `NewProductUiState.selectedMenu` to that item, and immediately trigger a category load
   for the selected menu's id via `CategoryRepository.getCategoriesByMenu`.
3. WHEN `MenuRepository.menuItems` emits a subsequent non-empty list and
   `NewProductUiState.selectedMenu` is still present in the new list, THE
   `NewProductViewModel` SHALL preserve the current selection.
4. WHEN `MenuRepository.menuItems` emits a subsequent list and `NewProductUiState.selectedMenu`
   is no longer present in the new list, THE `NewProductViewModel` SHALL set
   `NewProductUiState.selectedMenu` to the first item of the new list and reload categories.
5. WHEN the user selects a menu entry from the dropdown, THE `NewProductViewModel` SHALL set
   `NewProductUiState.selectedMenu` to the chosen item, set `NewProductUiState.categories`
   to `emptyList()`, set `NewProductUiState.selectedCategory` to `null`, and then reload
   categories by calling `CategoryRepository.getCategoriesByMenu(newMenuId)`.
6. WHILE `NewProductUiState.menus` is an empty list, THE `Modal` SHALL render the menu
   `ExposedDropdownMenuBox` with `enabled = false` and display the placeholder text
   "Sin menús disponibles".
7. WHEN `NewProductUiState.menus` transitions from empty to non-empty, THE
   `ExposedDropdownMenuBox` SHALL become enabled and display the auto-selected menu name.

---

### Requirement 5: Category Dropdown with Inline Creation

**User Story:** As a cashier, I want to select an existing category or create a new one
inline, so that I can organise products into categories without leaving the modal.

#### Acceptance Criteria

1. THE `Modal` SHALL display an `ExposedDropdownMenuBox` labelled "Categoría" that lists all
   categories in `NewProductUiState.categories` for the currently selected menu.
2. THE category dropdown list SHALL display a special entry at the bottom of the list,
   rendered in the `ButtonConfirm` color (`#4CAF50`), with the label "+ Nueva categoría...";
   this entry SHALL always be visible regardless of how many categories exist.
3. WHEN the user selects the "+ Nueva categoría..." entry, THE `Modal` SHALL dismiss the
   dropdown and show an inline `OutlinedTextField` labelled "Nombre de la nueva categoría"
   with a maximum of 80 characters, a "Guardar categoría" `Button`, and a "Cancelar" link
   or button to dismiss the inline creation UI.
4. WHEN the user taps "Guardar categoría" and the trimmed inline name field contains at
   least 1 non-whitespace character, THE `NewProductViewModel` SHALL call
   `CategoryRepository.insert` with a new `Category` whose `id` is a fresh UUID and whose
   `associatedMenuId` equals `NewProductUiState.selectedMenu.id`; upon success it SHALL set
   `NewProductUiState.selectedCategory` to the newly created category, clear the inline name
   field, and hide the inline creation UI.
5. IF `CategoryRepository.insert` throws an exception during inline category creation, THEN
   THE `NewProductViewModel` SHALL set `NewProductUiState.error` to a non-null descriptive
   message and SHALL NOT hide the inline creation UI.
6. WHEN the user taps "Guardar categoría" and the trimmed inline name field is empty, THE
   `NewProductViewModel` SHALL set `NewProductUiState.newCategoryNameError` to a non-null
   error message and SHALL NOT call `CategoryRepository.insert`.
7. WHEN the user types at least one character in the inline name field while
   `NewProductUiState.newCategoryNameError` is non-null, THE `NewProductViewModel` SHALL
   clear `NewProductUiState.newCategoryNameError` immediately.
8. WHEN the user taps "Cancelar" on the inline creation UI, THE `Modal` SHALL hide the
   inline creation field, clear the draft name text, and clear any
   `NewProductUiState.newCategoryNameError` without modifying `selectedCategory`.
9. WHILE `NewProductUiState.selectedMenu` is `null`, THE category
   `ExposedDropdownMenuBox` SHALL be rendered with `enabled = false`.
10. IF the user attempts to submit the full form and `NewProductUiState.selectedCategory` is
    `null`, THEN THE `NewProductViewModel` SHALL set `NewProductUiState.categoryError` to a
    non-null error message and SHALL NOT begin the save transaction.

---

### Requirement 6: Customizations Section — Group Management

**User Story:** As a cashier, I want to add customization groups to a new product, so that
customers can personalise their order (e.g. choose ingredients or size).

#### Acceptance Criteria

1. THE `Modal` SHALL display a section header labelled "Personalizaciones" followed by a
   "+ Grupo" `OutlinedButton`.
2. WHEN the user taps "+ Grupo", THE `NewProductViewModel` SHALL append a new `GroupDraft`
   to `NewProductUiState.groups`; the new `GroupDraft` SHALL have an empty `groupName`, a
   default `selectionType` of `SelectionType.MULTIPLE_CHECKBOXES`, and exactly one
   `OptionDraft` with an empty `optionName` and `extraPrice = 0.0`.
3. WHEN the user taps the trash `IconButton` on a group card, THE `NewProductViewModel` SHALL
   remove the `GroupDraft` at that index from `NewProductUiState.groups`.
4. WHEN the user edits the group name `TextField`, THE `NewProductViewModel` SHALL update the
   `groupName` of the corresponding `GroupDraft` in `NewProductUiState.groups` without
   recreating any other `GroupDraft` or `OptionDraft` in the list.
5. THE `Modal` SHALL render each `GroupDraft` as a card containing: an `OutlinedTextField`
   for the group name labelled "Nombre del grupo" with a maximum of 120 characters, a
   `SelectionTypeDropdown` composable, a trash `IconButton`, the list of `OptionDraft` rows,
   and an "+ Agregar opción" `TextButton` at the bottom of the card.
6. WHEN the user attempts to submit and a `GroupDraft.groupName` is empty or contains only
   whitespace after trimming, THE `NewProductViewModel` SHALL set the `groupNameError` of
   every such `GroupDraft` to a non-null error message simultaneously and SHALL NOT begin
   the save transaction.
7. WHEN the user types at least one non-whitespace character in a group name field while
   its `groupNameError` is non-null, THE `NewProductViewModel` SHALL clear that
   `groupNameError` immediately.

---

### Requirement 7: Customizations Section — Selection Type Dropdown

**User Story:** As a cashier, I want to choose whether a customization group allows multiple
selections or only one, so that the POS enforces the correct ordering rules.

#### Acceptance Criteria

1. EACH group card SHALL display an `ExposedDropdownMenuBox` labelled "Comportamiento" with
   exactly two entries: "Casillas (múltiple)" mapping to `SelectionType.MULTIPLE_CHECKBOXES`
   and "Opción única" mapping to `SelectionType.SINGLE_OPTION`.
2. THE `ExposedDropdownMenuBox` for a group SHALL display the label that corresponds to the
   `GroupDraft.selectionType` value currently stored in `NewProductUiState.groups` for that
   group.
3. WHEN the user selects a new entry from the "Comportamiento" dropdown of a group card, THE
   `NewProductViewModel` SHALL update only the `selectionType` field of that specific
   `GroupDraft`, leaving the `groupName`, `options`, `draftId`, and all other `GroupDraft`
   instances unchanged.
4. WHEN a new `GroupDraft` is appended to `NewProductUiState.groups`, its
   `selectionType` SHALL be `SelectionType.MULTIPLE_CHECKBOXES` and the dropdown SHALL
   display "Casillas (múltiple)".

---

### Requirement 8: Customizations Section — Option Row Management

**User Story:** As a cashier, I want to add, edit, and remove individual options inside each
customization group, so that the product offers the right set of choices.

#### Acceptance Criteria

1. THE `Modal` SHALL render each `OptionDraft` row with: an `OutlinedTextField` for
   `optionName` labelled "Nombre de la opción" with a maximum of 120 characters, a numeric
   `OutlinedTextField` for `extraPrice` labelled "Precio extra ($)" with
   `KeyboardType.Decimal` accepting non-negative values up to two decimal places, and an "X"
   `IconButton` to delete the row.
2. WHEN the user taps "+ Agregar opción" on a group card, THE `NewProductViewModel` SHALL
   append a new `OptionDraft` with an empty `optionName` and `extraPrice = 0.0` to the
   `options` list of only that `GroupDraft`, without modifying any other group's `options`
   list.
3. WHEN the user taps the "X" button on an option row, THE `NewProductViewModel` SHALL remove
   that `OptionDraft` from the `options` list of its parent `GroupDraft` without modifying
   any other group.
4. WHEN the user edits an `optionName` or `extraPrice` field, THE `NewProductViewModel` SHALL
   update only the targeted `OptionDraft` within the targeted `GroupDraft`, preserving
   referential identity of all sibling `OptionDraft` instances and all other `GroupDraft`
   instances.
5. WHEN the user attempts to submit and any `OptionDraft` has an empty `optionName` (after
   trimming), THE `NewProductViewModel` SHALL set the `optionNameError` of every such
   `OptionDraft` simultaneously to a non-null error message and SHALL NOT begin the save
   transaction.
6. WHEN the user attempts to submit and any `OptionDraft` has an `extraPrice` less than
   `0.0`, THE `NewProductViewModel` SHALL set the `optionPriceError` of every such
   `OptionDraft` simultaneously to a non-null error message and SHALL NOT begin the save
   transaction.
7. WHEN the user types at least one non-whitespace character in an option name field while
   its `optionNameError` is non-null, THE `NewProductViewModel` SHALL clear that
   `optionNameError` immediately.
8. WHEN the user edits an `extraPrice` field to a non-negative value while its
   `optionPriceError` is non-null, THE `NewProductViewModel` SHALL clear that
   `optionPriceError` immediately.

---

### Requirement 9: Save Transaction

**User Story:** As a cashier, I want pressing "Crear producto" to persist the entire product
tree atomically, so that the database is never left in a partial state if a write fails.

#### Acceptance Criteria

1. THE `Modal` SHALL display a "Crear producto" `Button` and a "Cancelar" `OutlinedButton` in
   a row at the bottom of the form.
2. WHEN the user presses "Crear producto" and `NewProductUiState` has no field-level errors
   and `NewProductUiState.name` is non-empty and `NewProductUiState.selectedCategory` is
   non-null, THE `NewProductViewModel` SHALL set `NewProductUiState.isSaving = true` and
   launch a coroutine that calls `AppDatabase.withTransaction`.
3. WHILE the transaction block is executing, THE `NewProductViewModel` SHALL insert the
   `ProductEntity` via `ProductRepository.insert`, then for each `GroupDraft` insert a
   `CustomizationGroupEntity` via `ProductRepository.insertGroup`, and for each `OptionDraft`
   within that group insert a `CustomizationOptionEntity` via `ProductRepository.insertOption`.
4. WHEN the transaction completes successfully, THE `NewProductViewModel` SHALL set
   `NewProductUiState.isSaving = false` and `NewProductUiState.saveResult =
   SaveResult.Success`.
5. WHEN `NewProductUiState.saveResult` is set to `SaveResult.Success`, THE `Modal` SHALL
   dismiss itself.
6. IF any write operation inside the transaction throws, THEN THE transaction SHALL be rolled
   back automatically by Room, THE `NewProductViewModel` SHALL set
   `NewProductUiState.isSaving = false`, and a non-null error message SHALL be displayed
   inside the `Modal` without dismissing it.
7. WHILE `NewProductUiState.isSaving = true`, THE "Crear producto" `Button` SHALL display a
   `CircularProgressIndicator` in place of its label and SHALL be disabled.
8. WHEN the user presses "Cancelar", THE `Modal` SHALL dismiss and THE `NewProductViewModel`
   SHALL reset `NewProductUiState` to the Initial Empty State, clearing `name`, `description`,
   `priceText`, validation errors, `saveResult`, `error`, `groups`, and `selectedCategory`.

---

### Requirement 10: ViewModel State — Nested List Correctness

**User Story:** As a developer, I want the ViewModel to manage nested group/option lists with
stable identity, so that Compose recompositions are minimal and no in-flight edits are lost.

#### Acceptance Criteria

1. WHEN a new `GroupDraft` is created, THE `NewProductViewModel` SHALL assign it a
   `draftId: String` equal to a freshly generated UUID; this `draftId` SHALL never be
   reassigned or mutated for the lifetime of that draft instance.
2. `LazyColumn` items that render `GroupDraft` rows SHALL use `GroupDraft.draftId` as the
   Compose `key` argument.
3. WHEN a new `OptionDraft` is created, THE `NewProductViewModel` SHALL assign it a
   `draftId: String` equal to a freshly generated UUID; this `draftId` SHALL never be
   reassigned or mutated for the lifetime of that draft instance.
4. WHEN THE `NewProductViewModel` updates a single field of one `GroupDraft`, it SHALL
   produce a new `NewProductUiState` where exactly the target element is replaced in the
   `groups` list; all other `GroupDraft` instances SHALL be `===`-equal to their prior
   counterparts (referential identity preserved for unmodified items).
5. WHEN THE `NewProductViewModel` updates a single field of one `OptionDraft`, it SHALL
   produce a new `NewProductUiState` where exactly the target `OptionDraft` is replaced
   inside its parent `GroupDraft.options` list; all sibling `OptionDraft` instances and all
   other `GroupDraft` instances SHALL be `===`-equal to their prior counterparts.
6. THE `NewProductUiState` SHALL be a `data class` with only immutable (`val`) fields; all
   list fields SHALL be of type `List<…>` (not `MutableList`), so that the `StateFlow`
   emits a new object on every add, edit, or remove operation.
7. FOR ALL sequences of add-group, edit-group-name, add-option, edit-option, remove-option,
   and remove-group operations, THE final `NewProductUiState.groups` list SHALL reflect every
   operation in order with no operation silently dropped or applied twice.
8. IF `removeGroup(index)` or `removeOption(groupIndex, optionIndex)` is called with an
   out-of-bounds index, THE `NewProductViewModel` SHALL leave `NewProductUiState` unchanged
   and SHALL NOT throw.

---

### Requirement 11: Correctness Properties for Property-Based Testing

**User Story:** As a developer, I want formally stated correctness properties for the ViewModel
state machine, so that property-based tests can validate the nested list logic with arbitrary
operation sequences.

#### Acceptance Criteria

1. FOR ALL sequences of `addGroup` calls of length N ≥ 0 starting from a state where
   `groups = emptyList()`, THE `NewProductUiState.groups` list SHALL have exactly N elements
   after N calls (size invariant).
2. FOR ALL sequences of `addGroup` calls of length N followed by `removeGroup(index)` for
   every valid index in descending order (indices N−1 down to 0), THE resulting
   `NewProductUiState.groups` list SHALL be empty (add/remove inverse).
3. FOR ALL `GroupDraft`s in `NewProductUiState.groups`, EACH `GroupDraft.draftId` SHALL be
   unique across the entire list (no duplicate keys).
4. FOR ALL sequences of `addOption(groupIndex)` calls of length M ≥ 0 on a single group
   starting from its initial state (1 option), THE `GroupDraft.options` list for that group
   SHALL have exactly M + 1 elements after M calls.
5. WHEN `updateGroupName(index, newName)` is called with a valid index, THE `draftId` of the
   `GroupDraft` at that index SHALL remain unchanged, and the `draftId` values of all other
   `GroupDraft` instances in the list SHALL also remain unchanged.
6. WHEN `updateOptionName(groupIndex, optionIndex, newName)` is called with valid indices,
   THE `draftId` of the `OptionDraft` at that position SHALL remain unchanged, and the
   `draftId` values of all sibling `OptionDraft` instances SHALL also remain unchanged.
7. FOR ALL `GroupDraft.options` lists, EACH `OptionDraft.draftId` SHALL be unique within its
   parent group (no duplicate option keys within a group).
