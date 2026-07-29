# Requirements Document

## Introduction

Dynamic Theme Engine para la aplicación POS Android (Compose/Material3). Este sistema permite al usuario personalizar la apariencia visual completa de la aplicación seleccionando entre 4 temas predefinidos. La preferencia se persiste localmente y se aplica de forma reactiva a toda la interfaz sin necesidad de reiniciar la app.

## Glossary

- **Theme_Engine**: Módulo responsable de gestionar la selección, persistencia y aplicación del tema visual activo en toda la aplicación.
- **AppTheme**: Enumeración Kotlin que define los 4 temas disponibles: DEFAULT_GREEN, DARK_NEON, OCEAN_BLUE, SUNSET_ORANGE.
- **Theme_Preferences_Repository**: Componente de persistencia que guarda y recupera la preferencia de tema seleccionado usando Preferences DataStore.
- **Theme_Selector_Screen**: Pantalla accesible desde la sección de Configuración que muestra las opciones de tema disponibles en formato de cuadrícula.
- **ColorScheme**: Objeto Material3 `ColorScheme` que define la paleta de colores completa para un tema dado.
- **Root_Composable**: Composable de nivel superior (`PuntoDeVentaTheme`) en `MainActivity` que envuelve toda la UI y aplica el `MaterialTheme`.
- **Theme_ViewModel**: ViewModel que expone el estado del tema seleccionado como `StateFlow` y gestiona los eventos de cambio de tema.

## Requirements

### Requirement 1: Theme Enumeration Definition

**User Story:** As a developer, I want a well-defined enum of available themes, so that the codebase has a single source of truth for supported visual themes.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define an AppTheme enum with exactly four values: DEFAULT_GREEN, DARK_NEON, OCEAN_BLUE, and SUNSET_ORANGE.
2. IF no theme preference entry exists in the persisted settings at app startup, THEN THE Theme_Engine SHALL use DEFAULT_GREEN as the active theme.
3. THE AppTheme enum SHALL be exhaustive such that adding or removing a value produces a compile-time error in all `when` expressions that consume it.

### Requirement 2: Theme Persistence

**User Story:** As a user, I want my selected theme to persist across app restarts, so that I do not need to reconfigure my preference each time I open the app.

#### Acceptance Criteria

1. WHEN the user selects a theme, THE Theme_Preferences_Repository SHALL save the selected AppTheme value to Preferences DataStore before emitting the updated value to collectors.
2. WHEN the application starts, THE Theme_Preferences_Repository SHALL emit the previously saved AppTheme value as the initial state within 500 milliseconds of the first collection.
3. IF no theme preference has been saved previously, THEN THE Theme_Preferences_Repository SHALL emit DEFAULT_GREEN as the initial value.
4. THE Theme_Preferences_Repository SHALL expose the saved theme as a Kotlin Flow that emits the current value upon collection and any subsequent changes.
5. IF the stored theme value is unrecognized or corrupted, THEN THE Theme_Preferences_Repository SHALL emit DEFAULT_GREEN and overwrite the invalid entry with DEFAULT_GREEN in Preferences DataStore.
6. IF a write to Preferences DataStore fails, THEN THE Theme_Preferences_Repository SHALL retain the previous valid AppTheme value in the emitted Flow without interrupting collectors.

### Requirement 3: Reactive Theme Application

**User Story:** As a user, I want the entire application to update its colors immediately when I select a new theme, so that my choice takes effect without restarting the app.

#### Acceptance Criteria

1. THE Root_Composable SHALL observe the theme StateFlow from Theme_ViewModel and pass the ColorScheme that maps to the current AppTheme value to MaterialTheme.
2. WHEN the theme StateFlow emits a new AppTheme value, THE Root_Composable SHALL recompose all composables that read MaterialTheme color attributes with the updated ColorScheme within 1 rendering frame.
3. THE Theme_ViewModel SHALL collect the Flow from Theme_Preferences_Repository and expose the current AppTheme as a StateFlow initialized with a default value of AppTheme.DEFAULT_GREEN.
4. IF Theme_Preferences_Repository emits an error or returns an unrecognized value, THEN THE Theme_ViewModel SHALL fall back to AppTheme.DEFAULT_GREEN and continue exposing a valid StateFlow without crashing.

### Requirement 4: DEFAULT_GREEN Color Palette

**User Story:** As a user, I want the default green theme to match the current application appearance, so that upgrading to the theme engine introduces no visual regression.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define the DEFAULT_GREEN ColorScheme as a lightColorScheme that assigns the same color values to the same Material3 color roles as the current AppColorScheme in Theme.kt: primary (#4A8C1C), onPrimary (#FFFFFF), primaryContainer (#2D5A1B), onPrimaryContainer (#FFFFFF), secondary (#4CAF50), onSecondary (#FFFFFF), background (#6BBF3E), onBackground (#FFFFFF), surface (#FFFFFF), onSurface (#1A1A1A), error (#E53935), and onError (#FFFFFF).
2. WHEN DEFAULT_GREEN is the active theme, THE Theme_Engine SHALL supply a Material3 ColorScheme whose color-role values are byte-identical to the current AppColorScheme, such that no composable receives a different resolved color from MaterialTheme.colorScheme.
3. IF the Theme_Engine fails to load the DEFAULT_GREEN ColorScheme, THEN THE Theme_Engine SHALL fall back to the hardcoded AppColorScheme values defined in criterion 1, so that the application never renders with Material3 default purple colors.

### Requirement 5: DARK_NEON Color Palette

**User Story:** As a user, I want a dark mode option with neon accents, so that I can use the application comfortably in low-light environments.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define the DARK_NEON ColorScheme as a Material3 darkColorScheme with background color #121212, surface color #1E1E1E, primary color neon green (#39FF14), and secondary color cyan (#00FFFF).
2. THE DARK_NEON ColorScheme SHALL assign the primary color (#39FF14) to confirm-action buttons and navigation-selected indicators, and the secondary color (#00FFFF) to informational accents and input-field borders.
3. THE DARK_NEON ColorScheme SHALL provide a minimum contrast ratio of 4.5:1 between normal-size text and its immediate background, and a minimum contrast ratio of 3:1 between large text and its immediate background, for every text-role/background-role pairing (onBackground/#121212, onSurface/#1E1E1E, onPrimary/#39FF14, onSecondary/#00FFFF).
4. THE DARK_NEON ColorScheme SHALL retain the error color (#E53935) for cancel and delete buttons so that destructive actions remain visually distinct from accent-colored elements.

### Requirement 6: OCEAN_BLUE Color Palette

**User Story:** As a user, I want a clean blue-themed light mode, so that I have a professional alternative to the default green.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define the OCEAN_BLUE ColorScheme as a Material3 lightColorScheme with the following slot assignments: primary set to Royal Blue (#1565C0), secondary set to Light Blue (#42A5F5), background and surface set to white (#FFFFFF), primaryContainer set to a light blue tint, and error remaining the existing error red.
2. THE OCEAN_BLUE ColorScheme SHALL provide a minimum contrast ratio of 4.5:1 between each onX color and its corresponding container or surface color (onPrimary over primary, onSecondary over secondary, onBackground over background, onSurface over surface).
3. IF the OCEAN_BLUE ColorScheme is active, THEN THE Theme_Engine SHALL use the secondary color (#42A5F5) for interactive elements including action buttons, selected navigation indicators, and input field focus borders.

### Requirement 7: SUNSET_ORANGE Color Palette

**User Story:** As a user, I want a warm orange-themed light mode, so that I have a visually comfortable alternative for extended use.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define the SUNSET_ORANGE ColorScheme as a Material3 lightColorScheme assigning at minimum the following roles: primary (#E65100), onPrimary (#FFFFFF), primaryContainer (#FFF3E0), onPrimaryContainer (#BF360C), secondary (#FFC107), onSecondary (#3E2723), background (#FFFBF5), onBackground (#1A1A1A), surface (#FFFFFF), onSurface (#1A1A1A), error (#B71C1C), and onError (#FFFFFF).
2. THE SUNSET_ORANGE ColorScheme SHALL maintain a minimum contrast ratio of 4.5:1 between every onX color token and its corresponding container or background token (e.g., onPrimary over primary, onBackground over background, onSurface over surface).
3. IF the SUNSET_ORANGE ColorScheme is selected, THEN THE Theme_Engine SHALL apply it to all interactive elements (buttons, text fields, navigation rail icons) using the same Material3 role mappings as the default AppColorScheme, substituting only the color values.

### Requirement 8: Theme Selector Screen Layout

**User Story:** As a user, I want a visual grid of theme options in the settings area, so that I can easily browse and compare available themes.

#### Acceptance Criteria

1. THE Theme_Selector_Screen SHALL display a LazyVerticalGrid with GridCells.Fixed(2) containing exactly 4 theme cards, one for each AppTheme value.
2. THE Theme_Selector_Screen SHALL display the title "Apariencia" as the first element above the grid.
3. WHEN the user taps a navigation element labeled "Apariencia" within the Settings area, THE application SHALL navigate to the Theme_Selector_Screen.
4. THE Theme_Selector_Screen SHALL display each theme card with the theme name and a representative color preview, allowing the user to visually distinguish between available themes.
5. THE Theme_Selector_Screen SHALL visually indicate the currently active theme by displaying a distinct selected state on the corresponding card.

### Requirement 9: Theme Card Design

**User Story:** As a user, I want each theme card to show a visual preview of its colors and name, so that I can make an informed choice before selecting a theme.

#### Acceptance Criteria

1. THE Theme_Selector_Screen SHALL display each theme card with the theme display name in Spanish (Verde por Defecto, Neón Oscuro, Océano Azul, Atardecer Naranja) as visible text within the card.
2. THE Theme_Selector_Screen SHALL display exactly 3 color swatches within each theme card, representing the primary, background, and accent colors from the respective ColorScheme, rendered as filled circles or rounded rectangles arranged in a horizontal row.
3. IF a theme card represents the currently active theme, THEN THE Theme_Selector_Screen SHALL display a visible border with a distinct color differentiating it from non-selected cards, and a check icon overlay on that card to indicate selection state.
4. IF a theme card does not represent the currently active theme, THEN THE Theme_Selector_Screen SHALL display the card without a highlighted border and without a check icon.

### Requirement 10: Theme Selection Interaction

**User Story:** As a user, I want to select a theme by tapping its card, so that the change takes effect immediately and is saved for future sessions.

#### Acceptance Criteria

1. WHEN the user taps a theme card, THE Theme_Selector_Screen SHALL dispatch a theme change event to the Theme_ViewModel.
2. WHEN the Theme_ViewModel receives a theme change event, THE Theme_ViewModel SHALL update the Theme_Preferences_Repository with the new AppTheme value.
3. WHEN a theme change event is processed, THE Root_Composable SHALL apply the new ColorScheme within the same frame or next recomposition cycle without requiring app restart or screen navigation.

### Requirement 11: DataStore Dependency Integration

**User Story:** As a developer, I want Preferences DataStore added as a project dependency, so that the theme persistence layer has the required library available.

#### Acceptance Criteria

1. THE version catalog (libs.versions.toml) SHALL declare an `androidx.datastore-preferences` library entry with a pinned version.
2. THE app-level build configuration SHALL include the `androidx.datastore-preferences` library as an `implementation` dependency using the version catalog alias.
3. WHEN the dependency is added, THE project SHALL compile successfully without unresolved import errors for `androidx.datastore.preferences` packages.
4. THE Theme_Preferences_Repository SHALL use Preferences DataStore (not SharedPreferences) as the persistence mechanism for theme preference.
