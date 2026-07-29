
# Implementation Plan: Dynamic Theme Engine

## Overview

Implementación incremental del Dynamic Theme Engine: desde la capa de datos (enum + DataStore + repositorio) hasta la UI (ViewModel, composable raíz, pantalla selector), integrando tests de propiedad y unitarios en cada paso. El enfoque asegura que cada tarea produce código funcional que se conecta con lo anterior.

## Tasks

- [x] 1. Add DataStore dependency and define AppTheme enum
  - [x] 1.1 Add Preferences DataStore to version catalog and build config
    - Add `datastorePreferences` version entry to `gradle/libs.versions.toml` (pinned version `1.1.4`)
    - Add `androidx-datastore-preferences` library entry in `[libraries]`
    - Add `implementation(libs.androidx.datastore.preferences)` to `app/build.gradle.kts`
    - Verify project compiles without unresolved import errors for `androidx.datastore.preferences`
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [x] 1.2 Create AppTheme enum with companion object
    - Create file `ui/theme/AppTheme.kt`
    - Define `enum class AppTheme` with values: `DEFAULT_GREEN`, `DARK_NEON`, `OCEAN_BLUE`, `SUNSET_ORANGE`
    - Add `companion object` with `val DEFAULT = DEFAULT_GREEN` and `fun fromName(name: String): AppTheme`
    - Add extension properties: `displayName` (Spanish names), `previewColors` (Triple of primary, background, accent Color)
    - _Requirements: 1.1, 1.2, 1.3, 9.1, 9.2_

  - [x]* 1.3 Write property tests for AppTheme enum (P4, P5)
    - **Property 4: Theme metadata completeness** — For any AppTheme value, `displayName` is non-empty and `previewColors` returns 3 non-null Colors
    - **Validates: Requirements 9.1, 9.2**
    - **Property 5: ColorScheme mapping is total and pure** — For any AppTheme value, `toColorScheme()` returns non-null without exceptions, and repeated calls produce identical results
    - **Validates: Requirements 4.3, 1.1**
    - Create test file `app/src/test/java/com/example/puntodeventa/ui/theme/AppThemePropertyTest.kt`
    - Use `Arb.enum<AppTheme>()` generator, minimum 100 iterations

- [x] 2. Define ColorScheme mappings for all 4 themes
  - [x] 2.1 Implement `toColorScheme()` extension function
    - Create or modify `ui/theme/ThemeColors.kt`
    - Implement exhaustive `when` expression mapping each `AppTheme` to its `ColorScheme`
    - `DEFAULT_GREEN`: lightColorScheme matching existing `AppColorScheme` colors byte-identical
    - `DARK_NEON`: darkColorScheme with bg #121212, surface #1E1E1E, primary #39FF14, secondary #00FFFF
    - `OCEAN_BLUE`: lightColorScheme with primary #1565C0, secondary #42A5F5, bg #FFFFFF
    - `SUNSET_ORANGE`: lightColorScheme with primary #E65100, secondary #FFC107, bg #FFFBF5
    - Include all color roles (onPrimary, primaryContainer, onPrimaryContainer, etc.) per design spec
    - _Requirements: 4.1, 4.2, 4.3, 5.1, 5.2, 5.4, 6.1, 6.3, 7.1, 7.3_

  - [x]* 2.2 Write property test for WCAG contrast compliance (P3)
    - **Property 3: WCAG contrast compliance across all themes** — For any AppTheme, all text/background pairings have ≥4.5:1 contrast ratio
    - **Validates: Requirements 5.3, 6.2, 7.2**
    - Implement `contrastRatio()` and `relativeLuminance()` utility functions in test support
    - Use `Arb.enum<AppTheme>()` and verify all 6 pairings (onPrimary/primary, onSecondary/secondary, onBackground/background, onSurface/surface, onError/error, onPrimaryContainer/primaryContainer)

  - [x]* 2.3 Write unit tests for each ColorScheme
    - Test `DEFAULT_GREEN` ColorScheme matches legacy `AppColorScheme` hex values exactly
    - Test `DARK_NEON` uses `darkColorScheme` with correct hex values and error color #E53935
    - Test `OCEAN_BLUE` uses `lightColorScheme` with correct primary #1565C0
    - Test `SUNSET_ORANGE` uses `lightColorScheme` with correct colors per spec
    - Test enum has exactly 4 entries
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.4, 6.1, 7.1_

- [x] 3. Implement ThemePreferencesRepository
  - [x] 3.1 Create ThemePreferencesRepository with DataStore
    - Create file `data/repository/ThemePreferencesRepository.kt`
    - Define DataStore instance creation (top-level `val Context.themeDataStore`)
    - Implement `themeFlow: Flow<AppTheme>` with `catch { emit(emptyPreferences()) }` and `map` using `AppTheme.fromName()`
    - Implement `suspend fun saveTheme(theme: AppTheme)` with `dataStore.edit`
    - Handle invalid/corrupted values by falling back to DEFAULT_GREEN
    - Handle write failures by catching exception so Flow remains uninterrupted
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 11.4_

  - [x]* 3.2 Write property test for persistence round-trip (P1)
    - **Property 1: Theme persistence round-trip** — For any sequence of AppTheme values saved, the themeFlow emits each in order, and collecting after the last save yields the last saved value
    - **Validates: Requirements 2.1, 2.4**
    - Use `Arb.list(Arb.enum<AppTheme>(), 1..10)` generator
    - Use `kotlinx-coroutines-test` and `turbine` for Flow testing with a fake/in-memory DataStore

  - [x]* 3.3 Write property test for invalid name fallback (P2)
    - **Property 2: Invalid theme name falls back to DEFAULT_GREEN** — For any arbitrary string NOT matching an AppTheme name, themeFlow emits DEFAULT_GREEN
    - **Validates: Requirements 2.5**
    - Use `Arb.string()` filtered to exclude valid AppTheme names
    - Inject corrupted value directly into a fake DataStore and verify flow emission

  - [x]* 3.4 Write unit tests for ThemePreferencesRepository
    - Test empty DataStore emits DEFAULT_GREEN
    - Test DataStore write failure preserves last valid value
    - Test initial emission occurs (validates Req 2.2 timing)
    - _Requirements: 2.2, 2.3, 2.5, 2.6_

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement ThemeViewModel and modify Root Composable
  - [x] 5.1 Create ThemeViewModel
    - Create file `ui/theme/ThemeViewModel.kt`
    - Implement `currentTheme: StateFlow<AppTheme>` collecting from `ThemePreferencesRepository.themeFlow` with `stateIn(Eagerly, initialValue = AppTheme.DEFAULT)`
    - Add error handling: `catch { emit(AppTheme.DEFAULT) }` before `stateIn`
    - Implement `fun selectTheme(theme: AppTheme)` launching `repository.saveTheme(theme)` in `viewModelScope`
    - Create inner `Factory` class implementing `ViewModelProvider.Factory`
    - _Requirements: 3.3, 3.4, 10.2_

  - [x] 5.2 Modify PuntoDeVentaTheme root composable
    - Modify `ui/theme/Theme.kt` to accept `appTheme: AppTheme = AppTheme.DEFAULT` parameter
    - Replace hardcoded `AppColorScheme` with `appTheme.toColorScheme()`
    - Keep `Typography` unchanged
    - Ensure the existing `AppColorScheme` private val remains as documentation/reference only or is removed
    - _Requirements: 3.1, 3.2_

  - [x] 5.3 Wire ThemeViewModel into MainActivity
    - In `MainActivity`, create DataStore instance and `ThemePreferencesRepository`
    - Instantiate `ThemeViewModel` using its `Factory`
    - Observe `themeViewModel.currentTheme` with `collectAsStateWithLifecycle()`
    - Pass the collected `AppTheme` value to `PuntoDeVentaTheme(appTheme = ...)`
    - _Requirements: 3.1, 3.2, 3.3_

  - [x]* 5.4 Write unit tests for ThemeViewModel
    - Test initial value is DEFAULT_GREEN
    - Test ViewModel falls back to DEFAULT_GREEN on repository error
    - Test selectTheme triggers repository save
    - Use `mockk` for repository and `turbine` for StateFlow testing
    - _Requirements: 3.3, 3.4, 10.2_

- [x] 6. Implement ThemeSelectorScreen and ThemeCard UI
  - [x] 6.1 Create ThemeCard composable
    - Create file `ui/theme/ThemeCard.kt`
    - Accept params: `theme: AppTheme`, `isSelected: Boolean`, `onClick: () -> Unit`
    - Display theme `displayName` as text
    - Display 3 color swatches (primary, background, accent) as filled circles in a horizontal row using `previewColors`
    - Show distinct border color + check icon overlay when `isSelected = true`
    - No border highlight and no check icon when `isSelected = false`
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 6.2 Create ThemeSelectorScreen composable
    - Create file `ui/theme/ThemeSelectorScreen.kt`
    - Accept params: `currentTheme: AppTheme`, `onThemeSelected: (AppTheme) -> Unit`
    - Display title "Apariencia" as first element above the grid
    - Use `LazyVerticalGrid` with `GridCells.Fixed(2)` containing exactly 4 `ThemeCard` items (one per `AppTheme.entries`)
    - Pass `isSelected = (theme == currentTheme)` to each card
    - Dispatch `onThemeSelected(theme)` on card tap
    - _Requirements: 8.1, 8.2, 8.4, 8.5, 10.1_

  - [x] 6.3 Add navigation from Settings to ThemeSelectorScreen
    - Add "Apariencia" navigation item in the Settings section/screen
    - Wire navigation to `ThemeSelectorScreen` passing `themeViewModel.currentTheme` and `themeViewModel::selectTheme`
    - Ensure theme change is dispatched to ViewModel and applied immediately without restart
    - _Requirements: 8.3, 10.1, 10.2, 10.3_

  - [x]* 6.4 Write unit/UI tests for ThemeSelectorScreen
    - Test screen shows 4 cards in 2-column grid
    - Test title "Apariencia" is displayed above grid
    - Test active theme card shows border + check icon
    - Test non-active cards have no highlight/icon
    - Test tapping a card triggers `onThemeSelected` callback
    - _Requirements: 8.1, 8.2, 9.3, 9.4, 10.1_

- [x] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (5 properties from design)
- Unit tests validate specific examples and edge cases
- The project already has Kotest Property, JUnit5, mockk, turbine, and kotlinx-coroutines-test as test dependencies
- Preferences DataStore is the only new dependency required (Req 11)
- The `DEFAULT_GREEN` ColorScheme must be byte-identical to the current `AppColorScheme` to avoid visual regression

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "2.1", "3.1"] },
    { "id": 3, "tasks": ["2.2", "2.3", "3.2", "3.3", "3.4"] },
    { "id": 4, "tasks": ["5.1", "5.2"] },
    { "id": 5, "tasks": ["5.3", "5.4"] },
    { "id": 6, "tasks": ["6.1"] },
    { "id": 7, "tasks": ["6.2"] },
    { "id": 8, "tasks": ["6.3", "6.4"] }
  ]
}
```
