# Implementation Plan: 01 Main Menu

## Overview

This plan covers all implementation tasks for the **01 Main Menu** feature (Base Screen Structure: green background, global color palette, persistent Nav Rail, "+" Add Card, and delete-from-modal). All 14 tasks are complete. Each task maps to a single atomic Git commit and references the acceptance criteria from `requirements.md`.

**Version:** 1.2  
**Status:** ✅ COMPLETED  
**Last updated:** 2026-07-06  
**Changelog:** v1.1 — Added T-11 (PrinterScreen); updated T-03, T-04, T-08, T-09, T-10 to reflect 5th destination  
**Changelog:** v1.2 — Added T-12 (ButtonDelete token), T-13 (delete button in modal), T-14 (ViewModel wire-up); updated T-10 QA checklist

---

## Atomic Commit Rule

> **Every task below must be completed in a single, self-contained Git commit.**
>
> - The commit must compile and run without errors before it is pushed.
> - Commit messages follow the Conventional Commits format:
>   `<type>(<scope>): <short description>`  
>   Example: `feat(theme): add global color palette tokens to Color.kt`
> - One task = one commit. Do not bundle multiple tasks into one commit.
> - This ensures each step is independently traceable, revertable, and reviewable.

---

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
    }
  ]
}
```

Sequential dependency chain for reference:

```
T-01 (Color.kt)
  └── T-02 (Theme.kt)
        └── T-03 (NavDestination.kt)
              └── T-04 (AppNavRail.kt)
                    └── T-05 (MainActivity shell)
                          └── T-06 (AddMenuCard.kt)
                                └── T-07 (HomeScreen.kt)
                                      └── T-08 (Placeholder screens — Stats, Settings, Tickets)
                                            └── T-11 (PrinterScreen.kt)
                                                  └── T-09 (Wire all screens into MainActivity)
                                                        ├── T-10 (Smoke test & verify)
                                                        └── T-12 (ButtonDelete color token)
                                                              └── T-13 (Delete button in AddMenuDialog)
                                                                    └── T-14 (Wire deleteMenu in HomeScreen)
                                                                          └── T-10 (updated QA checklist)
```

---

## Tasks

- [x] 1. Define global color palette

  **Scope:** `ui/theme/Color.kt`  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(theme): add global color palette tokens to Color.kt`

  **What to do:**
  - Replace all default Material purple tokens with the brand palette defined in `design.md`.
  - Define every named token: `BackgroundPrimary`, `NavRailBackground`, `NavRailIconDefault`, `NavRailIconSelected`, `CardBackground`, `CardText`, `CardIconTint`, and all modal/button/input tokens.
  - No token may be left referencing a purple or default Material color.

  **Acceptance check (maps to AC-02.1, AC-02.4):**
  - [x] 1.1 `Color.kt` contains zero references to `Purple`, `Pink`, or `PurpleGrey`.
  - [x] 1.2 Every token has an inline comment with its hex value and purpose.
  - [x] 1.3 The file compiles cleanly (`./gradlew compileDebugKotlin`).

- [x] 2. Wire color palette into MaterialTheme

  **Scope:** `ui/theme/Theme.kt`  
  **Depends on:** T-01  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(theme): wire brand palette into MaterialTheme, disable dynamicColor`

  **What to do:**
  - Remove `darkColorScheme`, `dynamicDarkColorScheme`, `dynamicLightColorScheme` imports and usages.
  - Create a single `AppColorScheme = lightColorScheme(...)` constant using tokens from `Color.kt`.
  - Map: `primary → NavRailIconSelected`, `background → BackgroundPrimary`, `surface → ModalSurface`, `error → ButtonCancel`, `onPrimary → CardText`, `onBackground → CardText`, `onSurface → ModalTitleText`.
  - Simplify `PuntoDeVentaTheme` to accept only `content` — remove `darkTheme` and `dynamicColor` parameters.

  **Acceptance check (maps to AC-02.2, AC-02.3):**
  - [x] 2.1 No `dynamicColor` or dark theme branch exists in `Theme.kt`.
  - [x] 2.2 The app background renders `#6BBF3E` when run on an Android 12+ emulator.
  - [x] 2.3 `./gradlew compileDebugKotlin` passes with no warnings about unused imports.

- [x] 3. Create navigation destination model

  **Scope:** `ui/navigation/NavDestination.kt`  
  **Depends on:** T-02  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(navigation): add NavDestination sealed class with 5 destinations`

  **What to do:**
  - Create the file `ui/navigation/NavDestination.kt`.
  - Define `sealed class NavDestination(val route: String, val label: String)` with **five** `object` members: `Home("home","Inicio")`, `Stats("stats","Estadísticas")`, `Settings("settings","Configuración")`, `Tickets("tickets","Tickets")`, `Printer("printer","Impresora")`.

  **Acceptance check (maps to AC-03.2, AC-03.8, AC-03.10):**
  - [x] 3.1 All five destinations are defined and compile.
  - [x] 3.2 `Printer` is the last declared object in the sealed class.
  - [x] 3.3 No string literals for destination names exist outside this file.

- [x] 4. Build the persistent navigation rail composable

  **Scope:** `ui/navigation/AppNavRail.kt`  
  **Depends on:** T-03  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(navigation): implement AppNavRail with 5 destinations and brand colors`

  **What to do:**
  - Create `AppNavRail(currentDestination: NavDestination, onDestinationSelected: (NavDestination) -> Unit)`.
  - Use `NavigationRail` with `containerColor = NavRailBackground`.
  - Map all five `NavDestination` objects to `NavigationRailItem` entries with their icons.
  - Apply `NavRailIconSelected` tint when selected, `NavRailIconDefault` when not.
  - Suppress the Material3 indicator pill by setting `indicatorColor = NavRailBackground`.

  **Acceptance check (maps to AC-03.1–AC-03.12):**
  - [x] 4.1 Rail surface color is `#F5F0E8`, not white.
  - [x] 4.2 All five items are visible; Impresora is the last one.
  - [x] 4.3 Tapping each item triggers `onDestinationSelected` with the correct destination.
  - [x] 4.4 Selected icon/label are `#4A8C1C`; unselected are `#2D2D2D`.
  - [x] 4.5 No indicator pill is visible.

- [x] 5. Refactor MainActivity to host the app shell

  **Scope:** `MainActivity.kt`  
  **Depends on:** T-04  
  **Commit type:** `refactor`  
  **Atomic commit message:** `refactor(main): replace Hello World scaffold with Row shell hosting NavRail`

  **What to do:**
  - Remove `Greeting` composable and `GreetingPreview`.
  - Replace the `Scaffold` with a `Row(fillMaxSize, background = BackgroundPrimary)`.
  - Instantiate `var currentDestination by remember { mutableStateOf(NavDestination.Home) }`.
  - Render `AppNavRail` as the first child of the `Row`.
  - Add a placeholder `Box` as the second child.
  - Keep `enableEdgeToEdge()`.

  **Acceptance check (maps to AC-01.1, AC-03.1, AC-03.9):**
  - [x] 5.1 App launches to a green screen with the cream nav rail on the left.
  - [x] 5.2 No "Hello Android!" text is visible.
  - [x] 5.3 Tapping nav items does not crash.

- [x] 6. Create the AddMenuCard composable

  **Scope:** `ui/home/AddMenuCard.kt`  
  **Depends on:** T-05  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(home): implement AddMenuCard with dark green background and + icon`

  **What to do:**
  - Create `AddMenuCard(onClick: () -> Unit, modifier: Modifier)`.
  - `200.dp × 200.dp` square, `RoundedCornerShape(12.dp)`, background `CardBackground`.
  - Center: `Icons.Default.AddCircle` at `80.dp`, tint `CardIconTint`.
  - Below icon: `"NOMBRE...."` label, `FontWeight.Bold`, 14.sp, color `CardText`.
  - Wire `clickable(onClick = onClick)`.

  **Acceptance check (maps to AC-04.2, AC-04.3, AC-04.4):**
  - [x] 6.1 Card is exactly `200×200 dp` with rounded corners.
  - [x] 6.2 Icon is white and centered; label "NOMBRE...." appears below it in white bold.
  - [x] 6.3 Card is tappable.

- [x] 7. Create the HomeScreen with LazyVerticalGrid

  **Scope:** `ui/home/HomeScreen.kt`  
  **Depends on:** T-06  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(home): implement HomeScreen with LazyVerticalGrid and AddMenuCard as last item`

  **What to do:**
  - Create `HomeScreen(viewModel: HomeViewModel = viewModel())`.
  - Collect `uiState` via `collectAsStateWithLifecycle()`.
  - Render a `LazyVerticalGrid(GridCells.Adaptive(200.dp))` with 16.dp padding and spacing.
  - List `MenuItemCard` for each item in `uiState.menuItems`.
  - Always append `AddMenuCard` as the last grid item.
  - Background of the surrounding `Box` = `BackgroundPrimary`.

  **Acceptance check (maps to AC-04.1, AC-04.5, AC-04.6):**
  - [x] 7.1 Green background fills the entire content area.
  - [x] 7.2 The single AddMenuCard is rendered in the grid.
  - [x] 7.3 Grid padding is visually ~16 dp on all sides.

- [x] 8. Create placeholder screens for Stats, Settings, Tickets

  **Scope:** `ui/stats/StatsScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/tickets/TicketsScreen.kt`  
  **Depends on:** T-07  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(screens): add placeholder composables for Stats, Settings, and Tickets`

  **What to do:**
  - Each screen is a `Box(fillMaxSize, background = BackgroundPrimary)` with a centered `Text` label.
  - Use `CardText` for text color, `FontWeight.Bold`, 24.sp.

  **Acceptance check:**
  - [x] 8.1 All three files compile without errors.
  - [x] 8.2 Each screen shows its label centered on a green background.

- [x] 9. Wire all screens into the MainActivity shell

  **Scope:** `MainActivity.kt`  
  **Depends on:** T-11  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(main): wire all 5 screens including PrinterScreen into MainActivity nav shell`

  **What to do:**
  - Replace the placeholder `Box` with a `when(currentDestination)` expression rendering all five screens.

  **Acceptance check (maps to AC-01.1, AC-03.4, AC-03.8, AC-03.9, AC-03.11):**
  - [x] 9.1 App opens on the HomeScreen.
  - [x] 9.2 Tapping each of the five Nav Rail items switches the content area to the correct screen.
  - [x] 9.3 Nav Rail remains visible on all five screens.
  - [x] 9.4 Tapping Impresora shows `PrinterScreen` with `"IMPRESORA"` centered.
  - [x] 9.5 Back button does not crash the app.

- [x] 10. Smoke test and layout verification

  **Scope:** Manual QA  
  **Depends on:** T-14  
  **Commit type:** `test`  
  **Atomic commit message:** `test(main): verify full layout, 5 nav destinations, and delete flow`

  **Checklist:**

  | Check | Passes? |
  |---|---|
  | App launches directly to HomeScreen (no splash) | ✅ |
  | Nav Rail is cream (#F5F0E8) and full height | ✅ |
  | Exactly 5 items visible in the Nav Rail | ✅ |
  | Inactive icons are dark (#2D2D2D) | ✅ |
  | Inicio icon is dark green (#4A8C1C) on launch | ✅ |
  | Background behind cards is bright green (#6BBF3E) | ✅ |
  | AddMenuCard is 200×200, dark green, white icon | ✅ |
  | Label "NOMBRE...." is white and bold | ✅ |
  | Tapping each Nav Rail item shows the correct screen | ✅ |
  | Tapping Impresora shows "IMPRESORA" centered in green | ✅ |
  | Nav Rail is visible on all 5 screens | ✅ |
  | Tapping "+" opens modal WITHOUT delete button | ✅ |
  | Tapping pencil on existing card opens modal WITH delete button | ✅ |
  | Delete button is deep red (#B71C1C) with trash icon and "ELIMINAR" label | ✅ |
  | Tapping delete removes card from grid and closes modal | ✅ |
  | No hardcoded colors exist outside Color.kt | ✅ |
  | `./gradlew testDebugUnitTest` passes | ✅ |

- [x] 11. Create PrinterScreen placeholder

  **Scope:** `ui/printer/PrinterScreen.kt`  
  **Depends on:** T-08  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(screens): add PrinterScreen placeholder with IMPRESORA label`

  **What to do:**
  - Create `ui/printer/PrinterScreen.kt`.
  - `Box(fillMaxSize, background = BackgroundPrimary)` with a centered `Text("IMPRESORA")`.
  - Match the same typography and color as the other placeholder screens: `CardText`, `FontWeight.Bold`, 24.sp.

  **Acceptance check (maps to AC-03.11, AC-03.12):**
  - [x] 11.1 File compiles without errors.
  - [x] 11.2 Screen displays `"IMPRESORA"` in white bold text centered on a green background.

- [x] 12. Add ButtonDelete color tokens to Color.kt

  **Scope:** `ui/theme/Color.kt`  
  **Depends on:** T-09  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(theme): add ButtonDelete and ButtonDeleteText color tokens to Color.kt`

  **What to do:**
  - Add `ButtonDelete = Color(0xFFB71C1C)` and `ButtonDeleteText = Color(0xFFFFFFFF)` under Action Buttons.

  **Acceptance check (maps to AC-06.8, AC-02.1):**
  - [x] 12.1 Both tokens appear in `Color.kt` with inline comments.
  - [x] 12.2 No inline color literals for the delete button exist anywhere else.
  - [x] 12.3 `./gradlew compileDebugKotlin` passes.

- [x] 13. Add delete button to AddMenuDialog

  **Scope:** `ui/home/AddMenuDialog.kt`  
  **Depends on:** T-12  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(home): add conditional delete button to AddMenuDialog for edit mode`

  **What to do:**
  - Add `onDelete: (() -> Unit)?` parameter (null in create mode).
  - Render delete button above GUARDAR/DESCARTAR only when `onDelete != null`.

  **Acceptance check (maps to AC-06.1, AC-06.2, AC-06.3, AC-06.6, AC-06.7):**
  - [x] 13.1 Delete button is not rendered when `onDelete` is null.
  - [x] 13.2 Delete button is visible and full-width when `onDelete` is non-null.
  - [x] 13.3 Button uses `ButtonDelete` background and `ButtonDeleteText` text color.
  - [x] 13.4 Button shows trash icon + "ELIMINAR" label.
  - [x] 13.5 Delete button row appears above the GUARDAR/DESCARTAR row.

- [x] 14. Wire deleteMenu into HomeScreen and modal

  **Scope:** `ui/home/HomeScreen.kt`  
  **Depends on:** T-13  
  **Commit type:** `feat`  
  **Atomic commit message:** `feat(home): wire deleteMenu ViewModel call through HomeScreen to AddMenuDialog`

  **What to do:**
  - Pass `onDelete = null` in create mode; pass delete lambda in edit mode calling `viewModel.deleteMenu(id)` + `viewModel.dismissDialog()`.

  **Acceptance check (maps to AC-06.4, AC-06.5):**
  - [x] 14.1 Tapping delete removes exactly that card from the grid.
  - [x] 14.2 Modal closes immediately after deletion.
  - [x] 14.3 Grid re-renders without the deleted card; AddMenuCard remains as last item.
  - [x] 14.4 No other cards are affected.

---

## Summary Table

| Task | File(s) | Type | Depends On | Status |
|---|---|---|---|---|
| T-01 | `Color.kt` | feat | — | ✅ Done |
| T-02 | `Theme.kt` | feat | T-01 | ✅ Done |
| T-03 | `NavDestination.kt` | feat | T-02 | ✅ Done |
| T-04 | `AppNavRail.kt` | feat | T-03 | ✅ Done |
| T-05 | `MainActivity.kt` | refactor | T-04 | ✅ Done |
| T-06 | `AddMenuCard.kt` | feat | T-05 | ✅ Done |
| T-07 | `HomeScreen.kt` | feat | T-06 | ✅ Done |
| T-08 | `StatsScreen.kt` · `SettingsScreen.kt` · `TicketsScreen.kt` | feat | T-07 | ✅ Done |
| T-11 | `PrinterScreen.kt` | feat | T-08 | ✅ Done |
| T-09 | `MainActivity.kt` | feat | T-11 | ✅ Done |
| T-12 | `Color.kt` | feat | T-09 | ✅ Done |
| T-13 | `AddMenuDialog.kt` | feat | T-12 | ✅ Done |
| T-14 | `HomeScreen.kt` | feat | T-13 | ✅ Done |
| T-10 | Manual QA | test | T-14 | ✅ Done |

---

## Notes

All 14 tasks were completed as of 2026-07-06 (v1.2). The feature is fully implemented and verified via the smoke-test checklist in T-10. No outstanding issues remain. Future work (printer integration, persistence, dialog create/edit flow) is tracked in separate feature specs.
