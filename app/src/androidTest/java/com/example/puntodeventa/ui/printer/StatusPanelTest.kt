package com.example.puntodeventa.ui.printer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose tests for the [StatusPanel] composable.
 *
 * Covers:
 * - Header text display ("Estado de conexion")          — Requirements 7.1, 7.2, 7.3
 * - All five status information rows (label + value)    — Requirements 8.1–8.5
 * - Description text content and presence              — Requirements 9.1, 9.2, 9.3
 *
 * **Validates: Requirements 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3**
 */
@RunWith(AndroidJUnit4::class)
class StatusPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderStatusPanel() {
        composeTestRule.setContent {
            MaterialTheme {
                StatusPanel()
            }
        }
        composeTestRule.waitForIdle()
    }

    // ── Requirement 7: Header ─────────────────────────────────────────────────

    /**
     * The panel must display "Estado de conexion" as the section header.
     *
     * **Validates: Requirements 7.1, 7.2, 7.3**
     */
    @Test
    fun statusPanel_displays_header_text() {
        renderStatusPanel()

        composeTestRule
            .onNodeWithText("Estado de conexion", substring = false)
            .assertIsDisplayed()
    }

    // ── Requirement 8: Status Information Rows ────────────────────────────────

    /**
     * The panel must show the "Modelo" label and "POS-8360 Termica" value.
     *
     * **Validates: Requirement 8.1**
     */
    @Test
    fun statusPanel_displays_Modelo_row() {
        renderStatusPanel()

        composeTestRule.onNodeWithText("Modelo").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
    }

    /**
     * The panel must show the "Papel" label and "80mm" value.
     *
     * **Validates: Requirement 8.2**
     */
    @Test
    fun statusPanel_displays_Papel_row() {
        renderStatusPanel()

        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
    }

    /**
     * The panel must show the "Conexion" label and "LAN / Socket TCP" value.
     *
     * **Validates: Requirement 8.3**
     */
    @Test
    fun statusPanel_displays_Conexion_row() {
        renderStatusPanel()

        composeTestRule.onNodeWithText("Conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN / Socket TCP").assertIsDisplayed()
    }

    /**
     * The panel must show the "Puerto" label and "9100" value.
     *
     * **Validates: Requirement 8.4**
     */
    @Test
    fun statusPanel_displays_Puerto_row() {
        renderStatusPanel()

        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
    }

    /**
     * The panel must show the "Cortador" label and "Activo al finalizar ticket" value.
     *
     * **Validates: Requirement 8.5**
     */
    @Test
    fun statusPanel_displays_Cortador_row() {
        renderStatusPanel()

        composeTestRule.onNodeWithText("Cortador").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activo al finalizar ticket").assertIsDisplayed()
    }

    /**
     * All five status rows must be displayed simultaneously.
     *
     * **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**
     */
    @Test
    fun statusPanel_displays_all_five_status_rows() {
        renderStatusPanel()

        // Labels
        composeTestRule.onNodeWithText("Modelo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cortador").assertIsDisplayed()

        // Values
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN / Socket TCP").assertIsDisplayed()
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activo al finalizar ticket").assertIsDisplayed()
    }

    // ── Requirement 9: Description Text ──────────────────────────────────────

    /**
     * The panel must display the description text explaining the test print behaviour.
     * Uses substring matching because the text may wrap and Compose may split it.
     *
     * **Validates: Requirements 9.1, 9.2, 9.3**
     */
    @Test
    fun statusPanel_displays_description_text() {
        renderStatusPanel()

        composeTestRule
            .onNodeWithText(
                "Cuando presiones imprimir prueba, se enviara el ticket real por red usando la clase Java ESC/POS.",
                substring = true
            )
            .assertIsDisplayed()
    }

    /**
     * All content — header, rows, and description — must be visible together.
     *
     * **Validates: Requirements 7.1, 8.1–8.5, 9.1**
     */
    @Test
    fun statusPanel_displays_all_sections_together() {
        renderStatusPanel()

        // Header
        composeTestRule.onNodeWithText("Estado de conexion").assertIsDisplayed()

        // At least one row from each section
        composeTestRule.onNodeWithText("Modelo").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()

        // Description (partial match to tolerate line wrapping)
        composeTestRule
            .onNodeWithText("Cuando presiones imprimir prueba", substring = true)
            .assertIsDisplayed()
    }
}
