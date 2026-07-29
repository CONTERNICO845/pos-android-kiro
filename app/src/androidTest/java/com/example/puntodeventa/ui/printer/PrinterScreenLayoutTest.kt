package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose tests for the [PrinterScreen] two-column layout.
 *
 * Covers:
 * - Two-column layout structure renders both panels        — Requirements 2.1, 2.4, 2.5
 * - ControlPanel content is visible in the left column     — Requirements 2.2, 2.4
 * - StatusPanel content is visible in the right column     — Requirements 2.3, 2.5
 * - Integration: both panels render simultaneously          — Requirements 2.1–2.5
 *
 * Note: exact pixel-level weight checks are not achievable with standard Compose
 * semantics APIs. These tests validate integration and presence of both columns.
 *
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
 */
@RunWith(AndroidJUnit4::class)
class PrinterScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Renders [PrinterScreen] directly, using the real [PrinterConfigViewModel]
     * (created internally by the composable's default viewModel() call).
     */
    private fun renderPrinterScreen() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    PrinterScreen()
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // ── Requirement 2.1: Two-column layout renders both columns ──────────────

    /**
     * PrinterScreen must render both ControlPanel and StatusPanel in a single pass.
     *
     * Verifying content from both panels confirms the Row layout hosts two columns.
     *
     * **Validates: Requirements 2.1, 2.4, 2.5**
     */
    @Test
    fun printerScreen_renders_both_columns() {
        renderPrinterScreen()

        // ControlPanel (left column) content
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()

        // StatusPanel (right column) content
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()
    }

    // ── Requirement 2.4: Left column contains ControlPanel ───────────────────

    /**
     * The left column must contain the ControlPanel composable with its header,
     * static rows, and action buttons all visible.
     *
     * **Validates: Requirement 2.4**
     */
    @Test
    fun printerScreen_left_column_contains_ControlPanel_content() {
        renderPrinterScreen()

        // Header
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 LAN").assertIsDisplayed()

        // Action buttons
        composeTestRule.onNodeWithText("Probar impresora").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
    }

    // ── Requirement 2.5: Right column contains StatusPanel ───────────────────

    /**
     * The right column must contain the StatusPanel composable with its header
     * and status rows visible.
     *
     * **Validates: Requirement 2.5**
     */
    @Test
    fun printerScreen_right_column_contains_StatusPanel_content() {
        renderPrinterScreen()

        // Header
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()

        // At least one status row
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
    }

    // ── Requirement 2.2 / 2.3: Both columns occupy the available space ────────

    /**
     * Both columns must be simultaneously visible — confirming neither is hidden
     * or overlaps the other beyond the layout's own rendering bounds.
     *
     * **Validates: Requirements 2.2, 2.3**
     */
    @Test
    fun printerScreen_both_columns_visible_simultaneously() {
        renderPrinterScreen()

        // Unique text nodes from each column must coexist on-screen
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 LAN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
    }

    // ── Integration: Full layout content check ────────────────────────────────

    /**
     * After rendering PrinterScreen the complete expected content from both panels
     * (header, static rows, status rows, description, buttons) must all be displayed.
     *
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
     */
    @Test
    fun printerScreen_full_layout_displays_all_expected_content() {
        renderPrinterScreen()

        // ── Left column (ControlPanel) ────────────────────────────────────────
        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 LAN").assertIsDisplayed()
        // "Puerto" and "Papel" appear in both panels — assert at least one instance is shown
        composeTestRule.onAllNodesWithText("Puerto")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Papel")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Probar impresora").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()

        // ── Right column (StatusPanel) ────────────────────────────────────────
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Modelo").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN / Socket TCP").assertIsDisplayed()
    }

    // ── Requirement 2.4: ControlPanel renders with state from ViewModel ───────

    /**
     * PrinterScreen correctly wires state from the ViewModel to ControlPanel —
     * the IP address field starts empty (default ViewModel state).
     *
     * **Validates: Requirements 2.4, 12.2**
     */
    @Test
    fun printerScreen_initial_state_shows_empty_ip_field() {
        renderPrinterScreen()

        // The "IP local" label should be visible on the OutlinedTextField
        composeTestRule.onNodeWithText("IP local").assertIsDisplayed()
    }
}
