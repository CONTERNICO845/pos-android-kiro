# AGENT.md — PuntoDeVenta (Spec-Driven Development)

## Project Overview

**PuntoDeVenta** is a native Android POS (Point of Sale) application built with Jetpack Compose and Material3.
The app features a persistent left-side navigation rail and a card-based menu creation flow.
All UI is written in Kotlin using Compose; there is no XML layout layer.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI Toolkit | Jetpack Compose (BOM 2026.02.01) |
| Design System | Material3 |
| Min SDK | 24 (Android 7.0) |
| Target / Compile SDK | 36 |
| Build System | Gradle (KTS) with Version Catalog (`libs.versions.toml`) |
| Architecture | MVVM + Unidirectional Data Flow (UDF) |

---

## Global Color Palette (Single Source of Truth)

All colors are defined in **one place only**:

```
app/src/main/java/com/example/puntodeventa/ui/theme/Color.kt
```

To retheme the entire app, edit only this file. No color value may be hardcoded anywhere else in the codebase.

### Current Palette

```kotlin
// ── Backgrounds ─────────────────────────────────────────
val BackgroundPrimary   = Color(0xFF6BBF3E)   // Main screen background (bright green)
val BackgroundSecondary = Color(0xFF5AAD30)   // Subtle variant / gradient end

// ── Navigation Rail ─────────────────────────────────────
val NavRailBackground   = Color(0xFFF5F0E8)   // Off-white / cream rail surface
val NavRailIconDefault  = Color(0xFF2D2D2D)   // Unselected icon tint
val NavRailIconSelected = Color(0xFF4A8C1C)   // Selected icon tint (dark green)

// ── Cards ────────────────────────────────────────────────
val CardBackground      = Color(0xFF2D5A1B)   // Dark green menu card surface
val CardText            = Color(0xFFFFFFFF)   // White card label / placeholder text
val CardIconTint        = Color(0xFFFFFFFF)   // White "+" icon tint

// ── Modal / Dialog ───────────────────────────────────────
val ModalSurface        = Color(0xFFFFFFFF)   // Dialog background
val ModalTitleText      = Color(0xFF1A1A1A)   // Dialog title color
val ModalBodyText       = Color(0xFF333333)   // Labels inside dialog
val EmojiPickerBorder   = Color(0xFFE0E0E0)   // Emoji cell border (unselected)
val EmojiPickerSelected = Color(0xFF1565C0)   // Emoji cell border (selected, blue)
val SearchBarBorder     = Color(0xFF4A8C1C)   // "BUSCAR EMOJI" bar border

// ── Action Buttons ───────────────────────────────────────
val ButtonConfirm       = Color(0xFF4CAF50)   // "GUARDAR CAMBIOS" green button
val ButtonConfirmText   = Color(0xFFFFFFFF)
val ButtonCancel        = Color(0xFFE53935)   // "DESCARTAR CAMBIOS" red button
val ButtonCancelText    = Color(0xFFFFFFFF)

// ── Text Input ───────────────────────────────────────────
val InputBorder         = Color(0xFF4A8C1C)   // Name text field border
val InputBackground     = Color(0xFFFFFFFF)
val InputText           = Color(0xFF1A1A1A)
val InputHint           = Color(0xFF9E9E9E)   // Placeholder "Tu Nombre Aqui"
```

The `Theme.kt` must set `dynamicColor = false` and wire these tokens into the `MaterialTheme` color scheme so Material3 components automatically inherit them.

---

## Project File Structure

```
app/src/main/java/com/example/puntodeventa/
│
├── MainActivity.kt                   # Entry point; hosts NavRail + content scaffold
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                  # ← ONLY place for color values
│   │   ├── Theme.kt                  # MaterialTheme wiring, dynamicColor = false
│   │   └── Type.kt                   # Typography tokens
│   │
│   ├── navigation/
│   │   ├── NavRail.kt                # Persistent left NavigationRail composable
│   │   └── NavDestination.kt         # Sealed class: Home, Stats, Settings, Tickets
│   │
│   ├── home/
│   │   ├── HomeScreen.kt             # Main content area (card grid)
│   │   ├── HomeViewModel.kt          # Holds menuItems: List<MenuItem> state
│   │   ├── AddMenuCard.kt            # "+" dark card composable
│   │   ├── MenuItemCard.kt           # Saved menu card with edit pencil icon
│   │   └── AddMenuDialog.kt          # "AGREGAR TU MENU" modal composable
│   │
│   ├── stats/
│   │   └── StatsScreen.kt            # Estadísticas placeholder screen
│   │
│   ├── settings/
│   │   └── SettingsScreen.kt         # Configuración placeholder screen
│   │
│   └── tickets/
│       └── TicketsScreen.kt          # Tickets placeholder screen
│
└── data/
    └── model/
        └── MenuItem.kt               # Data class: id, emoji, name
```

---

## Architecture

### Pattern: MVVM + UDF

```
User Action
    │
    ▼
Composable (UI)  ──event──►  ViewModel  ──updates──►  StateFlow<UiState>
    ▲                                                         │
    └──────────────── recompose ◄─────────────────────────────┘
```

- **Composables** are stateless; they observe `collectAsStateWithLifecycle()`.
- **HomeViewModel** owns the `menuItems` list and `dialogVisible` flag.
- **No Room / no network** in this phase — state is in-memory only.

### UiState model

```kotlin
data class HomeUiState(
    val menuItems: List<MenuItem> = emptyList(),
    val isDialogOpen: Boolean = false,
    val editingItem: MenuItem? = null       // null = create mode, non-null = edit mode
)

data class MenuItem(
    val id: String,          // UUID
    val emoji: String,       // e.g. "🌮"
    val name: String         // e.g. "TACOS BLANCA"
)
```

---

## Screen Specifications

### 1. App Shell — `MainActivity.kt`

- `Row` layout: `NavRail` (fixed width ~60 dp) + `Box` (fills remaining width).
- Background color of the `Box` = `BackgroundPrimary`.
- `enableEdgeToEdge()` is called; status bar and navigation bar are made transparent.
- The `NavRail` is always rendered regardless of which destination is active.
- `PuntoDeVentaTheme` wraps everything; `dynamicColor = false`.

---

### 2. Navigation Rail — `NavRail.kt`

| Property | Value |
|---|---|
| Surface color | `NavRailBackground` (cream/off-white) |
| Width | Wrap content (Material3 default ~80 dp) |
| Position | Left edge, full height |
| Header | App logo image or tinted icon at top |

**Destinations (top to bottom):**

| Label | Icon | Route |
|---|---|---|
| Inicio | `Icons.Default.Home` | `home` |
| Estadísticas | `Icons.Default.BarChart` | `stats` |
| Configuración | `Icons.Default.Settings` | `settings` |
| Tickets | `Icons.Default.ConfirmationNumber` | `tickets` |

- Selected item uses `NavRailIconSelected` tint.
- Unselected items use `NavRailIconDefault` tint.
- No bottom navigation bar exists; the rail is the only nav element.

---

### 3. Home Screen — `HomeScreen.kt`

- Full-screen background: `BackgroundPrimary`.
- Content: `LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 200.dp))`.
- Each cell renders either a `MenuItemCard` or the `AddMenuCard`.
- The `AddMenuCard` is always rendered as the **last** item in the grid.
- Vertical + horizontal padding: `16.dp`.
- Card spacing: `16.dp`.

#### 3a. Add Menu Card — `AddMenuCard.kt`

| Property | Value |
|---|---|
| Size | `200.dp × 200.dp` (square) |
| Shape | `RoundedCornerShape(12.dp)` |
| Background | `CardBackground` (dark green) |
| Center icon | `Icons.Default.AddCircle`, size `80.dp`, tint `CardIconTint` |
| Bottom label | `"NOMBRE...."`, `FontWeight.Bold`, color `CardText` |
| Click action | Opens `AddMenuDialog` (create mode) |

#### 3b. Menu Item Card — `MenuItemCard.kt`

| Property | Value |
|---|---|
| Size | `200.dp × 200.dp` (square) |
| Shape | `RoundedCornerShape(12.dp)` |
| Background | `CardBackground` (dark green) |
| Center content | Emoji rendered as large `Text`, size `64.sp` |
| Bottom label | Menu name, `FontWeight.Bold`, color `CardText`, uppercase |
| Edit icon | `Icons.Default.Edit`, top-right corner, `16.dp` padding, tint `CardIconTint` |
| Edit click | Opens `AddMenuDialog` (edit mode, pre-populated) |

---

### 4. Add Menu Dialog — `AddMenuDialog.kt`

Triggered from `HomeViewModel.openDialog()` / `HomeViewModel.openEditDialog(item)`.

#### Modal container

| Property | Value |
|---|---|
| Type | `AlertDialog` or custom `Dialog` composable |
| Surface color | `ModalSurface` |
| Shape | `RoundedCornerShape(16.dp)` |
| Title | `"AGREGAR TU MENU"`, `FontWeight.Bold`, `ModalTitleText` |

#### Emoji Picker section

- Section label: `"SELECCIONA TU FOTO DEL MENU"`, uppercase, `ModalBodyText`.
- Grid: `LazyVerticalGrid(columns = GridCells.Fixed(6))` inside a fixed-height `Box` (~180 dp).
- Each cell: `72.dp × 72.dp`, `RoundedCornerShape(8.dp)`, border `EmojiPickerBorder`.
- Selected cell: border changes to `EmojiPickerSelected` (blue), `2.dp` width.
- Default emoji set: minimum 18 emojis covering food, faces, and miscellaneous categories.
- Search bar below the grid:
  - Placeholder: `"BUSCAR EMOJI"` with search icon prefix.
  - Filters the visible emoji list in real time.
  - Border color: `SearchBarBorder`.

#### Name input section

- Label: `"ESCRIBE EL NOMBRE DE TU MENU"`, uppercase, `ModalBodyText`.
- `OutlinedTextField`, placeholder `"Tu Nombre Aqui"`.
- Border color (focused/unfocused): `InputBorder`.
- Background: `InputBackground`.

#### Action buttons

| Button | Label | Background | Text Color |
|---|---|---|---|
| Confirm | `"✓  GUARDAR CAMBIOS"` | `ButtonConfirm` | `ButtonConfirmText` |
| Cancel | `"DESCARTAR CAMBIOS"` | `ButtonCancel` | `ButtonCancelText` |

- Buttons are side-by-side in a `Row`, equally weighted (`weight(1f)` each).
- Confirm: validates that an emoji is selected AND name is not blank before saving.
- Cancel: dismisses without saving; resets dialog state.

#### Validation rules

1. An emoji must be selected (no empty selection allowed).
2. The menu name must be non-blank after trimming.
3. If validation fails, show an inline error message below the name field.

---

### 5. HomeViewModel — `HomeViewModel.kt`

```kotlin
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun openDialog()                          // create mode
    fun openEditDialog(item: MenuItem)        // edit mode
    fun dismissDialog()
    fun saveMenu(emoji: String, name: String) // add or update item
    fun deleteMenu(id: String)                // future use
}
```

- `saveMenu` generates a `UUID.randomUUID().toString()` id for new items.
- In edit mode, it replaces the existing item in the list by id.

---

## Navigation Destinations — `NavDestination.kt`

```kotlin
sealed class NavDestination(val route: String, val label: String) {
    object Home        : NavDestination("home",     "Inicio")
    object Stats       : NavDestination("stats",    "Estadísticas")
    object Settings    : NavDestination("settings", "Configuración")
    object Tickets     : NavDestination("tickets",  "Tickets")
}
```

Navigation is managed via `remember { mutableStateOf<NavDestination>(NavDestination.Home) }` in `MainActivity` (no Jetpack Navigation component required for this phase — simple conditional rendering is sufficient).

---

## Theming Rules (Enforced)

1. **`Color.kt` is the single source of truth.** No `Color(0x...)` literals anywhere else.
2. **`dynamicColor = false`** in `Theme.kt`. Dynamic color (Material You) is disabled so the green brand palette is always shown.
3. **No hardcoded colors** in any composable. Always reference a token from `Color.kt`.
4. To fully retheme the app: update values in `Color.kt` only — every screen updates automatically.
5. Dark mode support is deferred; `darkTheme` branch in `Theme.kt` may mirror the light scheme for now.

---

## Emoji Data

Define a default emoji list as a `val defaultEmojiList: List<String>` in a file named `EmojiData.kt` under `ui/home/`. Minimum required categories and examples:

| Category | Examples |
|---|---|
| Food & Drink | 🌮 🍕 🍔 🍜 🍣 🥗 🍩 ☕ 🧃 🍺 |
| Faces | 😀 😊 🥰 😏 😎 🤩 😋 🤔 |
| Animals | 🐱 🐶 🦊 🐻 🐼 🐸 |
| Misc | 🌍 ⭐ 🎉 🏆 🔥 💎 |

The emoji picker shows all emojis by default and filters by `emoji.contains(searchQuery)` when the search bar is non-empty. Because Unicode emoji cannot be substring-searched meaningfully, implement a `EmojiEntry(emoji: String, tags: List<String>)` model so tags like `"taco"`, `"food"`, `"mexico"` power the search.

---

## Correctness Properties (Property-Based Testing)

The following properties must hold and must be covered by unit tests:

| # | Property | Description |
|---|---|---|
| P1 | **Menu item persistence** | After `saveMenu(e, n)`, `uiState.menuItems` contains exactly one item with that emoji and name. |
| P2 | **AddMenuCard always last** | For any list of N items, the grid always has N+1 cells and the last cell is always the `AddMenuCard`. |
| P3 | **Edit preserves id** | After editing an existing item, its `id` is unchanged and the list size stays the same. |
| P4 | **No duplicate ids** | After any number of `saveMenu` calls, all ids in `menuItems` are distinct. |
| P5 | **Dialog dismissal resets state** | After `dismissDialog()`, `isDialogOpen == false` and `editingItem == null`. |
| P6 | **Validation gate** | `saveMenu` with blank name must not add an item to the list (enforced in ViewModel or Dialog). |
| P7 | **Emoji filter correctness** | Searching a tag keyword returns only emojis whose tag list contains that keyword (case-insensitive). |

---

## Development Phases

### Phase 1 — Theme Foundation
- [ ] Update `Color.kt` with the full palette above.
- [ ] Update `Theme.kt`: disable dynamic color, wire palette into `MaterialTheme`.
- [ ] Verify no existing hardcoded colors remain.

### Phase 2 — Shell & Navigation Rail
- [ ] Create `NavDestination.kt`.
- [ ] Create `NavRail.kt` with 4 destinations.
- [ ] Refactor `MainActivity.kt` to host Rail + content `Box`.
- [ ] Confirm Rail is visible on all destinations.

### Phase 3 — Home Screen & Cards
- [ ] Create `MenuItem.kt` data class.
- [ ] Create `HomeViewModel.kt` with `HomeUiState`.
- [ ] Create `AddMenuCard.kt`.
- [ ] Create `MenuItemCard.kt`.
- [ ] Create `HomeScreen.kt` with `LazyVerticalGrid`.
- [ ] Wire ViewModel to HomeScreen via `collectAsStateWithLifecycle`.

### Phase 4 — Add/Edit Modal
- [ ] Create `EmojiData.kt` with `EmojiEntry` model and default list.
- [ ] Create `AddMenuDialog.kt` with picker, search, name field, and action buttons.
- [ ] Implement create flow end-to-end.
- [ ] Implement edit flow (pre-populate dialog from existing `MenuItem`).
- [ ] Validate required fields before saving.

### Phase 5 — Placeholder Screens
- [ ] Create `StatsScreen.kt`, `SettingsScreen.kt`, `TicketsScreen.kt` (empty scaffolds).

### Phase 6 — Tests
- [ ] Unit test `HomeViewModel` covering properties P1–P7.
- [ ] (Optional) Compose UI test for dialog open/close flow.

---

## Build & Run

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (connected device required)
./gradlew connectedDebugAndroidTest
```

> On Windows use `gradlew.bat` instead of `./gradlew`.

---

## Constraints & Decisions

- **No Jetpack Navigation** in Phase 1–6. Simple `when (currentDestination)` conditional rendering in `MainActivity` is sufficient and keeps complexity low.
- **No persistence layer** (Room, DataStore) in this phase. All state is in-memory via `StateFlow`.
- **No network calls** in this phase.
- **`dynamicColor = false`** — Material You dynamic theming is explicitly disabled to preserve the brand green palette on Android 12+.
- Emoji picker uses Unicode text rendering, not image assets, to keep APK size minimal.
- The app is designed for tablet/landscape orientation (POS hardware), but must also function in portrait on phones.
