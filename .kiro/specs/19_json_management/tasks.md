# Implementation Plan: JSON Catalog Management

## Overview

Plan de implementación para la funcionalidad de importar, exportar y modificar el catálogo del POS vía JSON. Se sigue un orden que garantiza compilación en cada paso.

## Tasks

- [x] 1. Agregar kotlinx-serialization version, library, y plugin a `libs.versions.toml`
  - [x] 1.1 Agregar version `kotlinxSerialization = "1.8.1"` al bloque `[versions]`
  - [x] 1.2 Agregar library `kotlinx-serialization-json` al bloque `[libraries]`
  - [x] 1.3 Agregar plugin `kotlin-serialization` al bloque `[plugins]`
- [x] 2. Aplicar plugin de serialización en los build.gradle.kts
  - [x] 2.1 Agregar `alias(libs.plugins.kotlin.serialization) apply false` al root `build.gradle.kts`
  - [x] 2.2 Agregar `alias(libs.plugins.kotlin.serialization)` al plugins block de `app/build.gradle.kts`
  - [x] 2.3 Agregar `implementation(libs.kotlinx.serialization.json)` a dependencies de `app/build.gradle.kts`
- [x] 3. Crear DTOs de serialización en package `data.json`
  - [x] 3.1 Crear archivo `CatalogExportDto.kt` con las 7 data classes @Serializable (CatalogExport, CatalogData, MenuItemDto, CategoryDto, ProductDto, CustomizationGroupDto, CustomizationOptionDto)
- [x] 4. Agregar queries one-shot y deleteAll a los DAOs
  - [x] 4.1 Agregar `getAllMenuItemsOnce()` a MenuItemDao
  - [x] 4.2 Agregar `getCategoriesByMenuOnce()` a CategoryDao
  - [x] 4.3 Agregar `getProductsByCategoryOnce()` a ProductDao
  - [x] 4.4 Agregar `deleteAll()` a MenuItemDao, CategoryDao, ProductDao, CustomizationGroupDao, CustomizationOptionDao
- [x] 5. Crear CatalogJsonRepository con exportación e importación
  - [x] 5.1 Crear clase `CatalogJsonRepository` con constructor que recibe AppDatabase y todos los DAOs
  - [x] 5.2 Implementar `exportCatalogToJson(): String`
  - [x] 5.3 Implementar `validate(export: CatalogExport): Result<Unit>`
  - [x] 5.4 Implementar `importCatalogFromJson(jsonString: String): Result<Int>`
- [x] 6. Extender ConfigurationViewModel con estados y funciones JSON
  - [x] 6.1 Agregar CatalogJsonRepository al constructor y Factory
  - [x] 6.2 Agregar nuevos campos al ConfigurationUiState (isExporting, isImporting, showJsonEditor, jsonEditorContent, jsonEditorError, showImportConfirmDialog, importPendingJson, toastMessage)
  - [x] 6.3 Agregar funciones públicas (exportCatalog, onExportUriReceived, importCatalogFromUri, confirmImport, dismissImportDialog, openJsonEditor, closeJsonEditor, applyJsonEditorChanges, clearToast)
- [x] 7. Integrar SAF launchers en ConfigurationScreen
  - [x] 7.1 Agregar rememberLauncherForActivityResult para CreateDocument (exportar)
  - [x] 7.2 Agregar rememberLauncherForActivityResult para OpenDocument (importar)
  - [x] 7.3 Conectar los callbacks de los botones a los launchers y al ViewModel
- [x] 8. Crear diálogo de confirmación de importación
  - [x] 8.1 Crear composable ImportConfirmDialog y mostrarlo cuando showImportConfirmDialog == true
  - [x] 8.2 Conectar botones del diálogo a viewModel.confirmImport() y viewModel.dismissImportDialog()
- [x] 9. Crear modal del editor JSON
  - [x] 9.1 Crear composable JsonEditorDialog con TextField monospace multilínea
  - [x] 9.2 Mostrar cuando showJsonEditor == true, conectar botones Aplicar y Cancelar
- [x] 10. Agregar Toast feedback y wiring de DI
  - [x] 10.1 Mostrar Toast cuando toastMessage no sea null y llamar clearToast() después
  - [x] 10.2 Localizar donde se construye ConfigurationViewModel.Factory e inyectar CatalogJsonRepository

## Task Dependency Graph

```json
{
  "waves": [
    [1],
    [2],
    [3, 4],
    [5],
    [6],
    [7, 8, 9],
    [10]
  ]
}
```

Tasks 1-2: Infraestructura de serialización (build system)
Task 3: DTOs (requiere plugin de serialización)
Task 4: DAOs (independiente del plugin, ejecuta en paralelo con Task 3)
Task 5: Repository (requiere DTOs + DAOs)
Task 6: ViewModel (requiere Repository)
Tasks 7-9: UI (requieren ViewModel actualizado, pueden ejecutarse en paralelo)
Task 10: Wiring final (requiere todo lo anterior)

## Notes

- Cada tarea deja el proyecto en estado compilable.
- Las órdenes (orders, order_items, order_item_customizations) nunca se tocan.
- Se usa estrategia replace-all en importación (delete all + insert all) dentro de una transacción Room.
- Los ActivityResultLauncher se registran en el Composable, no en el ViewModel.
