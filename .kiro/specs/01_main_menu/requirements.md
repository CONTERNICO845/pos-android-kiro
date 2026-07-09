# Requirements Document

## Introduction

This document defines the requirements for the **01 Main Menu** feature of the PuntoDeVenta (POS) Android application. The feature establishes the base screen structure: a full-screen green background, a single source-of-truth color palette, a persistent five-item navigation rail on the left edge, and an initial "+" Add Card in the main content area. It also covers the delete-from-edit-modal capability added in v1.2.

**Version:** 1.2  
**Status:** Updated  
**Last updated:** 2026-07-06  
**Changelog:** v1.1 — Added US-03 update: Nav Rail now has 5 destinations (Impresora added at bottom)  
**Changelog:** v1.2 — Added US-06: Delete menu item from edit modal

---

## Glossary

| Term | Definition |
|---|---|
| Nav Rail | The static vertical `NavigationRail` component pinned to the left edge of the screen, always visible |
| Add Card | The single dark-green square card displaying a large white "+" icon and the label "NOMBRE...." |
| Global Palette | The single file (`Color.kt`) that is the only permitted source of color values in the entire codebase |
| Active Destination | The Nav Rail item currently selected; its icon and label adopt the `NavRailIconSelected` color |
| POS App | PuntoDeVenta Android application running on tablet (landscape primary) and phone (portrait supported) |

---

## Requirements

---

### US-01 — App launch presents the main screen immediately

**User Story:**  
As a POS operator, I want the app to open directly on the main menu screen, so that I can start working without navigating through a splash or login screen.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-01.1 | **When** the user launches the POS app, **the system shall** display the main menu screen as the first and only visible screen. |
| AC-01.2 | **When** the main menu screen is displayed, **the system shall** render the entire window background using the `BackgroundPrimary` color token (`#6BBF3E`). |
| AC-01.3 | **When** the main menu screen is displayed, **the system shall** not show any splash screen, loading spinner, or intermediate navigation step before the main content. |

---

### US-02 — Global color palette is the single source of truth

**User Story:**  
As a developer, I want all colors to be defined in one central file, so that the entire app's color scheme can be changed by editing a single location.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-02.1 | **Where** a color value is needed anywhere in the codebase, **the system shall** reference a named token from `ui/theme/Color.kt` and never use an inline `Color(0x...)` literal outside that file. |
| AC-02.2 | **When** a developer changes a color token value in `Color.kt`, **the system shall** propagate that change to every composable that references the token without requiring any other file edits. |
| AC-02.3 | **Where** the `PuntoDeVentaTheme` is applied, **the system shall** set `dynamicColor = false` so that Android 12+ Material You overrides do not replace the brand palette. |
| AC-02.4 | **When** the app is built in any variant (debug or release), **the system shall** compile without any hardcoded color literals outside `Color.kt`. |

---

### US-03 — Static left navigation rail is always visible

**User Story:**  
As a POS operator, I want a permanent navigation rail on the left side of the screen with five destinations, so that I can switch sections at any time without losing context.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-03.1 | **When** any screen of the app is displayed, **the system shall** render the Nav Rail flush against the left edge of the window at full screen height. |
| AC-03.2 | **When** the Nav Rail is rendered, **the system shall** display exactly five destinations in top-to-bottom order: **Inicio**, **Estadísticas**, **Configuración**, **Tickets**, and **Impresora**. |
| AC-03.3 | **When** the Nav Rail is rendered, **the system shall** display each destination with its corresponding icon and a text label beneath it. |
| AC-03.4 | **When** the user taps a Nav Rail destination that is not currently active, **the system shall** update the main content area to show the corresponding screen within the same activity without relaunching. |
| AC-03.5 | **When** a Nav Rail destination is active, **the system shall** tint its icon and label with the `NavRailIconSelected` color token (`#4A8C1C`). |
| AC-03.6 | **When** a Nav Rail destination is inactive, **the system shall** tint its icon and label with the `NavRailIconDefault` color token (`#2D2D2D`). |
| AC-03.7 | **When** the Nav Rail is rendered, **the system shall** use `NavRailBackground` (`#F5F0E8`) as its surface color, visually distinguishing it from the main content area. |
| AC-03.8 | **When** the app launches, **the system shall** default the active Nav Rail destination to **Inicio**. |
| AC-03.9 | **While** the user is on any destination, **the system shall** keep the Nav Rail visible and interactive (it shall never be hidden, collapsed, or overlaid). |
| AC-03.10 | **When** the Nav Rail is rendered, **the system shall** position the **Impresora** item as the fifth and last item, directly below **Tickets**. |
| AC-03.11 | **When** the user taps **Impresora** in the Nav Rail, **the system shall** display the `PrinterScreen` composable in the main content area, showing the text `"IMPRESORA"` centered on a `BackgroundPrimary` green background. |
| AC-03.12 | **When** the **Impresora** destination is rendered, **the system shall** display it using the `Icons.Default.Print` Material Design icon. |

---

### US-04 — Main content area displays the initial "+" Add Card

**User Story:**  
As a POS operator, I want to see a single prominent card with a "+" icon on the main screen when no menu items exist, so that I know exactly how to create my first menu.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-04.1 | **When** the Inicio destination is active and no menu items have been saved, **the system shall** display exactly one card — the Add Card — in the main content area. |
| AC-04.2 | **When** the Add Card is displayed, **the system shall** render it as a `200 dp × 200 dp` square with `RoundedCornerShape(12.dp)` and `CardBackground` fill (`#2D5A1B`). |
| AC-04.3 | **When** the Add Card is displayed, **the system shall** center a white `AddCircle` icon of size `80 dp` (tinted `CardIconTint`, `#FFFFFF`) within the card. |
| AC-04.4 | **When** the Add Card is displayed, **the system shall** render the label `"NOMBRE...."` in `FontWeight.Bold` with `CardText` color (`#FFFFFF`) below the icon. |
| AC-04.5 | **When** the main content area is rendered, **the system shall** use `BackgroundPrimary` (`#6BBF3E`) as the background behind all cards. |
| AC-04.6 | **When** one or more menu items exist, **the system shall** always render the Add Card as the last item in the card grid, after all saved menu cards. |
| AC-04.7 | **When** the Add Card is displayed, **the system shall** respond to a tap event by opening the Add Menu Dialog (defined in feature 02). |

---

### US-05 — Layout adapts to tablet landscape and phone portrait

**User Story:**  
As a POS operator using either a tablet or a phone, I want the main layout to remain usable in both orientations, so that the app works on the hardware I have available.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-05.1 | **When** the device is in landscape orientation, **the system shall** display the Nav Rail and the card grid side by side with no overlap. |
| AC-05.2 | **When** the device is in portrait orientation, **the system shall** display the Nav Rail on the left and the card grid occupying the remaining width, with no content clipped. |
| AC-05.3 | **When** the screen width results in fewer than two columns being 200 dp wide, **the system shall** display a single-column card grid while keeping the Nav Rail visible. |

---

### US-06 — Delete a menu item from the edit modal *(new in v1.2)*

**User Story:**  
As a POS operator, I want a delete button inside the edit modal so that I can remove an existing menu card without having to navigate elsewhere.

#### Acceptance Criteria

| ID | EARS Statement |
|---|---|
| AC-06.1 | **When** the user opens the modal by tapping the pencil icon on an existing menu card, **the system shall** display a "ELIMINAR" delete button inside the modal. |
| AC-06.2 | **When** the user opens the modal by tapping the "+" Add Card, **the system shall** hide the delete button entirely (it must not be visible or occupy space). |
| AC-06.3 | **When** the delete button is displayed, **the system shall** render it using the `ButtonDelete` color token and a trash-can icon (`Icons.Default.Delete`). |
| AC-06.4 | **When** the user taps the delete button, **the system shall** immediately remove the corresponding menu card from the grid. |
| AC-06.5 | **When** the delete action is confirmed, **the system shall** close the modal and return the user to the main grid with the card no longer present. |
| AC-06.6 | **When** the delete button is rendered, **the system shall** use `ButtonDeleteText` as the label color and display the label `"ELIMINAR"` in `FontWeight.Bold`. |
| AC-06.7 | **When** the modal is in edit mode and the delete button is displayed, **the system shall** position it as a full-width button in a separate row above the GUARDAR / DESCARTAR button row. |
| AC-06.8 | **Where** the `ButtonDelete` and `ButtonDeleteText` color tokens are used, **the system shall** source them exclusively from `Color.kt` — no inline color literals. |

---

## Out of Scope (for this feature)

- Add/Edit Menu Dialog creation flow (→ feature `02_add_menu_dialog`)
- Stats, Settings, Tickets, and Impresora screen content (placeholder screens only)
- Data persistence (Room, DataStore)
- Network calls
- Authentication or login flow
- Actual printer integration or Bluetooth/WiFi printing logic (→ future feature `05_printer_integration`)
- Undo / redo for deleted items
- Confirmation dialog before deletion (intentional — single tap deletes immediately in this phase)
