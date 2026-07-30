# 🌮 Punto de Venta — POS Android

Aplicación Android nativa de **Punto de Venta** para un negocio de comida ("LOS TACOS"), diseñada principalmente para **tablet en orientación horizontal**, con soporte para teléfonos en vertical.

Incluye gestión de menús, catálogo de productos con personalizaciones, carrito de compras, checkout con asistente de cambio y selección de método de pago, impresión de tickets por red LAN a impresoras térmicas (multiimpresora), historial de tickets con reimpresión, **dashboard de estadísticas enterprise** (gráfica de ventas interactiva, comparación periodo-sobre-periodo, desglose por método de pago y exportación CSV) y un motor de temas dinámico con 9 esquemas de color.

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Capturas de Pantalla (Screens)](#-pantallas)
- [Arquitectura](#-arquitectura)
- [Stack Tecnológico](#-stack-tecnológico)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Sistema de Impresión](#-sistema-de-impresión)
- [Temas Disponibles](#-temas-disponibles)
- [Base de Datos](#-base-de-datos)
- [Requisitos](#-requisitos)
- [Instalación y Build](#-instalación-y-build)
- [Testing](#-testing)
- [Licencia](#-licencia)

---

## ✨ Características

### Gestión de Menús (Inicio)
- Crear, editar y eliminar menús con emoji + nombre
- Grid adaptativo con tarjeta "+" para agregar nuevos menús
- Navegación directa al POS al tocar un menú

### Punto de Venta (POS)
- Layout de dos paneles: **catálogo (70%)** + **carrito (30%)**
- Filtrado por menú, categoría y búsqueda de texto
- Modal de producto con cantidad, personalizaciones múltiples y notas
- Edición in-place de ítems del carrito
- Divisor de orden con "tijeras" para separar cuentas

### Checkout
- Panel de cobro estilo calculadora con nombre de cliente
- Teclado de denominaciones de billetes mexicanos
- Cálculo automático de cambio
- **Método de pago:** Efectivo, Tarjeta o Transferencia (persiste con la orden)
- Estado de pago (Pagado, No pagó, Paga después)
- Tarjeta y transferencia no requieren que el efectivo cubra el total
- Modal de confirmación antes de imprimir

### Impresión Térmica por LAN
- Conexión TCP directa a impresora **POS-8360** (puerto 9100)
- Ticket de cliente (con precios, IVA 16%, subtotal, pago y cambio)
- Ticket interno/cocina (sin precios, con doble altura en ítems)
- Personalizzaciones y notas impresas bajo cada producto
- Folio secuencial con zero-padding (`001`, `002`, ...)
- Hasta 3 reintentos automáticos en caso de fallo
- Encoding **Cp850** para caracteres latinos (acentos, eñes)

### Estadísticas (Enterprise Dashboard)
- **Gráfica de tendencia de ventas** interactiva (Canvas, sin librería externa)
  - Granularidad adaptativa: horas para Hoy/Ayer, días para Este Mes, meses para Todo
  - Toggle barras / línea
  - Tap para inspeccionar un bucket con tooltip de periodo y monto
- **Indicadores de comparación vs periodo anterior** en cada tarjeta de métrica
  - Flecha y porcentaje (`+5.0%`, `-2.3%`, `0.0%`, o "Nuevo" cuando no hay baseline)
  - Se ocultan en el filtro "Todo" (no hay periodo previo por definición)
- **Desglose de ingresos por método de pago** (Efectivo/Tarjeta/Transferencia)
  - Donut chart con arcos proporcionales a la participación de cada método
  - Leyenda con nombre, monto, % y número de órdenes
- **Exportación de reporte a CSV** vía Storage Access Framework
  - Resumen + comparación, ventas por método, tendencia y top productos
  - RFC 4180, UTF-8 BOM, números como decimales puros para spreadsheets
- Dashboard con ingresos totales, número de órdenes, ticket promedio, clientes únicos
- Top 50 productos más vendidos
- Órdenes recientes (últimas 20)
- Filtro temporal: Hoy / Ayer / Este Mes / Todo / Rango personalizado (DateRangePicker)

### Historial de Tickets
- Lista de órdenes con tarjetas estilo recibo monoespaciado
- Filtros por periodo de tiempo
- Reimpresión de cualquier ticket pasado

### Configuración de Productos
- CRUD de categorías y productos
- Tabs por categoría con búsqueda
- Toggle activo/inactivo por producto
- Grupos de personalización con N opciones (Single / Multi selección)
- Creación en modal bottom sheet con transacción atómica
- **Exportar catálogo a JSON** — guarda todo el catálogo (menús, categorías, productos, personalizaciones) en un archivo `.json` via Storage Access Framework
- **Importar catálogo desde JSON** — reemplaza todo el catálogo actual desde un archivo `.json` con validación completa y transacción atómica
- **Editor JSON in-app** — visualiza y edita el catálogo como JSON formateado directamente en la app

### Configuración de Impresora
- Layout dos columnas: Panel de Control + Panel de Status
- Campo editable de IP con validación
- Botón de prueba de conexión con feedback por Snackbar
- Specs técnicas de la POS-8360 visibles en panel de status

### Motor de Temas
- **9 temas dinámicos** (6 claros + 3 oscuros) que se aplican en tiempo real sin reiniciar
- Persistencia del tema seleccionado en DataStore
- Cuadrícula adaptable que escala las columnas al ancho de pantalla
- Contraste WCAG 2.1 ≥ 4.5:1 verificado en todos los pares texto/fondo
- Todos los componentes usan `MaterialTheme.colorScheme`

---

## 🖥️ Pantallas

| Pantalla | Ruta | Descripción |
|----------|------|-------------|
| Inicio | `home` | Grid de menús con tarjetas emoji |
| POS | `pos` | Catálogo + carrito + checkout |
| Estadísticas | `stats` | Dashboard de métricas de ventas |
| Configuración | `settings` | CRUD de categorías y productos |
| Tickets | `tickets` | Historial con reimpresión |
| Impresora | `printer` | Configuración de impresora LAN |
| Apariencia | `appearance` | Selector de tema visual |

Navegación mediante `NavigationRail` lateral persistente (7 destinos).

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  Composables (stateless) ← StateFlow ← ViewModels (MVVM)   │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                              │
│  Repositories → DAOs (Room) / SharedPreferences / DataStore │
├─────────────────────────────────────────────────────────────┤
│                     Printer Layer                            │
│  TicketFormatter (pure) → EscPosPrinterLan (TCP socket)     │
└─────────────────────────────────────────────────────────────┘
```

### Patrones clave

- **MVVM + UDF** (Unidirectional Data Flow)
- `StateFlow` expuesto por ViewModels, consumido con `collectAsStateWithLifecycle()`
- Composables **stateless**: reciben estado + lambdas `onXxx`
- **Inyección de dependencias manual** en `MainActivity.onCreate`
- Cada ViewModel con su propio `class Factory : ViewModelProvider.Factory`
- Navegación por **estado** (`mutableStateOf<NavDestination>` + `when`), sin Navigation Compose
- Single Activity pattern

---

## 🛠️ Stack Tecnológico

| Área | Tecnología | Versión |
|------|-----------|---------|
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| Build | Gradle KTS + Version Catalog | AGP 9.2.1 |
| Persistencia | Room (KSP) | 2.7.1 |
| Serialización | kotlinx-serialization-json | 1.8.1 |
| Preferencias | DataStore Preferences | 1.1.4 |
| Lifecycle | ViewModel Compose + Runtime Compose | 2.11.0 |
| SDK mínimo | Android | API 24 (Android 7.0) |
| SDK objetivo | Android | API 37 |
| Tests JVM | Kotest Property + JUnit + MockK + Turbine | 5.9.1 / 4.13.2 / 1.13.14 / 1.2.0 |
| Tests Device | Espresso + Compose UI Test + Room Testing | 3.7.0 |

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/example/puntodeventa/
├── MainActivity.kt                 # Single Activity: DI manual + navegación
├── data/
│   ├── local/                      # Room: AppDatabase, Entities, DAOs, Seeder
│   ├── json/                       # DTOs @Serializable para import/export JSON
│   ├── model/                      # Modelos de dominio: MenuItem, Category, Product,
│   │                               #   PaymentMethod, PeriodSummary, PaymentMethodRevenue,
│   │                               #   OrderTotalPoint, ProductSaleSummary, PrinterConfig
│   ├── printer/                    # EscPosPrinterLan (TCP ESC/POS)
│   └── repository/                 # Menu, Category, Product, Order, CatalogJson, Printer, Theme
└── ui/
    ├── navigation/                 # NavDestination (sealed), AppNavRail
    ├── theme/                      # AppTheme, ThemeColors, ThemeViewModel, Selector
    ├── home/                       # HomeScreen, HomeViewModel, MenuItemCard, AddMenu
    ├── pos/                        # PosScreen, PosViewModel, Catalog, Cart, Checkout,
    │                               #   TicketFormatter, ProductModal, CashKeypad
    ├── configuration/              # ConfigurationScreen, ConfigurationViewModel
    ├── newproduct/                 # NewProductModal, NewProductViewModel, GroupCard
    ├── stats/                      # StatsScreen, StatsViewModel, TimeFilter,
    │                               #   SalesTrendCalculator, SalesTrendChart, PaymentMethodDonut,
    │                               #   MetricDelta, StatsCsvBuilder, StatsFormatters
    ├── tickets/                    # TicketHistoryScreen, TicketHistoryViewModel, TicketCard
    └── printer/                    # PrinterScreen, PrinterConfigViewModel, ControlPanel
```

---

## 🖨️ Sistema de Impresión

### Flujo completo: Confirmar Pago → Impresora

```
Usuario presiona "Confirmar Pago"
    │
    ├─ 1. Validar IP de impresora (SharedPreferences)
    ├─ 2. Generar folio secuencial (001, 002, ...)
    ├─ 3. Formatear ticket cliente (con precios + IVA)
    ├─ 4. Formatear ticket interno (sin precios, para cocina)
    ├─ 5. Enviar ambos tickets por TCP (una sola conexión)
    │       └─ ESC_INIT → payload Cp850 → 3×LF → ESC_CUT (×2)
    └─ 6. Persistir orden en Room (solo si impresión exitosa)
```

### Especificaciones de conexión

| Parámetro | Valor |
|-----------|-------|
| Impresora | POS-8360 |
| Puerto | 9100 (ESC/POS estándar) |
| Connect timeout | 5 segundos |
| Overall timeout | 15 segundos |
| Charset | Cp850 (Code Page 850) |
| Dispatcher | `Dispatchers.IO` |

### Formato de ticket

- Ancho fijo: **48 caracteres**
- Columnas: `CANT(5)` + `DESCRIPCION(30)` + `IMPORTE(13)`
- IVA: 16% calculado como `total / 1.16` con `BigDecimal` HALF_UP
- Personalizaciones indentadas: `"      - {opción}"`
- Notas con word-wrapping automático

### Comandos ESC/POS

| Comando | Bytes | Función |
|---------|-------|---------|
| ESC_INIT | `0x1B 0x40` | Reset de la impresora |
| ESC_CUT | `0x1D 0x56 0x00` | Corte completo del papel |
| ESC_DOUBLE_HEIGHT | `0x1B 0x21 0x10` | Doble altura (ticket cocina) |
| ESC_NORMAL | `0x1B 0x21 0x00` | Tamaño normal |

---

## 🎨 Temas Disponibles

El motor de temas ofrece **9 esquemas de color** (6 claros + 3 oscuros), seleccionables en tiempo real sin reiniciar la app. La preferencia se persiste en **DataStore Preferences**.

### Temas originales

| Tema | Nombre en UI | Modo | Primary | Descripción |
|------|-------------|------|---------|-------------|
| `DEFAULT_GREEN` | Verde por Defecto | Claro | `#4A8C1C` | Tonos verdes naturales, tema principal |
| `DARK_NEON` | Neón Oscuro | Oscuro | `#39FF14` | Fondo oscuro (#121212) con acentos neón verde y cian |
| `OCEAN_BLUE` | Océano Azul | Claro | `#1565C0` | Azules profesionales sobre fondo blanco |
| `SUNSET_ORANGE` | Atardecer Naranja | Claro | `#E65100` | Naranjas cálidos con acentos dorados |

### Temas premium (expansión)

| Tema | Nombre en UI | Modo | Primary | Descripción |
|------|-------------|------|---------|-------------|
| `MIDNIGHT_SLATE` | Pizarra Medianoche | Oscuro | `#8AB4FF` | Pizarra azulada con índigo suave y acento teal. Sobrio y tecnológico. |
| `CHARCOAL_AMBER` | Carbón Ámbar | Oscuro | `#FFCA6B` | Carbón cálido con ámbar de lujo y acento salvia. Acogedor para uso nocturno. |
| `ROSE_QUARTZ` | Cuarzo Rosa | Claro | `#B0235A` | Blush suave con rosa boutique y acento bronce. Elegante y cálido. |
| `EMERALD_TEAL` | Esmeralda | Claro | `#00695C` | Menta fría con esmeralda profundo y acento azul pizarra. Limpio y profesional. |
| `ROYAL_PLUM` | Ciruela Real | Claro | `#6A1B9A` | Lila aireado con ciruela regia y acento rosa. Rico y distintivo. |

> Los hex completos de cada color role (primary, secondary, tertiary, containers, surfaces, error, outlines) están documentados en `.kiro/specs/18_theme_engine/design.md`.

---

## 🗄️ Base de Datos

**Room** v5 con `Migration(4,5)` explícita — nombre: `punto_de_venta_db`

> La migración 4→5 agrega la columna `orders.paymentMethod` con default `'EFECTIVO'` para preservar
> el historial de órdenes al actualizar. `fallbackToDestructiveMigration` sigue como last-resort.

### Esquema de entidades

```
menu_items
└── categories (FK: associatedMenuId → menu_items.id, CASCADE)
    └── products (FK: categoryId → categories.id, CASCADE)
        └── customization_groups (FK: productId → products.id, CASCADE)
            └── customization_options (FK: groupId → groups.id, CASCADE)

orders
└── order_items (FK: orderId → orders.id, CASCADE)
    └── order_item_customizations (FK: orderItemId → order_items.id, CASCADE)
```

### Seeder

La app incluye un `DatabaseSeeder` que pobla datos de ejemplo al primer inicio:
- 1 menú por defecto con categorías y productos
- IDs deterministas con `UUID.nameUUIDFromBytes`
- Ejecutado en transacción atómica antes de exponer la DB

### Gestión de Catálogo vía JSON

La pantalla de Configuración permite gestionar el catálogo completo mediante JSON:

**Exportar** — Serializa toda la jerarquía (MenuItems → Categories → Products → CustomizationGroups → Options) a un archivo `.json` con esquema anidado. Usa Android Storage Access Framework para que el usuario elija la ubicación de guardado.

**Importar** — Lee un archivo `.json`, valida el esquema (version, IDs, tipos, precios), muestra confirmación y ejecuta un replace-all transaccional: borra todo el catálogo actual e inserta el nuevo en una sola transacción Room.

**Editor in-app** — Muestra el catálogo actual como JSON formateado en un TextField monospace editable, permitiendo modificaciones directas con la misma validación y transacción del import.

Esquema JSON:
```json
{
  "version": 1,
  "exportedAt": "ISO-8601",
  "catalog": {
    "menuItems": [{ "id", "emoji", "name", "categories": [
      { "id", "name", "products": [
        { "id", "emoji", "name", "description", "basePrice", "isActive",
          "customizationGroups": [
            { "id", "groupName", "selectionType", "options": [
              { "id", "optionName", "extraPrice" }
            ]}
          ]}
      ]}
    ]}]
  }
}
```

---

## 📱 Requisitos

- **Android Studio** Ladybug o superior
- **JDK 11** o superior
- Dispositivo o emulador con **API 24+** (Android 7.0+)
- Para impresión: impresora térmica compatible ESC/POS en la misma red LAN

---

## 🚀 Instalación y Build

```bash
# Clonar el repositorio
git clone https://github.com/CONTERNICO845/pos-android-kiro.git
cd pos-android-kiro

# Build del APK debug
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

> **Windows:** Usar `gradlew.bat` en lugar de `./gradlew`

---

## 🧪 Testing

```bash
# Tests unitarios JVM (~40 archivos)
./gradlew testDebugUnitTest

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedDebugAndroidTest
```

### Frameworks de testing

| Framework | Uso |
|-----------|-----|
| **Kotest Property** | Property-based testing de lógica pura |
| **JUnit 4/5** | Tests unitarios convencionales |
| **MockK** | Mocking de repositorios y dependencias |
| **Turbine** | Testing de `Flow` y `StateFlow` |
| **Compose UI Test** | Tests de interfaz instrumentados |
| **Room Testing** | Validación de DAOs y migraciones |

### Cobertura de tests

- `app/src/test/` — ~40 archivos: ViewModels, TicketFormatter, Repositories, lógica de filtrado
- `app/src/androidTest/` — ~34 archivos: DAOs, UI Compose, flujos de navegación, impresora

---

## 📄 Licencia

Copyright (c) 2026 Geovani Gael (CONTERNICO). Todos los derechos reservados.

El código fuente de este proyecto se publica en este repositorio público exclusivamente con fines de **demostración**, **evaluación de portafolio** y **análisis educativo**.

Por la presente, **NO** se concede ningún derecho ni licencia, explícita o implícita, para:

1. Usar, copiar, modificar, fusionar, publicar, distribuir, sublicenciar o vender copias de este software o partes del mismo.
2. Utilizar este código en entornos de producción o con fines comerciales.
3. Crear software o trabajos derivados basados en esta arquitectura, lógica o diseño.

Cualquier uso del código fuente, total o parcial, fuera de la simple lectura y visualización en la plataforma GitHub, requiere una **autorización expresa y por escrito** del autor original.
