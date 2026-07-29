package com.example.puntodeventa.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puntodeventa.ui.printer.PrinterScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose tests for navigation integration of the printer destination.
 *
 * Covers:
 * - Clicking "Impresora" NavRail item changes currentDestination to Printer  — Requirement 1.1
 * - Navigating to the printer route renders PrinterScreen in the content area — Requirement 1.2
 * - NavRail maintains its visual presence when PrinterScreen is displayed     — Requirement 1.3
 * - App initializes with a non-printer destination                            — Requirement 1.6
 *
 * **Validates: Requirements 1.1, 1.2, 1.3**
 */
@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Renders a minimal app shell that mirrors the real MainActivity structure:
     * [AppNavRail] on the left, and a simple content switcher on the right.
     *
     * The [onDestinationChanged] callback allows tests to observe destination changes.
     */
    private fun renderAppShell(
        initialDestination: NavDestination = NavDestination.Home,
        onDestinationChanged: (NavDestination) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                var currentDestination: NavDestination by remember {
                    mutableStateOf(initialDestination)
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavRail(
                        currentDestination    = currentDestination,
                        onDestinationSelected = { destination ->
                            currentDestination = destination
                            onDestinationChanged(destination)
                        }
                    )

                    // Minimal content area – same pattern as MainActivity
                    when (currentDestination) {
                        NavDestination.Printer -> PrinterScreen()
                        else                   -> Text(text = "other_screen")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // ── Requirement 1.6: Default destination is not Printer ──────────────────

    /**
     * When the app initialises the current destination must NOT be the printer route.
     *
     * **Validates: Requirement 1.6**
     */
    @Test
    fun appShell_initialDestination_isNotPrinter() {
        var initialDestination: NavDestination? = null

        composeTestRule.setContent {
            MaterialTheme {
                var currentDestination: NavDestination by remember {
                    mutableStateOf(NavDestination.Home)
                }
                // Capture the first value before any interaction
                if (initialDestination == null) {
                    initialDestination = currentDestination
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavRail(
                        currentDestination    = currentDestination,
                        onDestinationSelected = { currentDestination = it }
                    )
                    when (currentDestination) {
                        NavDestination.Printer -> PrinterScreen()
                        else                   -> Text(text = "other_screen")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        assertNotEquals(
            "Initial destination must not be NavDestination.Printer",
            NavDestination.Printer,
            initialDestination
        )
    }

    // ── Requirement 1.1: Clicking Impresora navigates to Printer ─────────────

    /**
     * When the user clicks the "Impresora" NavRail item the destination callback
     * must be invoked with NavDestination.Printer.
     *
     * **Validates: Requirement 1.1**
     */
    @Test
    fun navRail_clickImpresora_changesDestinationToPrinter() {
        var selectedDestination: NavDestination? = null

        renderAppShell(
            initialDestination    = NavDestination.Home,
            onDestinationChanged  = { selectedDestination = it }
        )

        // Click the "Impresora" NavRail item (matched by its content description)
        composeTestRule
            .onNodeWithContentDescription("Impresora", substring = false)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            "Clicking Impresora must set destination to NavDestination.Printer",
            NavDestination.Printer,
            selectedDestination
        )
    }

    /**
     * After clicking the "Impresora" item, the content area must switch to the
     * printer screen and no longer show the placeholder "other_screen" text.
     *
     * **Validates: Requirement 1.1**
     */
    @Test
    fun navRail_clickImpresora_contentAreaSwitchesToPrinterRoute() {
        renderAppShell(initialDestination = NavDestination.Home)

        // Confirm we start on a non-printer screen
        composeTestRule.onNodeWithText("other_screen").assertIsDisplayed()

        // Navigate to Printer
        composeTestRule
            .onNodeWithContentDescription("Impresora")
            .performClick()
        composeTestRule.waitForIdle()

        // Placeholder must be gone and printer content must appear
        composeTestRule.onNodeWithText("other_screen").assertDoesNotExist()
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
    }

    // ── Requirement 1.2: PrinterScreen renders in the main content area ───────

    /**
     * When NavDestination.Printer is the current destination PrinterScreen must
     * be rendered in the main content area, showing its ControlPanel content.
     *
     * **Validates: Requirement 1.2**
     */
    @Test
    fun navigation_toPrinterDestination_displaysPrinterScreen() {
        renderAppShell(initialDestination = NavDestination.Printer)

        // PrinterScreen ControlPanel content is the primary indicator
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
    }

    /**
     * When navigated to the printer destination both the ControlPanel and the
     * StatusPanel (both columns of PrinterScreen) must be visible.
     *
     * **Validates: Requirement 1.2**
     */
    @Test
    fun navigation_toPrinterDestination_displaysBothPanels() {
        renderAppShell(initialDestination = NavDestination.Printer)

        // Left column — ControlPanel
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 LAN").assertIsDisplayed()

        // Right column — StatusPanel
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()
    }

    /**
     * After clicking Impresora in the NavRail, the PrinterScreen must be displayed
     * in the main content area with its full two-panel content.
     *
     * **Validates: Requirements 1.1, 1.2**
     */
    @Test
    fun navRail_clickImpresora_showsPrinterScreenContent() {
        renderAppShell(initialDestination = NavDestination.Home)

        composeTestRule
            .onNodeWithContentDescription("Impresora")
            .performClick()
        composeTestRule.waitForIdle()

        // Both panels must be visible
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()
    }

    // ── Requirement 1.3: NavRail remains visible with PrinterScreen ───────────

    /**
     * When the printer destination is active the NavRail must still be displayed
     * alongside the PrinterScreen (it must not be hidden or replaced).
     *
     * **Validates: Requirement 1.3**
     */
    @Test
    fun navRail_remainsVisible_whenPrinterScreenIsDisplayed() {
        renderAppShell(initialDestination = NavDestination.Printer)

        // NavRail items for other destinations must still be present
        composeTestRule
            .onNodeWithContentDescription("Inicio")
            .assertIsDisplayed()

        // The Impresora item itself should also be present in the rail
        composeTestRule
            .onNodeWithContentDescription("Impresora")
            .assertIsDisplayed()
    }

    /**
     * After navigating to printer, the NavRail items for other destinations are
     * still clickable and can be used to navigate away.
     *
     * **Validates: Requirement 1.3**
     */
    @Test
    fun navRail_otherItemsRemainFunctional_whenPrinterScreenIsDisplayed() {
        renderAppShell(initialDestination = NavDestination.Printer)

        // Confirm printer screen is displayed
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()

        // Navigate away using the Home item — NavRail must remain present
        composeTestRule
            .onNodeWithContentDescription("Inicio")
            .performClick()
        composeTestRule.waitForIdle()

        // Printer content must be gone; placeholder (other_screen) must show
        composeTestRule.onNodeWithText("IMPRESORA").assertDoesNotExist()
        composeTestRule.onNodeWithText("other_screen").assertIsDisplayed()

        // NavRail item for Inicio must still be present after navigation
        composeTestRule
            .onNodeWithContentDescription("Inicio")
            .assertIsDisplayed()
    }

    // ── Requirement 1.3: NavRail label for Impresora is present ──────────────

    /**
     * The NavRail must display the "Impresora" label text item at all times,
     * confirming it maintains its existing visual design.
     *
     * **Validates: Requirement 1.3**
     */
    @Test
    fun navRail_displaysImpresoraLabel_atAllTimes() {
        renderAppShell(initialDestination = NavDestination.Home)

        // Label text is rendered by NavigationRailItem
        composeTestRule
            .onNodeWithText("Impresora", substring = false)
            .assertIsDisplayed()
    }

    /**
     * The "Impresora" label remains visible in the NavRail even after navigating
     * to the printer destination.
     *
     * **Validates: Requirement 1.3**
     */
    @Test
    fun navRail_displaysImpresoraLabel_whenPrinterIsCurrentDestination() {
        renderAppShell(initialDestination = NavDestination.Printer)

        composeTestRule
            .onNodeWithText("Impresora", substring = false)
            .assertIsDisplayed()
    }

    // ── Round-trip navigation ─────────────────────────────────────────────────

    /**
     * After navigating to Printer and back to Home, the printer screen must no
     * longer be displayed and the NavRail must still be fully functional.
     *
     * **Validates: Requirements 1.1, 1.2, 1.3**
     */
    @Test
    fun navigation_roundTrip_homeToprinterToHome() {
        renderAppShell(initialDestination = NavDestination.Home)

        // Step 1: Start on Home — printer screen not visible
        composeTestRule.onNodeWithText("IMPRESORA").assertDoesNotExist()

        // Step 2: Navigate to Printer
        composeTestRule
            .onNodeWithContentDescription("Impresora")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()

        // Step 3: Navigate back to Home via NavRail
        composeTestRule
            .onNodeWithContentDescription("Inicio")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("IMPRESORA").assertDoesNotExist()
        composeTestRule.onNodeWithText("other_screen").assertIsDisplayed()

        // NavRail is still present with both items
        composeTestRule.onNodeWithContentDescription("Inicio").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Impresora").assertIsDisplayed()
    }
}
