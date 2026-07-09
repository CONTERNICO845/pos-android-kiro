# Requirements Document

## Introduction

This document defines the requirements for the **04 Configuration Screen UI** feature of the
PuntoDeVenta Android POS app. This is **Phase 2** of the product-catalog module; the data layer
(Phase 1 — Room entities, DAOs, and repositories for `Category`, `Product`, and customization
tables) is already complete and must not be modified.

The Configuration Screen is reached by tapping the **Configuración** destination in the existing
static `NavigationRail`. It allows POS operators to browse all products in a selected category,
toggle each product's active state, and access per-product actions (edit, duplicate, delete) via a
contextual dropdown. JSON import/export actions and a "Nuevo Producto" entry point are also
surfaced in the top bar; the actual JSON handling and the "Nuevo Producto" modal are out of scope
for this phase.

**Version:** 1.0
**Status:** Draft
**Phase:** Phase 2 — UI Layer (data layer from Phase 1 is the foundation)

---

## Glossary

| Term | Definition |
|---|---|
| **ConfigurationScreen** | The full Compose screen shown when the user selects the Configuración destination in the Nav Rail |
| **ConfigurationViewModel** | The `ViewModel` that exposes `ConfigurationUiState` to the UI and mediates all read/write operations against `CategoryRepository` and `ProductRepository` |
| **ConfigurationUiState** | An immutable data class holding all state required to render `ConfigurationScreen` without additional data fetching |
| **CategoryTab** | One selectable tab or chip in the top bar that corresponds to a single `Category` loaded from `CategoryRepository` |
| **ProductCard** | A `Card` composable representing one `Product` from the selected category; shows emoji, name, price, an active toggle, and a settings icon |
| **ProductActionMenu** | A `DropdownMenu` anchored to the settings icon on a `ProductCard`; offers "Editar", "Duplicar", and "Eliminar" |
| **NavRail** | The existing static `NavigationRail` pinned to the left edge of the screen (defined in feature 01; must not be modified) |
| **CategoryRepository** | Phase 1 repository exposing `getCategoriesByMenu(menuId): Flow<List<Category>>` |
| **ProductRepository** | Phase 1 repository exposing `getProductsByCategory(categoryId): Flow<List<Product>>` and `insert(product)` / `deleteById(id)` |
| **isActive** | Boolean field on `Product`; `true` = product is shown to customers; `false` = product is hidden |
| **basePrice** | Double field on `Product`; the displayed price in currency units |
| **DarkGreen** | Primary brand color for this screen's UI chrome (`#2D5A1B` — same as `CardBackground` from `Color.kt`) |
| **LightGreen** | Secondary brand accent color (`#6BBF3E` — same as `BackgroundPrimary` from `Color.kt`) |

---

## Requirements

---

### US-01 — Display the Configuration Screen within the existing Nav Rail layout

**User Story:**
As a POS operator, I want the Configuration Screen to appear in the existing app layout when I tap
"Configuración" in the Nav Rail, so that the navigation experience stays consistent with the rest
of the app.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-01.1 | **When** the user taps the **Configuración** destination in the Nav Rail, **the ConfigurationScreen** shall replace the current content area without relaunching the Activity or hiding the Nav Rail. |
| AC-01.2 | **While** ConfigurationScreen is displayed, **the NavRail** shall remain fully visible and interactive at the left edge of the screen with the same width, item layout, and interactive state as when any other destination is selected. |
| AC-01.3 | **When** ConfigurationScreen is first composed, **the ConfigurationViewModel** shall begin collecting category and product data from the repositories and the ConfigurationScreen shall show a loading indicator until the first emission arrives. |
| AC-01.4 | **While** the ConfigurationScreen is displayed, **the ConfigurationScreen** shall fill the entire content area to the right of the NavRail with no overlap between the NavRail and the screen content. |
| AC-01.5 | **When** `ConfigurationViewModel` encounters an error while collecting from `CategoryRepository` or `ProductRepository`, **the ConfigurationScreen** shall hide the product list and category tabs and display an error message in the content area; the NavRail shall remain visible and functional. |

---

### US-02 — Category selector in the top bar

**User Story:**
As a POS operator, I want to see all available categories listed as tabs at the top of the
Configuration Screen, so that I can switch between categories to manage their products.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-02.1 | **When** the ConfigurationScreen top bar is rendered, **the ConfigurationScreen** shall display one selectable tab or chip per `Category` returned by `CategoryRepository.getCategoriesByMenu`, using the category `name` as the label. |
| AC-02.2 | **When** the category list is empty, **the ConfigurationScreen** shall display an empty tab row and an informational message in the product list area stating "No hay categorías disponibles". |
| AC-02.3 | **When** the user taps a category tab, **the ConfigurationViewModel** shall set the selected category to the tapped `Category` and switch the product Flow subscription to that category's products. |
| AC-02.4 | **When** the user taps a category tab, **the ConfigurationScreen** shall update the product list to show only the products emitted by the new category's Flow. |
| AC-02.5 | **When** ConfigurationScreen first loads and at least one category exists, **the ConfigurationViewModel** shall select the first `Category` in the list as emitted by `CategoryRepository.getCategoriesByMenu` by default. |
| AC-02.6 | **While** a category tab is selected, **the ConfigurationScreen** shall render the selected tab's indicator using `CardBackground` (`#2D5A1B`) and the selected tab's label text using `NavRailIconSelected` (`#4A8C1C`), distinguishing it from unselected tabs. |
| AC-02.7 | **When** the category list changes (a category is added or removed externally), **the ConfigurationViewModel** shall reactively update the tab list without requiring a manual refresh. |
| AC-02.8 | **When** the currently selected category is removed from the list externally, **the ConfigurationViewModel** shall automatically select the first category remaining in the updated list, or set the selected category to null if the list becomes empty. |

---

### US-03 — Top bar static action buttons

**User Story:**
As a POS operator, I want access to product management actions at the top of the screen, so that
I can create, import, and export products without leaving the Configuration Screen.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-03.1 | **When** the ConfigurationScreen top bar is rendered, **the ConfigurationScreen** shall display the button **"+ Nuevo Producto"** at the trailing (end) edge of the top bar. |
| AC-03.2 | **When** the user taps **"+ Nuevo Producto"**, **the ConfigurationScreen** shall print a log message containing `"Nuevo Producto clicked"` and shall not open any dialog or navigate to another screen (the modal is out of scope for this phase). |
| AC-03.3 | **When** the ConfigurationScreen top bar is rendered, **the ConfigurationScreen** shall display the buttons **"Modificar JSON"**, **"Importar JSON"**, and **"Exportar JSON"** on the leading side of **"+ Nuevo Producto"**. |
| AC-03.4 | **When** the user taps **"Modificar JSON"**, **"Importar JSON"**, or **"Exportar JSON"**, **the ConfigurationScreen** shall print a log message containing the label text of the tapped button and shall not perform any data operation (JSON handling is out of scope for this phase). |
| AC-03.5 | **When** the top bar buttons are rendered, **the ConfigurationScreen** shall style **"+ Nuevo Producto"** as a filled button with `CardBackground` (`#2D5A1B`) background and `CardText` (`#FFFFFF`) label text, and shall style the JSON buttons as outlined buttons with transparent background, 1dp `CardBackground` border, and `CardBackground` label text. |

---

### US-04 — Search field in the top bar

**User Story:**
As a POS operator, I want a search field in the top bar, so that I can quickly filter visible
products by name within the selected category.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-04.1 | **When** the ConfigurationScreen top bar is rendered, **the ConfigurationScreen** shall display a text input field labeled **"Buscar Producto"** to the left of the action buttons. |
| AC-04.2 | **When** the user types in the search field, **the ConfigurationViewModel** shall filter the displayed product list to show only `Product` items whose `name` contains the typed text as a case-insensitive substring, accepting at most 100 characters of input. |
| AC-04.3 | **When** the search field is empty, **the ConfigurationViewModel** shall display all products for the selected category without any filtering. |
| AC-04.4 | **When** the user selects a different category, **the ConfigurationViewModel** shall clear the search field and display all products for the newly selected category without any filtering. |
| AC-04.5 | **When** no products match the current search query, **the ConfigurationScreen** shall display a message "No se encontraron productos" in the product list area instead of an empty list. |

---

### US-05 — Product list displayed as a LazyColumn

**User Story:**
As a POS operator, I want to see all products in the selected category as a scrollable list, so
that I can manage each product individually.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-05.1 | **When** a category is selected and it has products, **the ConfigurationScreen** shall display those products in a `LazyColumn`, with one `ProductCard` per product, using the product `id` as the stable item key. |
| AC-05.2 | **When** a category is selected, it has no products, and the search field is empty, **the ConfigurationScreen** shall display the message "No hay productos en esta categoría" in the list area (distinct from the search-empty-state message in AC-04.5). |
| AC-05.3 | **While** the product list is displayed, **the ConfigurationViewModel** shall reactively update the list whenever a product is inserted, deleted, or updated in `ProductRepository`. |
| AC-05.4 | **While** the product list is rendered, **the ConfigurationScreen** shall display products in the order they are emitted by `ProductRepository.getProductsByCategory`. |

---

### US-06 — Product Card design

**User Story:**
As a POS operator, I want each product card to clearly display the product's emoji, name, and
price with consistent styling, so that I can identify products at a glance.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-06.1 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall use `CardBackground` (`#2D5A1B`) as the card surface color. |
| AC-06.2 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall display the product `emoji`, `name`, and `basePrice` using `CardText` (`#FFFFFF`) text color. |
| AC-06.3 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall display `basePrice` formatted as a currency string with the `$` symbol and exactly two decimal places using half-up rounding (e.g., `"$12.50"`). |
| AC-06.4 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall lay out the card in a single horizontal row: emoji on the leading side, then a vertical column with the product name on top and price below it, then the card controls (Switch above settings icon) anchored to the trailing side. |
| AC-06.5 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall apply `RoundedCornerShape(8.dp)` to the card, 8dp bottom padding per card (producing an 8dp gap between consecutive cards), and 16dp horizontal content padding inside the card. |

---

### US-07 — Active/Inactive toggle on Product Card

**User Story:**
As a POS operator, I want a toggle switch on each product card to quickly activate or deactivate a
product, so that I can control which products are visible to customers without deleting them.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-07.1 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall display a Material Design 3 `Switch` component on the trailing side of the card reflecting the product's current `isActive` value. |
| AC-07.2 | **When** the `Switch` is in the `checked = true` state, **the ConfigurationScreen** shall render the switch thumb and track using green colors (thumbColor = `#FFFFFF`, trackColor = `#4CAF50`). |
| AC-07.3 | **When** the `Switch` is in the `checked = false` state, **the ConfigurationScreen** shall render the switch track using red color (`#E53935`) and the thumb using white (`#FFFFFF`). |
| AC-07.4 | **When** the user toggles the `Switch` on a ProductCard, **the ConfigurationViewModel** shall call `ProductRepository.insert` with a copy of the product where `isActive` is set to the new boolean value. |
| AC-07.5 | **When** `ProductRepository.insert` completes after a toggle, **the ConfigurationViewModel** shall not require any manual refresh; the reactive `Flow` from `ProductRepository` shall automatically emit the updated product list. |

---

### US-08 — Per-product settings dropdown menu

**User Story:**
As a POS operator, I want a settings icon on each product card that opens a dropdown menu with
product actions, so that I can edit, duplicate, or delete a product without leaving the list.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-08.1 | **When** a ProductCard is rendered, **the ConfigurationScreen** shall display a settings icon (`Icons.Default.Settings`) on the trailing side of the card, adjacent to the Switch. |
| AC-08.2 | **When** the user taps the settings icon, **the ConfigurationScreen** shall expand a Material Design 3 `DropdownMenu` anchored to the icon, containing exactly three items in this order: **"Editar"**, **"Duplicar"**, **"Eliminar"**. |
| AC-08.3 | **When** the DropdownMenu is open and the user taps outside it or presses Back, **the ConfigurationScreen** shall dismiss the DropdownMenu without performing any action. |
| AC-08.4 | **When** the user taps **"Editar"** in the DropdownMenu, **the ConfigurationScreen** shall dismiss the menu and print a log message containing `"Editar: <productId>"` (the modal is out of scope for this phase). |
| AC-08.5 | **When** the user taps **"Duplicar"** in the DropdownMenu, **the ConfigurationViewModel** shall create a new `Product` with a new UUID as `id` and the following fields copied from the original: `emoji`, `name`, `description`, `basePrice`, `isActive`, and `categoryId`; insert it via `ProductRepository.insert`; then dismiss the DropdownMenu. |
| AC-08.6 | **When** `ProductRepository.insert` throws an exception during a "Duplicar" action, **the ConfigurationViewModel** shall dismiss the DropdownMenu, expose an error state in `ConfigurationUiState`, and leave the original product unchanged. |
| AC-08.7 | **When** the user taps **"Eliminar"** in the DropdownMenu, **the ConfigurationViewModel** shall call `ProductRepository.deleteById` with the product's `id`, dismiss the DropdownMenu, and the product card shall disappear from the list as the reactive Flow emits the updated product list. |
| AC-08.8 | **When** `ProductRepository.deleteById` throws an exception during an "Eliminar" action, **the ConfigurationViewModel** shall dismiss the DropdownMenu, expose an error state in `ConfigurationUiState`, and the product card shall remain visible in the list. |
| AC-08.9 | **While** a DropdownMenu is open for one product, **the ConfigurationScreen** shall ensure only one DropdownMenu is expanded at a time; opening the menu for a second product shall close the first. |

---

### US-09 — ConfigurationViewModel reactive data pipeline

**User Story:**
As a developer, I want a `ConfigurationViewModel` that connects the UI to the Phase 1 repositories
using reactive Kotlin Flows, so that the screen always reflects the current database state without
manual refresh logic.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-09.1 | **When** `ConfigurationViewModel` is initialized, **the ConfigurationViewModel** shall expose a `StateFlow<ConfigurationUiState>` that the UI collects via `collectAsStateWithLifecycle()`. |
| AC-09.2 | **When** the selected category changes, **the ConfigurationViewModel** shall switch the product `Flow` subscription to the new category's `Flow` and the `StateFlow` shall emit an updated `ConfigurationUiState`. |
| AC-09.3 | **The ConfigurationViewModel** shall hold `CategoryRepository` and `ProductRepository` as constructor parameters and SHALL NOT access `AppDatabase` or any DAO directly. |
| AC-09.4 | **When** `ConfigurationViewModel` is created, **the ConfigurationViewModel** shall accept the `menuId` of the active menu as a constructor parameter and pass it to `CategoryRepository.getCategoriesByMenu(menuId)`. |
| AC-09.5 | **The ConfigurationViewModel** shall provide a `ViewModelProvider.Factory` inner class that constructs it with both repositories and the `menuId`. |
| AC-09.6 | **When** `ConfigurationViewModel` is created using `SharingStarted.WhileSubscribed(5_000)` for `stateIn`, **the ConfigurationViewModel** shall tear down the upstream Flows 5 seconds after the last UI subscriber leaves, preserving resources across configuration changes. |

---

### US-10 — Color palette and Material Design 3 compliance

**User Story:**
As a developer, I want all colors in the Configuration Screen to come from the global `Color.kt`
palette and all interactive components to use Material Design 3 primitives, so that the screen
integrates visually with the existing app and remains maintainable.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-10.1 | **Where** a color value is used in ConfigurationScreen or any composable created for this feature, **the implementation** shall reference either a named `val` token from `ui/theme/Color.kt` or a `MaterialTheme.colorScheme.*` accessor; **IF** an inline `Color(0x...)` or `Color(r, g, b)` literal appears anywhere outside `Color.kt`, **THEN** that is a compliance violation. |
| AC-10.2 | **When** interactive components are rendered in the ConfigurationScreen, **the ConfigurationScreen** shall use MD3 primitives: `Card`, `Switch`, `DropdownMenu`, `DropdownMenuItem`, `OutlinedTextField`, `FilledTonalButton`, and `OutlinedButton`; for the category selector, **IF** the category list overflows the available width, **THEN** `ScrollableTabRow` shall be used, **ELSE** `TabRow` shall be used; the implementation shall not substitute any of these components with non-MD3 alternatives. |
| AC-10.3 | **When** the ConfigurationScreen is rendered, **the ConfigurationScreen** shall not contain a nested `MaterialTheme` call or a `CompositionLocalProvider` overriding `LocalColorScheme`; all colors that do not require explicit `CardBackground`, `NavRailIconSelected`, `BackgroundPrimary`, or `BackgroundSecondary` overrides shall be resolved from the ambient `MaterialTheme.colorScheme`. |

---

## Out of Scope (for this phase)

- **"Nuevo Producto" modal** — tapping the button prints a log only; the create-product dialog is defined in Phase 3.
- **JSON Modificar / Importar / Exportar logic** — buttons print logs only; actual JSON parsing, import, and export is a separate feature.
- **Edit product modal** — "Editar" from the dropdown menu prints a log only; the edit form is Phase 3.
- **Category creation or deletion** — categories are read-only in this phase; management of categories is a future feature.
- **Confirmation dialogs** — "Eliminar" deletes immediately without a confirmation prompt in this phase.
- **Undo / redo** for deleted or toggled products.
- **Printer, Tickets, Estadísticas** screen content — placeholders remain unchanged.
- **Offline sync or network operations** of any kind.
