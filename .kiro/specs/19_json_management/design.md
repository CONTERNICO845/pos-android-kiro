# Design: JSON Catalog Management

## Overview

Este diseño define la funcionalidad para importar, exportar y modificar el catálogo completo del POS (MenuItems, Categories, Products, CustomizationGroups, CustomizationOptions) mediante archivos JSON. Se utiliza un esquema jerárquico anidado que refleja las relaciones FK de Room, y `kotlinx.serialization` como librería de serialización.

## Architecture

### Flujo de Exportación
```
ConfigurationScreen (botón "Exportar JSON")
  → ConfigurationViewModel.exportCatalog()
    → CatalogJsonRepository.exportCatalogToJson(): String
      → DAOs (queries jerárquicas)
    → SAF CreateDocument → OutputStream → write UTF-8
```

### Flujo de Importación
```
ConfigurationScreen (botón "Importar JSON")
  → SAF OpenDocument → InputStream → read UTF-8
    → ConfigurationViewModel.importCatalogFromUri()
      → CatalogJsonRepository.importCatalogFromJson(json): Result<Int>
        → Validate → Transaction (delete all + insert all)
```

### Flujo de Modificación
```
ConfigurationScreen (botón "Modificar JSON")
  → ConfigurationViewModel.openJsonEditor()
    → CatalogJsonRepository.exportCatalogToJson(): String
      → Mostrar en TextField editable
    → Usuario edita → "Aplicar"
      → CatalogJsonRepository.importCatalogFromJson(editedJson)
```

## Components and Interfaces

### CatalogJsonRepository
```kotlin
class CatalogJsonRepository(
    private val database: AppDatabase,
    private val menuItemDao: MenuItemDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val groupDao: CustomizationGroupDao,
    private val optionDao: CustomizationOptionDao
) {
    suspend fun exportCatalogToJson(): String
    suspend fun importCatalogFromJson(jsonString: String): Result<Int>
    private fun validate(export: CatalogExport): Result<Unit>
}
```

### DTOs de Serialización (package `data.json`)
```kotlin
@Serializable data class CatalogExport(version, exportedAt, catalog)
@Serializable data class CatalogData(menuItems)
@Serializable data class MenuItemDto(id, emoji, name, categories)
@Serializable data class CategoryDto(id, name, products)
@Serializable data class ProductDto(id, emoji, name, description, basePrice, isActive, customizationGroups)
@Serializable data class CustomizationGroupDto(id, groupName, selectionType, options)
@Serializable data class CustomizationOptionDto(id, optionName, extraPrice)
```

### ConfigurationViewModel (extensiones)
- Nuevos estados: `isExporting`, `isImporting`, `showJsonEditor`, `jsonEditorContent`, `jsonEditorError`, `showImportConfirmDialog`, `importPendingJson`, `toastMessage`
- Nuevas funciones: `exportCatalog()`, `onExportUriReceived()`, `importCatalogFromUri()`, `confirmImport()`, `openJsonEditor()`, `applyJsonEditorChanges()`, etc.

### UI Composables nuevos
- `ImportConfirmDialog` — Confirmación antes de importar
- `JsonEditorDialog` — Editor JSON in-app con TextField monospace

## Data Models

### Esquema JSON del Catálogo

```json
{
  "version": 1,
  "exportedAt": "2026-07-29T14:30:00Z",
  "catalog": {
    "menuItems": [
      {
        "id": "uuid-string",
        "emoji": "🌮",
        "name": "Tacos",
        "categories": [
          {
            "id": "uuid-string",
            "name": "Tacos",
            "products": [
              {
                "id": "uuid-string",
                "emoji": "🌮",
                "name": "Taco de Bistec",
                "description": "",
                "basePrice": 16.0,
                "isActive": true,
                "customizationGroups": [
                  {
                    "id": "uuid-string",
                    "groupName": "Remover",
                    "selectionType": "multiple_checkboxes",
                    "options": [
                      {
                        "id": "uuid-string",
                        "optionName": "Sin cilantro",
                        "extraPrice": 0.0
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### Mapeo JSON ↔ Room Entities

Las relaciones FK se infieren del anidamiento:
- `Category.associatedMenuId` ← se asigna del `MenuItem` padre
- `Product.categoryId` ← se asigna de la `Category` padre
- `CustomizationGroup.productId` ← se asigna del `Product` padre
- `CustomizationOption.groupId` ← se asigna del `CustomizationGroup` padre

### Campos del Esquema

| Nivel | Campo | Tipo | Requerido |
|-------|-------|------|-----------|
| Root | `version` | Int | Sí |
| Root | `exportedAt` | String (ISO-8601) | Sí |
| MenuItem | `id` | String (UUID) | Sí |
| MenuItem | `emoji` | String | Sí |
| MenuItem | `name` | String | Sí |
| MenuItem | `categories` | Array | Sí |
| Category | `id` | String (UUID) | Sí |
| Category | `name` | String | Sí |
| Category | `products` | Array | Sí |
| Product | `id` | String (UUID) | Sí |
| Product | `emoji` | String | Sí |
| Product | `name` | String | Sí |
| Product | `description` | String | Sí |
| Product | `basePrice` | Double (≥ 0) | Sí |
| Product | `isActive` | Boolean | Sí |
| Product | `customizationGroups` | Array | Sí |
| CustomizationGroup | `id` | String (UUID) | Sí |
| CustomizationGroup | `groupName` | String | Sí |
| CustomizationGroup | `selectionType` | String | Sí |
| CustomizationGroup | `options` | Array | Sí |
| CustomizationOption | `id` | String (UUID) | Sí |
| CustomizationOption | `optionName` | String | Sí |
| CustomizationOption | `extraPrice` | Double (≥ 0) | Sí |

## Error Handling

| Escenario | Acción |
|-----------|--------|
| JSON malformado (parse error) | Mostrar error, no modificar DB |
| `version` != 1 | Mostrar "Versión de esquema no soportada" |
| `selectionType` no reconocido | Mostrar error de validación |
| `basePrice` < 0 o `extraPrice` < 0 | Mostrar error de validación |
| IDs duplicados | Mostrar error con la entidad duplicada |
| Fallo en transacción DB | Rollback automático, DB intacta |
| Usuario cancela SAF picker | No action, sin error |
| Archivo vacío o ilegible | Error "No se pudo leer el archivo" |

## Testing Strategy

- Unit tests para `CatalogJsonRepository.exportCatalogToJson()` con datos mock
- Unit tests para `CatalogJsonRepository.importCatalogFromJson()` con JSON válido e inválido
- Unit tests para la función `validate()` con casos edge (IDs duplicados, precios negativos, selectionType inválido)
- Integration tests con Room in-memory DB para verificar el ciclo export → import roundtrip
