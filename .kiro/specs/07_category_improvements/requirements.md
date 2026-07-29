# Requirements Document

## Introduction

Este spec abarca dos mejoras independientes a la pantalla de configuración principal (`ConfigurationScreen`) de la aplicación POS (Punto de Venta) construida con Jetpack Compose.

1. **Corrección de ordenamiento determinístico de productos**: La consulta `getProductsByCategory` en `ProductDao` carece de cláusula `ORDER BY`, lo que provoca que la lista de productos se reordene visualmente cada vez que se emite un nuevo valor desde Room/Flow (por ejemplo, al alternar el Switch `isActive` de un producto). La corrección consiste en agregar `ORDER BY name ASC` a la consulta para garantizar un orden estable y predecible.

2. **Eliminación de categoría con confirmación**: Se agrega un botón de ícono de papelera en el TopBar de `ConfigurationScreen`, junto al selector de categorías. Al presionarlo, se muestra un `AlertDialog` de confirmación antes de ejecutar la eliminación. Si el usuario confirma, se invoca `CategoryRepository.deleteById()` y se limpia la selección de categoría activa.

## Glossary

- **ConfigurationScreen**: Pantalla principal de configuración del menú, compuesta por una fila de pestañas de categorías y una lista de productos.
- **CategoryTabsRow**: Componente Composable que renderiza las pestañas de categorías en la parte superior de `ConfigurationScreen`.
- **ProductDao**: Interfaz Room DAO que provee acceso a la tabla `products` de la base de datos local.
- **CategoryRepository**: Repositorio que actúa como capa de acceso a datos para las entidades `Category`, incluyendo el método `deleteById(id: String)`.
- **ConfigurationViewModel**: ViewModel que gestiona el estado de `ConfigurationScreen`, incluyendo la categoría seleccionada y la lista de productos.
- **AlertDialog**: Componente de Material 3 que muestra un diálogo modal de confirmación antes de ejecutar una acción destructiva.
- **Flow**: Tipo de Kotlin Coroutines que emite una secuencia de valores de forma reactiva; usado por Room para observar cambios en la base de datos.
- **isActive**: Campo booleano de `ProductEntity` que indica si un producto está habilitado en el menú.
- **selectedCategory**: Estado en `ConfigurationUiState` que representa la categoría actualmente seleccionada; puede ser `null` si no hay ninguna seleccionada.

---

## Requirements

### Requirement 1: Ordenamiento determinístico de la lista de productos

**User Story:** Como operador del POS, quiero que la lista de productos mantenga un orden estable y predecible al interactuar con ella, para que los ítems no salten de posición al activar o desactivar un producto.

#### Acceptance Criteria

1. THE lista de productos en `ConfigurationScreen` SHALL mostrarse siempre ordenada alfabéticamente en orden ascendente (insensible a mayúsculas/minúsculas) por nombre, independientemente de cuándo fue creado o modificado el producto.
2. WHEN el switch `isActive` de un producto es alternado, THE secuencia renderizada de todos los demás productos en la lista SHALL permanecer idéntica a la que existía antes del toggle; ningún ítem SHALL cambiar su posición visual relativa a los demás.
3. WHEN el usuario activa o desactiva un producto, THE `ConfigurationScreen` SHALL volver a renderizar la lista completa manteniendo el mismo orden alfabético ascendente por nombre que tenía antes de la operación.
4. IF dos productos pertenecientes a la misma categoría tienen exactamente el mismo nombre (comparación insensible a mayúsculas/minúsculas), THEN THE lista SHALL usar un criterio de desempate secundario estable (por ejemplo, `id` ascendente) para garantizar que su orden relativo sea siempre el mismo en emisiones sucesivas de Room.
5. IF la lista de productos de una categoría está vacía, THEN THE `ConfigurationScreen` SHALL mostrar un estado vacío sin errores ni reordenamientos.

---

### Requirement 2: Eliminación de categoría con confirmación

**User Story:** Como operador del POS, quiero poder eliminar una categoría y todos sus productos desde la pantalla de configuración, con un paso de confirmación explícito, para evitar eliminaciones accidentales de datos.

#### Acceptance Criteria

1. IF `selectedCategory` no es `null`, THEN THE `ConfigurationScreen` SHALL mostrar un botón de ícono de papelera (trash icon) interactuable en el TopBar, junto al selector de categorías.
2. IF `selectedCategory` es `null`, THEN THE `ConfigurationScreen` SHALL ocultar o deshabilitar el botón de ícono de papelera de modo que no sea interactuable ni visible para el usuario.
3. WHEN el usuario presiona el botón de ícono de papelera con una categoría seleccionada, THE `ConfigurationScreen` SHALL mostrar un `AlertDialog` sin ejecutar ninguna operación de eliminación en ese momento.
4. THE `AlertDialog` SHALL presentar el mensaje: "¿Estás seguro? Eliminar esta categoría eliminará permanentemente todos los productos dentro de ella."
5. THE `AlertDialog` SHALL presentar exactamente dos acciones: un botón de confirmación con la etiqueta "Eliminar" y un botón de cancelación con la etiqueta "Cancelar".
6. WHEN el usuario presiona el botón "Cancelar" del `AlertDialog`, THE `ConfigurationScreen` SHALL cerrar el diálogo y el estado de `selectedCategory` y la base de datos SHALL permanecer sin cambios.
7. WHEN el usuario presiona el botón "Eliminar" del `AlertDialog`, THE `ConfigurationViewModel` SHALL invocar `CategoryRepository.deleteById()` con el `id` de la categoría actualmente seleccionada.
8. WHEN `CategoryRepository.deleteById()` se completa exitosamente, THE `ConfigurationViewModel` SHALL establecer `selectedCategory` en `null` y THE `ConfigurationScreen` SHALL cerrar el `AlertDialog`.
9. WHEN `selectedCategory` se establece en `null` tras una eliminación exitosa y existen otras categorías en la lista, THE `ConfigurationScreen` SHALL mostrar automáticamente la primera categoría disponible seleccionada.
10. IF `CategoryRepository.deleteById()` lanza una excepción, THEN THE `ConfigurationViewModel` SHALL capturar la excepción, mantener `selectedCategory` con el valor que tenía antes del intento de eliminación, y establecer el estado de error en `ConfigurationUiState.error` con el mensaje de la excepción.
11. WHEN `CategoryRepository.deleteById()` lanza una excepción, THE `ConfigurationScreen` SHALL cerrar el `AlertDialog` y mostrar el mensaje de error al usuario.
12. WHEN la operación de eliminación falla y el error es mostrado al usuario, THE `ConfigurationScreen` SHALL permitir al usuario reintentar o cancelar sin necesidad de reiniciar la pantalla.
