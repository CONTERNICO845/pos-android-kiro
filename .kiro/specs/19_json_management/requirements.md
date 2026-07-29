# Requirements Document

## Introduction

Este documento define los requisitos funcionales para las tres operaciones de gestión de catálogo vía JSON en la pantalla de Configuración del POS: Exportar JSON, Importar JSON, y Modificar JSON. Los botones ya existen en la UI pero actualmente solo ejecutan `Log.d()`.

## Glossary

- **Catálogo**: Conjunto completo de MenuItems, Categories, Products, CustomizationGroups, y CustomizationOptions en la base de datos.
- **SAF**: Storage Access Framework de Android, para acceder al sistema de archivos del usuario.
- **Replace-All**: Estrategia de importación que borra todo el catálogo y lo reinserta desde el JSON.
- **DTO**: Data Transfer Object, clases intermedias para serialización/deserialización.

## Requirements

### REQ-EXP: Exportar JSON

- REQ-EXP-01: El botón "Exportar JSON" en `ActionBarRow` invoca un evento en `ConfigurationViewModel` que inicia el flujo de exportación.
- REQ-EXP-02: El ViewModel solicita a `CatalogJsonRepository` una lectura jerárquica completa de la DB (MenuItems → Categories → Products → Groups → Options) y serializa a JSON con pretty-print.
- REQ-EXP-03: Se usa `ActivityResultContracts.CreateDocument("application/json")` para abrir el selector SAF. Nombre sugerido: `catalogo_<yyyyMMdd_HHmmss>.json`.
- REQ-EXP-04: Una vez obtenida la Uri, se escribe el JSON string completo con encoding UTF-8 vía `contentResolver.openOutputStream(uri)`. Se muestra Toast de confirmación.
- REQ-EXP-05: Si el usuario cancela el selector no se hace nada. Si falla la escritura se muestra error. El botón se deshabilita durante la operación.

### REQ-IMP: Importar JSON

- REQ-IMP-01: El botón "Importar JSON" invoca un evento que abre `ActivityResultContracts.OpenDocument` con MIME `application/json`.
- REQ-IMP-02: Se lee el contenido como String UTF-8 y se deserializa al DTO `CatalogExport`.
- REQ-IMP-03: Se valida: parsing exitoso, `version == 1`, IDs no vacíos ni duplicados, `selectionType` válido, precios ≥ 0.
- REQ-IMP-04: Se muestra un `AlertDialog` de confirmación antes de aplicar: "Esto reemplazará TODO el catálogo actual. ¿Continuar?"
- REQ-IMP-05: Se ejecuta replace-all en una única transacción Room: DELETE all (options → groups → products → categories → menu_items) → INSERT all (menu_items → categories → products → groups → options).
- REQ-IMP-06: Éxito muestra Toast con count de productos. La UI se actualiza automáticamente via Flows reactivos.
- REQ-IMP-07: Las tablas de órdenes nunca se tocan durante import.

### REQ-MOD: Modificar JSON

- REQ-MOD-01: El botón "Modificar JSON" abre un modal con el catálogo actual serializado a JSON en un TextField editable monospace.
- REQ-MOD-02: Al presionar "Aplicar", se parsea el texto editado, se valida (misma lógica que import), y si es válido se muestra diálogo de confirmación.
- REQ-MOD-03: Si la validación falla, se muestra error inline sin cerrar el modal.
- REQ-MOD-04: Si confirma, se aplica la misma transacción replace-all que el import.

### REQ-CROSS: Requisitos Transversales

- REQ-CROSS-01: Todas las operaciones de DB se ejecutan en `Dispatchers.IO`. La UI muestra loading durante operaciones.
- REQ-CROSS-02: Las tablas `orders`, `order_items`, `order_item_customizations` nunca se modifican.
- REQ-CROSS-03: Se crea un nuevo `CatalogJsonRepository` que encapsula serialización y transacciones.
- REQ-CROSS-04: Se agrega `kotlinx.serialization` (plugin + dependencia) al proyecto.
- REQ-CROSS-05: Los `ActivityResultLauncher` se registran en el Composable (lifecycle-aware), no en el ViewModel.
