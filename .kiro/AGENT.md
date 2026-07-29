# AGENT.md — PuntoDeVenta

> **Mapa de contexto maestro.** Este archivo describe el estado **real** del proyecto (no el planificado).
> Última actualización: 2026-07-29 · Rama: `master` · Specs indexadas: 21 · Specs completas: 21/21
> Estado local: soporte **multiimpresora LAN + discovery** implementado, todavía sin push y pendiente de validación física con impresoras reales.

---

## 1. Resumen del Proyecto

**PuntoDeVenta** es una aplicación Android nativa de Punto de Venta (POS) para un negocio de comida
("LOS TACOS"), pensada principalmente para **tablet en horizontal** y con soporte de teléfono en vertical.

Flujo funcional completo hoy:

1. **Inicio** — el operador crea "menús" (tarjetas con emoji + nombre) y toca uno para entrar al POS.
2. **POS** — catálogo filtrable por menú / categoría / búsqueda, modal de producto con personalizaciones
   y notas, carrito editable con divisor de cuenta ("tijeras").
3. **Checkout** — nombre de cliente, estado de pago, teclado de denominaciones, asistente de cambio y
   modal de confirmación.
4. **Impresión** — genera ticket cliente + interno y los envía secuencialmente por **ESC/POS/TCP** a
   todas las impresoras LAN activas, respetando puerto, papel 58/80 mm y autocorte por destino; los
   reintentos omiten impresoras ya exitosas y la orden solo se guarda cuando todas terminan.
5. **Persistencia** — la orden se guarda en Room con el texto exacto de ambos tickets.
6. **Consulta** — pantallas de Estadísticas (ingresos, órdenes, ticket promedio, top productos) e
   Historial de Tickets con reimpresión del ticket cliente a todas las impresoras activas.
7. **Configuración** — CRUD de categorías/productos, **importar/exportar/modificar catálogo vía JSON**,
   gestión multiimpresora con activación individual y discovery LAN, y selector de tema.

### Stack tecnológico (verificado en `app/build.gradle.kts` y `gradle/libs.versions.toml`)

| Área | Detalle |
|---|---|
| Lenguaje | Kotlin `2.2.10` (Java 11) |
| UI | Jetpack Compose (BOM `2026.02.01`) + Material 3 + `material-icons-extended` |
| Build | Gradle KTS + **version catalog** (`libs.*`), AGP `9.2.1`, KSP `2.2.10-2.0.2` |
| SDK | `minSdk 24` · `compileSdk 37` · `targetSdk 37` |
| Persistencia | Room `2.7.1` (KSP) + DataStore Preferences `1.1.4` (tema e impresoras) + SharedPreferences solo para migración legacy de impresoras |
| Serialización | kotlinx-serialization-json `1.8.1` (catálogo y lista de impresoras) |
| Lifecycle | lifecycle `2.11.0` (viewmodel-compose, runtime-compose), activity-compose `1.13.0` |
| Arquitectura | MVVM + UDF, `StateFlow` / `collectAsStateWithLifecycle()` |
| DI | **Manual** en `MainActivity` (sin Hilt ni Koin) |
| Navegación | **Sin Navigation Compose**: `mutableStateOf<NavDestination>` + `when` |
| Tests (JVM) | JUnit4 `4.13.2`, kotest `5.9.1` (property), MockK `1.13.14`, Turbine `1.2.0`, coroutines-test — `useJUnitPlatform()` |
| Tests (device) | androidx-junit `1.3.0`, espresso `3.7.0`, `room-testing`, `compose-ui-test-junit4` |
| Red | Sockets TCP crudos; permisos `INTERNET`, `ACCESS_NETWORK_STATE` y `ACCESS_LOCAL_NETWORK` (runtime desde API 37). Sin Retrofit/OkHttp, Bluetooth ni USB |

---

## 2. Arquitectura Real

### 2.1 Estructura de paquetes

```
app/src/main/java/com/example/puntodeventa/
├── MainActivity.kt              # Único Activity: DI manual + navegación por estado
├── data/
│   ├── local/                   # Room: AppDatabase, DatabaseSeeder, SeedCallback(muerto),
│   │                            #       SelectionType, 8 *Entity, 6 *Dao
│   ├── json/                    # DTOs @Serializable para import/export de catálogo JSON
│   ├── model/                   # Modelos de dominio/proyección: MenuItem, Category, Product,
│   │                            #       ProductSaleSummary, PrinterConfig (8 campos)
│   ├── printer/                 # EscPosPrinterLan + LanPrinterDiscovery
│   └── repository/              # Menu, Category, Product, Order, CatalogJson,
│                                #       PrinterPreferences (lista JSON en DataStore dedicado),
│                                #       ThemePreferences (DataStore)
└── ui/
    ├── navigation/              # NavDestination.kt (sealed), AppNavRail.kt
    ├── theme/                   # AppTheme, ThemeColors, Theme, Color, Type,
    │                            #       ThemeSelectorScreen, ThemeCard, ThemeViewModel
    ├── home/                    # HomeScreen, HomeViewModel, MenuItemCard, AddMenuCard,
    │                            #       AddMenuDialog, EmojiData
    ├── pos/                     # PosScreen, PosViewModel, CatalogPanel, CartPanel, CartItem,
    │                            #       CheckoutPanel, CheckoutState, CashKeypad, BillsGrid,
    │                            #       ChangeAssistant, ConfirmationModal, CategoryTabBar,
    │                            #       MenuFilterBar, ProductGrid, ProductModal,
    │                            #       SearchTextField, PosHelpers, TicketFormatter
    ├── configuration/           # ConfigurationScreen, ConfigurationViewModel, ProductCard
    ├── newproduct/              # NewProductModal, NewProductViewModel, GroupCard,
    │                            #       EmojiPicker, SelectionTypeDropdown
    ├── stats/                   # StatsScreen, StatsViewModel, StatsUiState, StatsFormatters, TimeFilter
    ├── tickets/                 # TicketsScreen(→TicketHistoryScreen), TicketHistoryViewModel,
    │                            #       TicketHistoryUiState, TicketCard, TicketHistoryTopBar
    ├── printer/                 # Pantalla/config multiimpresora: bottom sheet, formulario, switches,
    │                            #       discovery, ControlPanel, StatusPanel, ViewModel/UiState
    └── settings/                # SettingsScreen.kt (placeholder huérfano, sin ruta)
```

**No existen** los paquetes `domain/`, `di/`, `util/` ni `data/preferences/`. Tampoco hay clase
`Application` propia (el manifest no declara `android:name`).

### 2.2 Base de datos (Room)

`data/local/AppDatabase.kt` — `punto_de_venta_db`, **version = 4**, `exportSchema = false`,
singleton `@Volatile INSTANCE` + `synchronized` vía `getInstance(context)`.

Jerarquía de entidades (todas las FK con `onDelete = CASCADE` e `Index` en la columna FK):

```
menu_items (MenuItemEntity)
└── categories.associatedMenuId
    └── products.categoryId
        └── customization_groups.productId
            └── customization_options.groupId

orders (OrderEntity)                     # id, timestamp, totalAmount, status:String,
└── order_items.orderId                  # customerName?, clientTicketText?, internalTicketText?
    └── order_item_customizations.orderItemId
```

- **DAOs (6):** `MenuItemDao`, `CategoryDao`, `ProductDao`, `CustomizationGroupDao`,
  `CustomizationOptionDao`, `OrderDao`. No hay DAO propio para `order_items` /
  `order_item_customizations`: sus inserts viven en `OrderDao`.
- **Migraciones:** ninguna. Se usa `fallbackToDestructiveMigration(dropAllTables = true)`.
- **TypeConverters:** ninguno. `SelectionType` se serializa a mano (`value` / `fromValue`).
- **FK enforcement:** `Callback.onOpen { PRAGMA foreign_keys = ON }`.
- **Seeder:** `DatabaseSeeder.seedIfEmpty(db)` con IDs deterministas
  (`UUID.nameUUIDFromBytes`), invocado desde `getInstance` con `runBlocking(Dispatchers.IO)`.

### 2.3 Inyección de dependencias

Todo se construye a mano en `MainActivity.onCreate`: `AppDatabase.getInstance(this)` y luego los seis
repositorios. Los ViewModels se obtienen en Composables con
`viewModel(factory = XViewModel.Factory(...))`; **cada ViewModel declara su propia
`class Factory : ViewModelProvider.Factory`**. Ese es el patrón a replicar para cualquier ViewModel nuevo.

### 2.4 Navegación

`ui/navigation/NavDestination.kt` es una `sealed class` con 7 objetos:
`Home`, `Pos`, `Stats`, `Settings`, `Tickets`, `Printer`, `Appearance`.
`MainActivity` mantiene `var currentDestination by remember { mutableStateOf(NavDestination.Home) }` y
un `when` que renderiza la pantalla. `AppNavRail` es el rail izquierdo persistente.
Los `route` existen pero **no se usan** (no hay `NavHost`, ni back stack, ni deep links).
Ojo: `NavDestination.Settings` renderiza `ConfigurationScreen`, no `SettingsScreen`.

### 2.5 Impresión

- `data/model/PrinterConfig.kt`: configuración por destino con 8 campos (`id`, `name`, `ipAddress`,
  `port`, `paperSize`, `autoCut`, `protocol`, `isActive`). Defaults: 9100, 80 mm, autocorte,
  `ESC/POS`, activa.
- `PrinterPreferencesRepository`: la fuente de verdad es una lista JSON en el Preferences DataStore
  dedicado `printer_preferences` (clave `printers_json`). En el primer acceso sin datos migra desde
  `SharedPreferences` legacy `printer_config`: prioriza `printers_json`; si falta o está corrupto,
  usa `ip_address` o `192.168.1.248` y crea la impresora estable `default-printer`. JSON corrupto en
  DataStore también recupera esa impresora inicial. Conserva las APIs síncronas y legacy de IP solo
  por compatibilidad; todas operan sobre la colección DataStore.
- UI multiimpresora: `ModalBottomSheet` de guardadas, selección para editar, switches de activación y
  alta nueva. El formulario cubre nombre, IPv4, puerto, papel 58/80, autocorte y protocolo; defaults
  de alta: `Nueva impresora`, IP vacía, 9100, 80 mm, autocorte, ESC/POS y activa.
- Discovery: `LanPrinterDiscovery` escanea la `/24` de una IPv4 privada activa, hosts 1..254 (excepto
  la IP local), en el puerto del borrador/default 9100, con 32 probes concurrentes y timeout 250 ms.
  Solo detecta puertos TCP abiertos: **no garantiza ESC/POS ni que el host sea una impresora**.
  `ACCESS_LOCAL_NETWORK` se solicita en runtime desde API 37; el manifest también declara
  `INTERNET` y `ACCESS_NETWORK_STATE`.
- Complete Order obtiene únicamente impresoras activas y bloquea si no hay ninguna. Imprime
  secuencialmente a cada activa; `printOrder()` abre una conexión por impresora para sus dos tickets
  (cliente + interno), usando `port`, `paperSize` (80 = 48 columnas; 58 = 32), `autoCut` y `protocol`.
  Los IDs exitosos se conservan durante el checkout para que un reintento procese solo las fallidas.
  Room se ejecuta únicamente después del éxito de todas.
- Historial reimprime solo el ticket cliente a **todas** las impresoras activas, secuencialmente y con
  la configuración de cada destino.
- `EscPosPrinterLan` sigue siendo `object`: TCP crudo, connect/read timeout 5 s, timeout global 15 s,
  `Dispatchers.IO` y **Cp850**. Solo admite protocolo ESC/POS; usa `ESC @`, corte `GS V 0` condicional
  y doble altura para los items del ticket interno.
- `TicketFormatter` permanece puro: base de 48 columnas, IVA 16 %, efectivo/cambio,
  personalizaciones, notas y divisores. Folio = `(orderCount + 1).padStart(3, '0')`.
- Documento detallado: **`.kiro/steering/printer_architecture.md`**.

### 2.6 Theming

Motor de temas dinámico y persistido, **completamente integrado** en toda la UI:

- `ui/theme/AppTheme.kt` — `enum AppTheme { DEFAULT_GREEN, DARK_NEON, OCEAN_BLUE, SUNSET_ORANGE }`.
- `ui/theme/ThemeColors.kt` — `AppTheme.toColorScheme()` con `when` exhaustivo. Incluye todos los roles
  de Material3: primary, secondary, tertiary, surface, surfaceVariant, outline, error, y sus variantes
  on*/container.
- `ui/theme/Theme.kt` — `PuntoDeVentaTheme(appTheme, content)`.
- Persistencia en **DataStore** (`themeDataStore`, clave `selected_theme`) vía `ThemePreferencesRepository`.
- **Todos los componentes** usan `MaterialTheme.colorScheme` — el cambio de tema se refleja en tiempo
  real sin reiniciar la app. `Color.kt` permanece como documentación/referencia histórica.

---

## 3. Índice de Specs (`.kiro/specs/`)

Cada carpeta contiene `requirements.md` (o `bugfix.md`), `design.md`, `tasks.md` y `.config.kiro`
(`{ specId, workflowType, specType }`). El estado se deriva de los checkboxes de `tasks.md`.

**⚠️ Regla dura: estas carpetas son de solo lectura para tareas de documentación. No fusionar,
renumerar, mover ni borrar specs.**

> Faltan los números `02` y `09`. Por contenido y dependencias, `local-data-persistence` ocupa el
> hueco 02 y `pos-main-screen` el hueco 09. `statistics-dashboard` y `printer-audit-and-ticket-format`
> son posteriores a la numeración. Es una inferencia, no está declarado en los archivos.

### Specs numeradas

| # | Spec | Tareas | Propósito |
|---|---|---|---|
| 01 | `01_main_menu` | **58/58 ✅** | Estructura base de la pantalla principal: fondo verde a pantalla completa, paleta única en `Color.kt`, NavRail persistente de 5 destinos y tarjeta "+" para crear menús. v1.2 añade borrado desde el modal de edición. |
| 03 | `03_products_database` | **38/38 ✅** | Fase 1 del catálogo relacional, **solo capa de datos**: entidades `Category`, `Product`, `CustomizationGroup`, `CustomizationOption` con FKs, sus DAOs, migración de `AppDatabase` a v2 y repositorios con `Flow`. Sin UI. |
| 04 | `04_configuration_screen_ui` | **20/20 ✅** | Fase 2: `ConfigurationScreen` con tabs de categoría, `ProductCard` con toggle `isActive` y menú contextual (Editar/Duplicar/Eliminar). Import/export JSON queda fuera de alcance. |
| 05 | `05_new_product_modal` | **32/32 ✅** | `ModalBottomSheet` "Nuevo Producto": emoji, nombre, descripción, precio, asignación menú/categoría y N grupos de personalización con N opciones. Persiste el árbol completo en **una sola transacción Room**. |
| 06 | `06_bugfixes_config` | **38/38 ✅** | Cuatro bugs de las fases 2–3 (usa `bugfix.md`, sin `requirements.md`): parpadeo del modal por recomposición excesiva, botón "Editar" inerte, duplicado con IDs compartidos y espaciado del top bar. Un commit por bug. |
| 07 | `07_category_improvements` | **17/17 ✅** | Dos mejoras a `ConfigurationScreen`: `ORDER BY name ASC` en `getProductsByCategory` para orden determinístico al togglear `isActive`, y borrado de categoría con `AlertDialog` de confirmación. |
| 08 | `08_printer_config_ui` | **42/42 ✅** | Solo UI y ruteo de la pantalla de impresora POS-8360: layout de dos columnas (Control Panel verde oscuro / Status Panel gris), campo de IP, botones Probar y Guardar. Sin lógica de red. |
| 10 | `10_checkout_and_print` | **33/33 ✅** | Flujo de cobro completo: `CheckoutPanel` que reemplaza al catálogo, nombre de cliente, estado de pago, teclado de denominaciones, cálculo de cambio, generación de ticket cliente + interno, impresión LAN y persistencia con `clientTicketText`/`internalTicketText`. Incluye `bugfix_checkout_crash.md`. |
| 11 | `11_database_seeding` | **19/19 ✅** | `DatabaseSeeder`: detecta `menu_items` vacío y siembra menú, categorías, productos y personalizaciones por defecto en una transacción atómica, en orden FK-safe, antes de exponer la DB. |
| 12 | `12_lan_printer_connection` | **16/16 ✅** | Conexión real ESC/POS por TCP: permiso `INTERNET`, IP por defecto `192.168.1.248`, connect timeout con `InetSocketAddress`, encoding CP850 y test de impresión cableado hasta la UI. |
| 13 | `13_ticket_printing_fixes` | **11/11 ✅** | Tres bugs de impresión (usa `bugfix.md`): folio UUID en vez de secuencial `001`, ticket interno nunca enviado a la impresora, y desalineación del formato respecto a las plantillas (prefijos `Fecha:`/`Estado:`, separadores, columna `IMPORTE`). |
| 14 | `14_cart_edit_and_ticket_options` | **23/23 ✅** | `TicketLineItem.customizations` para imprimir personalizaciones indentadas (`"      - {opción}"`) en ambos tickets, y edición in-place de ítems del carrito (`Edit_Mode`, botón "Actualizar"). |
| 15 | `15_ticket_history` | **17/17 ✅** | Pantalla "Historial de Tickets" que sustituye el placeholder de la ruta `tickets`: query por rango de timestamps, `TimeFilter` (Hoy/Ayer/Este mes/Todo), tarjetas con estilo de recibo monoespaciado y reimpresión. |
| 16 | `16_sprint_correcciones` | **37/37 ✅** | Sprint UX de tres frentes: `MenuFilterBar` + búsqueda en el POS, divisor de orden con tijeras (`isDivider`) e impresión de línea separadora con doble altura, y rediseño del checkout estilo "calculadora/asistente" en tema claro. |
| 17 | `17_ux_polish_sprint` | **27/27 ✅** | Pulido UX: navegación directa Home → POS aplicando el `menuId` tocado, lógica anti-spam del botón de tijeras, limpieza del `CheckoutPanel` y estado visual "glow" en los botones de completar orden. |
| 18 | `18_theme_engine` | **25/25 ✅** | Motor de temas dinámico: enum `AppTheme` de 4 temas, persistencia en Preferences DataStore con fallback a `DEFAULT_GREEN`, `ThemeViewModel` reactivo, `ThemeSelectorScreen` en cuadrícula y migración completa de todos los componentes a `MaterialTheme.colorScheme`. Sin reinicio de app. |
| 19 | `19_json_management` | **10/10 ✅** | Gestión de catálogo vía JSON: exportar a archivo (SAF `CreateDocument`), importar desde archivo (SAF `OpenDocument` con validación y replace-all transaccional), y editor JSON in-app con `TextField` monospace. DTOs con `kotlinx.serialization`, `CatalogJsonRepository` para la lógica y diálogos de confirmación. |

### Specs con nombre (previas a la numeración)

| Spec | Tareas | Propósito |
|---|---|---|
| `local-data-persistence` | **24/24 ✅** | Introduce Room para que los menús sobrevivan al cierre de la app: `MenuItemEntity`, `MenuItemDao`, `AppDatabase` v1, `MenuRepository` y `HomeViewModel` recableado con `ViewModelFactory`. Es la base histórica de toda la capa de datos. |
| `pos-main-screen` | **38/38 ✅** | Pantalla POS de dos paneles (catálogo 70 % / carrito 30 %), tabs de categoría con pestaña TODO, `ProductModal` con cantidad, personalizaciones y notas, y esquema de órdenes (`OrderEntity`, `OrderItemEntity`, `OrderItemCustomizationEntity`) con `AppDatabase` v3 y cascadas. |
| `printer-audit-and-ticket-format` | **28/28 ✅** | Dos entregables: auditoría documentada del flujo de impresión (hoy mantenida en **`.kiro/steering/printer_architecture.md`**) y corrección del ticket de cliente para incluir efectivo/cambio y las `extraNotes` bajo cada producto. |
| `statistics-dashboard` | **29/29 ✅** | Dashboard "Estadísticas" sobre el historial de órdenes: `ProductSaleSummary` como proyección sin `@Entity`, queries agregadas por rango (`SUM`, `COUNT`, `COUNT DISTINCT customerName`, top 50 productos) y `Time_Filter` de cuatro opciones. |

> **Estado fuera del índice de specs:** la evolución reciente a multiimpresora y discovery LAN está
> implementada en el código local, pero no existe una carpeta/spec numerada nueva para ella. Por eso
> se conservan sin cambios los conteos **21 specs indexadas / 21 completas**; no atribuir este trabajo
> a las specs históricas 08 o 12 ni inventar una spec hasta que exista su carpeta.

### Lectura del estado

- **✅ (x/x)** — todas las tareas marcadas. La feature está implementada y verificada en el código.
- **🟡 (n/m)** — quedan tareas sin marcar. En la mayoría de casos la funcionalidad principal **sí está
  en el código** y lo pendiente son tareas de test, validación o pulido. Antes de retomar una spec 🟡,
  lee su `tasks.md` para ver exactamente qué quedó abierto: no asumas que la feature falta.

---

## 4. Reglas Generales y Patrones a Respetar

### 4.1 Proceso (spec-driven)

1. **Los specs son la fuente de verdad de requisitos.** Antes de tocar una feature, lee su
   `requirements.md` / `design.md` / `tasks.md`.
2. **No modifiques specs existentes** salvo petición explícita. Nada de fusionar, renumerar o borrar
   carpetas de `.kiro/specs/`.
3. Al implementar, marca la tarea en el `tasks.md` correspondiente y **referencia el requisito en un
   comentario** con el formato ya usado en el código: `// (Req 3.2)`.
4. Los sprints de corrección usan `bugfix.md` en lugar de `requirements.md` y se resuelven
   **un commit por bug**.

### 4.2 Capa de datos

- Lecturas de DAO **reactivas** con `Flow<List<...>>`; escrituras `suspend`.
  Variantes puntuales con sufijo `...Once()` (`suspend`, no `Flow`).
- `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
- Operaciones multi-tabla siempre dentro de `database.withTransaction { }`.
- Toda query que alimente una lista debe llevar **`ORDER BY` explícito** con desempate estable
  (lección de la spec 07: sin él, Room reordena la UI en cada emisión).
- Los repositorios son **clases concretas sin interfaz**, wrappers finos del DAO que mapean
  Entity → modelo de `data/model`.
- IDs de dominio: `String` con `UUID.randomUUID().toString()`. En el seeder, IDs **deterministas**
  con `UUID.nameUUIDFromBytes`.
- Si añades una entidad o cambias el esquema: **sube `version` en `AppDatabase`**. Hoy hay
  `fallbackToDestructiveMigration`, así que cualquier bump **borra los datos del dispositivo**.
  Avisa de esto antes de hacerlo.

### 4.3 Capa de presentación

- **MVVM + UDF.** Un `XUiState` inmutable (`data class` con valores por defecto) por pantalla,
  expuesto como `StateFlow` y consumido con `collectAsStateWithLifecycle()`.
- Composables **stateless**: reciben estado + lambdas `onXxx`. Los eventos son llamadas a métodos del
  ViewModel, no una jerarquía sellada de intents.
- Un Composable por archivo para componentes reutilizables (`ProductCard.kt`, `StatusInfoRow.kt`).
- Nomenclatura fija: `XEntity.kt`, `XDao.kt`, `XRepository.kt`, `XScreen.kt`, `XViewModel.kt`,
  `XUiState.kt`.
- Cada ViewModel expone su propia `class Factory(...) : ViewModelProvider.Factory`.
- Errores: se capturan con `try/catch` o `runCatching` y se publican en el `UiState`
  (`errorMessage`). No se propagan excepciones a la UI ni existe un `Result<T>` propio;
  para resultados de guardado se usa `sealed interface SaveResult { Success | Failure(message) }`.
- Comentarios de sección con el estilo `// ── Título ──────────`.

### 4.4 Impresión y tickets

- `TicketFormatter` debe permanecer **puro y testeable** (sin Android, sin I/O). Su formato base es
  de **48 columnas** para 80 mm; la adaptación a **32 columnas** para 58 mm pertenece al printer.
- La fuente de verdad es `PrinterPreferencesRepository.getPrinters()` (lista JSON). Los flujos de
  orden y reimpresión operan solo sobre `isActive`; nunca vuelvas a asumir una IP única.
- Complete Order imprime secuencialmente y abre **una conexión por impresora para los dos tickets**.
  Respeta `port`, `paperSize`, `autoCut` y `protocol`, registra IDs exitosos y en reintentos procesa
  solo destinos fallidos. Si no hay activas o alguna sigue fallando, **no se persiste la orden**.
- Historial reimprime el ticket cliente a todas las activas; no imprime el interno.
- Envío de bytes siempre por `EscPosPrinterLan`, con **Cp850**, connect/read timeout de 5 s y timeout
  global de 15 s. El discovery usa el puerto del borrador y solo prueba apertura TCP: no lo presentes
  como identificación segura de impresoras ESC/POS.
- Errores de Complete Order: hasta **3 intentos** (`printAttempts`); el carrito se conserva.
- Antes de tocar el flujo, lee `.kiro/steering/printer_architecture.md`. La validación física de la
  implementación multiimpresora/discovery continúa pendiente.

### 4.5 Colores y tema

- **Toda la UI** usa `MaterialTheme.colorScheme` para que los 4 temas se propaguen dinámicamente.
- No introduzcas literales `Color(0x...)` en Composables. Si necesitas un token de marca fijo,
  sácalo de `ThemeColors.kt` mediante un color role del ColorScheme.
- Al añadir un tema, el `when` de `AppTheme.toColorScheme()` es exhaustivo: el compilador te obligará
  a cubrirlo (así está diseñado a propósito, Req 1.3 de la spec 18).
- `Color.kt` permanece como referencia histórica pero **no se importa** desde archivos de producción.

### 4.6 Testing

- `app/src/test/` (JVM, ~40 archivos): lógica pura, formatters y ViewModels. Frameworks:
  **kotest-property** (mayoría), JUnit, MockK, Turbine, coroutines-test.
  El módulo usa `useJUnitPlatform()`, así que kotest (JUnit5) y JUnit4 conviven.
- `app/src/androidTest/` (~34 archivos): DAOs y Room (`room-testing`), y UI con `compose-ui-test`.
- Sufijos en uso: `...PropertyTest` (property-based), `...UnitTest`, `...Test`, y por bug
  `...BugConditionTest` / `...PreservationTest`.
- Los specs definen propiedades de corrección explícitas; cúbrelas con property tests, no solo
  con casos de ejemplo.

### 4.7 Build y comandos (Windows / PowerShell)

```powershell
.\gradlew.bat assembleDebug              # APK debug
.\gradlew.bat testDebugUnitTest          # tests JVM
.\gradlew.bat connectedDebugAndroidTest  # tests instrumentados (requiere dispositivo)
```

- Toda dependencia nueva se declara **en el version catalog** (`gradle/libs.versions.toml`) y se
  referencia como `libs.*`. No pongas coordenadas literales en `build.gradle.kts`.
- Room usa **KSP**, no kapt.

---

## 5. Deuda Técnica Conocida

Contexto para no "arreglar" cosas que son decisiones conscientes, y para saber dónde están las minas.

| # | Tema | Detalle |
|---|---|---|
| 1 | DI manual | Repos y DB se instancian por Activity. `PosScreen` recibe **DAOs de Room directamente** y `NewProductViewModel.Factory` recibe el `AppDatabase` completo: la UI está acoplada a la persistencia. |
| 2 | `runBlocking` en `getInstance` | El seeder se ejecuta con `runBlocking(Dispatchers.IO)` desde `MainActivity.onCreate`, es decir bloqueando el main thread. |
| 3 | Migraciones inexistentes | `fallbackToDestructiveMigration(dropAllTables = true)` en v4 y `exportSchema = false`: cada bump de versión borra los datos y no hay validación de esquema. |
| 4 | Dos DataStores/formato distinto | Impresora y tema usan **Preferences DataStore** dedicados, pero impresora conserva API síncrona y serializa una lista JSON, mientras tema expone `Flow` y guarda un enum simple. `SharedPreferences printer_config` queda solo como origen de migración legacy. |
| 5 | ~~Theming a medias~~ | **Resuelto.** Todos los componentes (`ui/pos`, `ui/printer`, `ui/newproduct`, `ui/home`, `AppNavRail`) ahora usan `MaterialTheme.colorScheme` y reaccionan dinámicamente al cambio de tema sin reinicio de app. `Color.kt` permanece como referencia histórica pero ya no se importa desde producción. |
| 6 | Navegación frágil | Sin Navigation Compose: el destino es `mutableStateOf` (no `rememberSaveable`), sin back stack; los `route` son decorativos. `NavDestination.Settings` renderiza `ConfigurationScreen`. |
| 7 | Código muerto | `data/local/SeedCallback.kt` y `ui/settings/SettingsScreen.kt` no se referencian desde ningún sitio. |
| 8 | Impresión no mockeable | `object EscPosPrinterLan` se llama estáticamente desde `PosViewModel` y `TicketHistoryViewModel`; no hay interfaz que permita sustituirlo en test JVM. `TicketFormatter` vive en `ui/pos` en lugar de una capa de datos/dominio. |
| 9 | Sin capa `domain` | La lógica de negocio (IVA 16 % hardcodeado, totales, mapeo carrito → orden) vive repartida entre `PosViewModel` y `TicketFormatter`. |
| 10 | `PosViewModel` monolítico | ~750 líneas y ~14 `MutableStateFlow` internos, con `@OptIn(ExperimentalCoroutinesApi, FlowPreview)`. |
| 11 | Tests inconsistentes | `PrinterConfigViewModelTest` está **duplicado** en `test/` y `androidTest/`; `androidTest/assets/printer_sources/` contiene **copias de fuentes de producción** que se desincronizarán; hay property tests instrumentados que en realidad son puros. |
| 12 | Build | `compileSdk`/`targetSdk` 37 (preview) con `minSdk 24`; release sin minify ni firma; `kotlinx-coroutines-core` no se declara (llega transitivamente). |
| 13 | `OrderEntity.status` | Es un `String` libre; los valores válidos (`COMPLETED`, `CANCELLED`, `REFUNDED`) solo están documentados en comentarios, sin enum ni TypeConverter. |

---

## 6. Business Rules / Contextual UI

| # | Regla | Detalle |
|---|---|---|
| 1 | Botón TOTAL con comportamiento dual | El botón TOTAL (en `CartPanel`) tiene doble función: en la vista de catálogo (checkout oculto), abre el panel de checkout (`showCheckout()`); dentro del panel de checkout, si las condiciones de pago se cumplen (`isCompletarOrdenEnabled == true`), funciona como atajo para completar la orden (`showConfirmationModal()`). |
| 2 | Nombre de cliente permite espacios | El campo "Nombre del cliente" en el `CheckoutPanel` acepta espacios internos (para apellidos). La validación de "no vacío" se hace con `.trim().isEmpty()` solo al evaluar `isCompletarOrdenEnabled`, no al almacenar el valor. |
| 3 | Filtrado de fechas con rangos rápidos y personalizados | El filtrado de fechas en toda la app (Estadísticas e Historial de Tickets) soporta tanto rangos rápidos predefinidos (Hoy, Ayer, Este mes, Todo) como rangos personalizados de timestamps mediante un DateRangePicker de Material 3. El usuario accede al rango personalizado tocando la pill "📅 Rango" en el selector de filtros. Al seleccionar un rango, el `endMillis` se ajusta al final del día (23:59:59.999) para incluir todas las órdenes del último día seleccionado. El `TimeFilter.CUSTOM` se maneja de forma especial: no pasa por `computeRange()` sino que usa los timestamps explícitos almacenados en el UiState. |
