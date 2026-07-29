# Design Document — 07 Category Improvements

## Overview

Este spec introduce dos mejoras independientes pero relacionadas a `ConfigurationScreen`:

1. **Ordenamiento determinístico de productos** — Se añade `ORDER BY name COLLATE NOCASE ASC, id ASC` a `ProductDao.getProductsByCategory()`. Room emite un nuevo valor del Flow cada vez que cambia cualquier fila de la tabla `products`; sin un `ORDER BY` explícito, SQLite puede reordenar las filas entre emisiones. La corrección garantiza que la lista sea siempre estable sin importar qué operación desencadenó la reemisión.

2. **Eliminación de categoría con confirmación** — Se añade un `IconButton` de papelera junto a `CategoryTabsRow`. El botón solo se muestra cuando `selectedCategory != null`. Al pulsarlo, un `AlertDialog` de Material 3 pide confirmación antes de invocar `CategoryRepository.deleteById()`. Tras la eliminación exitosa se limpia `selectedCategory` (el pipeline reactivo existente auto-selecciona la primera categoría restante). Los errores se propagan a `ConfigurationUiState.error`.

Ambas mejoras son quirúrgicas: no alteran la arquitectura ni el flujo de datos existente más allá de los puntos de cambio específicos descritos.

---

## Architecture

La aplicación sigue una arquitectura MVVM de una sola capa de datos:

```
ConfigurationScreen (Composable)
        │  collectAsStateWithLifecycle
        ▼
ConfigurationViewModel
        │  getProductsByCategory()  ◄─── FIX: ORDER BY name COLLATE NOCASE ASC, id ASC
        ▼
ProductRepository ──► ProductDao ──► Room DB (products table)
        │
        │  deleteById() / getCategoriesByMenu()
        ▼
CategoryRepository ──► CategoryDao ──► Room DB (categories table)
```

### Flujo reactivo existente (sin cambios de estructura)

```
categoriesFlow (shareIn)
    │
    ├─► onEach { auto-select firstOrNull when current disappears }
    │
    └─► combine(...)
            │
_selectedCategory ──► flatMapLatest ──► rawProducts (Flow<List<Product>>)
            │
_searchQuery ──────► combine(rawProducts, query) ──► filteredProducts
            │
combine(all 8 streams) ──► uiState: StateFlow<ConfigurationUiState>
```

La eliminación de categoría encaja en este pipeline sin alterarlo: tras `deleteById()` exitoso, Room emite la nueva lista de categorías, el `onEach` detecta que `selectedCategory` ya no existe y llama a `firstOrNull()`, y el `StateFlow` se actualiza en cascada automáticamente.

---

## Components and Interfaces

### 1. `ProductDao` — cambio de query

```kotlin
// ANTES
@Query("SELECT * FROM products WHERE categoryId = :categoryId")
fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

// DESPUÉS
@Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC, id ASC")
fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>
```

`COLLATE NOCASE` de SQLite compara sin distinción de mayúsculas/minúsculas para caracteres ASCII. El desempate secundario `id ASC` garantiza orden estable cuando dos productos tienen el mismo nombre.

### 2. `ConfigurationUiState` — nuevo campo

```kotlin
data class ConfigurationUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val expandedProductMenuId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteCategoryDialog: Boolean = false   // NUEVO
)
```

### 3. `ConfigurationViewModel` — tres nuevas funciones públicas

```kotlin
/** Muestra el diálogo de confirmación sin ejecutar eliminación. */
fun requestDeleteCategory() {
    _showDeleteCategoryDialog.value = true
}

/** Cancela el diálogo sin cambios. */
fun dismissDeleteCategoryDialog() {
    _showDeleteCategoryDialog.value = false
}

/**
 * Confirma la eliminación: invoca deleteById(), limpia selectedCategory y cierra el diálogo.
 * En caso de excepción conserva selectedCategory y propaga el error.
 */
fun confirmDeleteCategory() {
    val categoryToDelete = _selectedCategory.value ?: return
    _showDeleteCategoryDialog.value = false
    viewModelScope.launch {
        try {
            categoryRepository.deleteById(categoryToDelete.id)
            // selectedCategory se limpia vía el onEach del categoriesFlow
            // (Room emite la nueva lista y firstOrNull() es invocado).
        } catch (e: Exception) {
            _error.value = e.message ?: "Error desconocido"
        }
    }
}
```

Se añade el `MutableStateFlow` correspondiente:

```kotlin
private val _showDeleteCategoryDialog = MutableStateFlow(false)
```

Y se incluye en el `combine` del `uiState`:

```kotlin
combine(
    categoriesFlow,
    _selectedCategory,
    rawProducts,
    filteredProducts,
    _searchQuery,
    _expandedMenuId,
    _error,
    _isLoading,
    _showDeleteCategoryDialog      // NUEVO — combine de 9 streams
) { args -> ... }
```

> **Nota de implementación:** El `combine` de Kotlin Coroutines soporta hasta 6 parámetros como lambdas tipadas; para más argumentos se usa la sobrecarga de `Array<*>` que ya existe en el ViewModel (`args[N]`). Al añadir el 9.º stream se extiende ese array.

### 4. `ConfigurationScreen` — modificaciones de UI

#### 4a. Wrapper `CategoryTabsRow` + trash icon

La fila de pestañas se envuelve en un `Row` para acomodar el `IconButton` a la derecha:

```kotlin
// Row 1 — Category tabs + delete button
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    CategoryTabsRow(
        categories = uiState.categories,
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = { viewModel.selectCategory(it) },
        modifier = Modifier.weight(1f)
    )
    if (uiState.selectedCategory != null) {
        IconButton(onClick = { viewModel.requestDeleteCategory() }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar categoría"
            )
        }
    }
}
```

#### 4b. `DeleteCategoryDialog` — nuevo Composable privado

```kotlin
@Composable
private fun DeleteCategoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar categoría") },
        text = {
            Text("¿Estás seguro? Eliminar esta categoría eliminará permanentemente todos los productos dentro de ella.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
```

Se muestra condicionalmente en `ConfigurationScreen`:

```kotlin
if (uiState.showDeleteCategoryDialog) {
    DeleteCategoryDialog(
        onConfirm = { viewModel.confirmDeleteCategory() },
        onDismiss = { viewModel.dismissDeleteCategoryDialog() }
    )
}
```

---

## Data Models

No se introducen nuevas entidades de base de datos.

### `ConfigurationUiState` (actualizado)

| Campo | Tipo | Descripción |
|---|---|---|
| `categories` | `List<Category>` | Lista reactiva de categorías del menú |
| `selectedCategory` | `Category?` | Categoría actualmente seleccionada; `null` si no hay ninguna |
| `products` | `List<Product>` | Todos los productos de la categoría seleccionada (ordenados) |
| `filteredProducts` | `List<Product>` | Productos tras aplicar `searchQuery` |
| `searchQuery` | `String` | Texto actual del campo de búsqueda (máx. 100 chars) |
| `expandedProductMenuId` | `String?` | Id del producto con el menú desplegado abierto |
| `isLoading` | `Boolean` | `true` hasta la primera emisión de categorías |
| `error` | `String?` | Mensaje de error a mostrar; `null` si no hay error |
| `showDeleteCategoryDialog` | `Boolean` | **NUEVO** — `true` cuando el `AlertDialog` debe estar visible |

### Invariantes de estado

- `showDeleteCategoryDialog == true` implica `selectedCategory != null` (el botón solo es accesible cuando hay categoría seleccionada).
- `showDeleteCategoryDialog` vuelve a `false` en todos los caminos de salida: cancelar, confirmar exitoso y confirmar con error.

---

## Correctness Properties

*Una propiedad es una característica o comportamiento que debe ser verdadero en todas las ejecuciones válidas del sistema — esencialmente, una afirmación formal sobre lo que el software debe hacer. Las propiedades sirven como puente entre las especificaciones legibles por humanos y las garantías de corrección verificables por máquinas.*

### Property 1: Stable alphabetical ordering

*For any* list of products belonging to the same category, regardless of their capitalization, the order emitted by `getProductsByCategory` SHALL be sorted by `name` case-insensitively ascending, with `id` ascending as a stable tiebreaker for products whose names are case-insensitively equal.

**Validates: Requirements 1.1, 1.4**

### Property 2: Order invariance after isActive toggle

*For any* list of products, toggling the `isActive` field of any one product and re-observing the Flow SHALL yield a list where all products maintain the same relative order they had before the toggle.

**Validates: Requirements 1.2, 1.3**

> **Reflection note:** Property 2 is implied by Property 1 — if the ORDER BY is always applied, stability after mutation is automatic. However it is retained as an explicit regression property because it targets the exact bug scenario described in the spec (reordering on `isActive` toggle).

### Property 3: Trash button visibility matches selectedCategory

*For any* `ConfigurationUiState`, the trash `IconButton` SHALL be present and enabled if and only if `selectedCategory != null`.

**Validates: Requirements 2.1, 2.2**

### Property 4: Cancel preserves all state

*For any* selected category, opening the delete confirmation dialog and pressing "Cancelar" SHALL leave `selectedCategory`, the database content, and `showDeleteCategoryDialog` (set back to `false`) unchanged.

**Validates: Requirements 2.6**

### Property 5: Confirm calls deleteById with the correct id

*For any* selected category, pressing "Eliminar" in the confirmation dialog SHALL invoke `CategoryRepository.deleteById()` with exactly that category's `id`, and no other `id`.

**Validates: Requirements 2.7**

### Property 6: Successful deletion clears selectedCategory

*For any* category that is successfully deleted, `selectedCategory` SHALL be `null` immediately after deletion completes (before the auto-select logic runs), and `showDeleteCategoryDialog` SHALL be `false`.

**Validates: Requirements 2.8**

### Property 7: Auto-select first remaining category after deletion

*For any* category list of size N ≥ 2, after deleting the currently selected category, `selectedCategory` SHALL equal `categories.firstOrNull()` of the remaining N-1 categories.

**Validates: Requirements 2.9**

### Property 8: Error handling preserves selectedCategory and sets error field

*For any* selected category and any exception thrown by `deleteById()`, `selectedCategory` SHALL retain its value prior to the deletion attempt, `error` in `ConfigurationUiState` SHALL be non-null (set to the exception message), and `showDeleteCategoryDialog` SHALL be `false`.

**Validates: Requirements 2.10, 2.11**

---

## Error Handling

| Scenario | Handling |
|---|---|
| `deleteById()` throws any `Exception` | `viewModelScope.launch` catches it; `_error.value` is set to `e.message ?: "Error desconocido"`; `selectedCategory` is not changed; dialog is already closed (dismissed before the coroutine runs) |
| `deleteById()` succeeds but `categoriesFlow` is slow to emit | `selectedCategory` is set to `null` by the ViewModel `onEach` as soon as the updated list arrives; UI shows loading/empty state in the interim |
| Trash button pressed when `selectedCategory` becomes `null` mid-flight | `requestDeleteCategory()` is a no-op guard: the button is only rendered when `selectedCategory != null`; `confirmDeleteCategory()` has an early-return guard (`?: return`) |
| Empty product list for a category | `getProductsByCategory` returns an empty Flow; UI shows "No hay productos en esta categoría" (existing state machine) |

All errors surface through `ConfigurationUiState.error`, which the existing error branch in the `Box` state machine already renders. The user can clear the error via `clearError()` (already implemented) and retry.

---

## Testing Strategy

### Unit tests (ViewModel logic — JUnit + MockK/Turbine)

- `requestDeleteCategory()` sets `showDeleteCategoryDialog = true`.
- `dismissDeleteCategoryDialog()` sets `showDeleteCategoryDialog = false` without side effects.
- `confirmDeleteCategory()` with a null `selectedCategory` is a no-op (no call to repository).
- `confirmDeleteCategory()` calls `categoryRepository.deleteById(id)` with the correct id.
- `confirmDeleteCategory()` closes the dialog before launching the coroutine.
- On success, `selectedCategory` eventually becomes `null` (via the reactive pipeline).
- On exception, `error` is set to the exception message and `selectedCategory` is unchanged.

### Property-based tests (Room in-memory DB — Robolectric or Android instrumented)

The property-based tests use **Kotest with the `kotest-property` module** (already available or to be added). Each test runs a minimum of **100 iterations**.

#### Property 1 — Stable alphabetical ordering
```
// Feature: 07_category_improvements, Property 1: Stable alphabetical ordering
forAll(Arb.list(productArb(), 1..20)) { products ->
    // Insert all products into in-memory DB under same categoryId
    // Collect first emission of getProductsByCategory(categoryId)
    // Assert emittedList == products.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
}
```

#### Property 2 — Order invariance after isActive toggle
```
// Feature: 07_category_improvements, Property 2: Order invariance after isActive toggle
forAll(Arb.list(productArb(), 2..20), Arb.int(0..19)) { products, toggleIdx ->
    // Insert products, collect initial order
    // Toggle isActive of products[toggleIdx % products.size]
    // Collect new order
    // Assert new order (excluding toggled) == initial order (excluding toggled) by relative position
}
```

#### Property 3 — Trash button visibility
```
// Feature: 07_category_improvements, Property 3: Trash button visibility
forAll(Arb.orNull(categoryArb())) { selectedCategory ->
    // Render ConfigurationScreen with given selectedCategory in state
    // Assert trashButton.isDisplayed() == (selectedCategory != null)
}
```

#### Property 4 — Cancel preserves state
```
// Feature: 07_category_improvements, Property 4: Cancel preserves all state
forAll(categoryArb()) { category ->
    // Set selectedCategory = category, call requestDeleteCategory()
    // Call dismissDeleteCategoryDialog()
    // Assert selectedCategory == category, showDeleteCategoryDialog == false, deleteById not called
}
```

#### Property 5 — Confirm calls deleteById with correct id
```
// Feature: 07_category_improvements, Property 5: Confirm calls deleteById with correct id
forAll(categoryArb()) { category ->
    // Set selectedCategory = category
    // Call confirmDeleteCategory()
    // Assert deleteById(category.id) was called exactly once
}
```

#### Property 6 — Successful deletion clears selectedCategory
```
// Feature: 07_category_improvements, Property 6: Successful deletion clears selectedCategory
forAll(categoryArb()) { category ->
    // Mock deleteById to succeed
    // confirmDeleteCategory()
    // Assert showDeleteCategoryDialog == false
    // Assert selectedCategory eventually becomes null via reactive pipeline
}
```

#### Property 7 — Auto-select after deletion
```
// Feature: 07_category_improvements, Property 7: Auto-select first remaining category
forAll(Arb.list(categoryArb(), 2..10)) { categories ->
    // Set up categoriesFlow to emit categories
    // Set selectedCategory = categories[0]
    // Delete categories[0], categories flow emits categories.drop(1)
    // Assert selectedCategory == categories[1] (first remaining)
}
```

#### Property 8 — Error handling
```
// Feature: 07_category_improvements, Property 8: Error handling preserves selectedCategory
forAll(categoryArb(), Arb.string()) { category, errorMsg ->
    // Mock deleteById to throw RuntimeException(errorMsg)
    // confirmDeleteCategory()
    // Assert selectedCategory == category (unchanged)
    // Assert error == errorMsg
    // Assert showDeleteCategoryDialog == false
}
```

### Integration tests (Android Instrumented)

- Insert categories and products into a real Room DB; verify `getProductsByCategory` emits in correct order after an `isActive` toggle (end-to-end ordering regression test).
- Verify `CategoryDao.deleteById()` cascades to delete all associated products (existing `CascadeDeletionTest.kt` covers this).

### Composable snapshot / example tests

- `DeleteCategoryDialog` renders with the correct confirmation message and exactly two buttons ("Eliminar", "Cancelar").
- After a failed delete, the error message is displayed and the screen remains interactive (Requirement 2.12).
