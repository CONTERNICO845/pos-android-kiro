package com.example.puntodeventa.ui.printer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose tests for the [ControlPanel] composable.
 *
 * Covers:
 * - Header text display ("IMPRESORA", "POS-8360 LAN")       — Requirements 3.1, 3.2, 3.4, 3.5, 3.6
 * - Static settings rows in order (Puerto, Papel, Corte, Modo) — Requirements 5.1
 * - Button labels and click handlers ("Probar impresora", "Guardar") — Requirements 6.1, 6.2, 6.3
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 5.1, 6.1, 6.2, 6.3**
 */
@RunWith(AndroidJUnit4::class)
class ControlPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helper — render ControlPanel with sensible defaults ──────────────────

    private fun renderControlPanel(
        ipAddress: String = "",
        onIpAddressChange: (String) -> Unit = {},
        onTestClick: () -> Unit = {},
        onSaveClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                ControlPanel(
                    ipAddress         = ipAddress,
                    onIpAddressChange = onIpAddressChange,
                    onTestClick       = onTestClick,
                    onSaveClick       = onSaveClick
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // ── Requirement 3: Header ─────────────────────────────────────────────────

    /**
     * The panel must display "IMPRESORA" as the title text.
     *
     * **Validates: Requirements 3.1, 3.4, 3.6**
     */
    @Test
    fun controlPanel_displays_IMPRESORA_title() {
        renderControlPanel()

        composeTestRule
            .onNodeWithText("IMPRESORA", substring = false)
            .assertIsDisplayed()
    }

    /**
     * The panel must display "POS-8360 LAN" as the subtitle text.
     *
     * **Validates: Requirements 3.2, 3.3, 3.5, 3.6**
     */
    @Test
    fun controlPanel_displays_POS8360LAN_subtitle() {
        renderControlPanel()

        composeTestRule
            .onNodeWithText("POS-8360 LAN", substring = false)
            .assertIsDisplayed()
    }

    /**
     * Both header texts must be visible simultaneously.
     *
     * **Validates: Requirements 3.1, 3.2, 3.6**
     */
    @Test
    fun controlPanel_displays_both_header_texts_simultaneously() {
        renderControlPanel()

        composeTestRule.onNodeWithText("IMPRESORA").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 LAN").assertIsDisplayed()
    }

    // ── Requirement 5: Static Settings Rows ──────────────────────────────────

    /**
     * The panel must show "Puerto" and its value "9100".
     *
     * **Validates: Requirement 5.1**
     */
    @Test
    fun controlPanel_displays_static_row_Puerto_9100() {
        renderControlPanel()

        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
    }

    /**
     * The panel must show "Papel" and its value "80mm".
     *
     * **Validates: Requirement 5.1**
     */
    @Test
    fun controlPanel_displays_static_row_Papel_80mm() {
        renderControlPanel()

        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
    }

    /**
     * The panel must show "Corte" and its value "Automatico".
     *
     * **Validates: Requirement 5.1**
     */
    @Test
    fun controlPanel_displays_static_row_Corte_Automatico() {
        renderControlPanel()

        composeTestRule.onNodeWithText("Corte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatico").assertIsDisplayed()
    }

    /**
     * The panel must show "Modo" and its value "ESC/POS".
     *
     * **Validates: Requirement 5.1**
     */
    @Test
    fun controlPanel_displays_static_row_Modo_ESCPOS() {
        renderControlPanel()

        composeTestRule.onNodeWithText("Modo").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESC/POS").assertIsDisplayed()
    }

    /**
     * All four static rows must be displayed together on screen.
     *
     * **Validates: Requirement 5.1**
     */
    @Test
    fun controlPanel_displays_all_four_static_rows() {
        renderControlPanel()

        // Row labels
        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Corte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Modo").assertIsDisplayed()

        // Row values
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatico").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESC/POS").assertIsDisplayed()
    }

    // ── Requirement 6: Action Buttons ────────────────────────────────────────

    /**
     * The Test_Button must be visible with the label "Probar impresora".
     *
     * **Validates: Requirements 6.1, 6.3**
     */
    @Test
    fun controlPanel_displays_test_button_with_correct_label() {
        renderControlPanel()

        composeTestRule
            .onNodeWithText("Probar impresora", substring = false)
            .assertIsDisplayed()
    }

    /**
     * The Save_Button must be visible with the label "Guardar".
     *
     * **Validates: Requirements 6.2, 6.3**
     */
    @Test
    fun controlPanel_displays_save_button_with_correct_label() {
        renderControlPanel()

        composeTestRule
            .onNodeWithText("Guardar", substring = false)
            .assertIsDisplayed()
    }

    /**
     * Both buttons must be visible simultaneously (horizontal arrangement).
     *
     * **Validates: Requirement 6.3**
     */
    @Test
    fun controlPanel_displays_both_action_buttons_simultaneously() {
        renderControlPanel()

        composeTestRule.onNodeWithText("Probar impresora").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
    }

    /**
     * Clicking Test_Button must invoke the onTestClick callback.
     *
     * **Validates: Requirement 6.6 (UI wiring for onTestClick)**
     */
    @Test
    fun controlPanel_clicking_test_button_invokes_onTestClick() {
        var testClicked = false

        renderControlPanel(onTestClick = { testClicked = true })

        composeTestRule
            .onNodeWithText("Probar impresora")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue("Clicking 'Probar impresora' must invoke onTestClick", testClicked)
    }

    /**
     * Clicking Save_Button must invoke the onSaveClick callback.
     *
     * **Validates: Requirement 6.7 (UI wiring for onSaveClick)**
     */
    @Test
    fun controlPanel_clicking_save_button_invokes_onSaveClick() {
        var saveClicked = false

        renderControlPanel(onSaveClick = { saveClicked = true })

        composeTestRule
            .onNodeWithText("Guardar")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue("Clicking 'Guardar' must invoke onSaveClick", saveClicked)
    }

    /**
     * Each button click must trigger only its own callback and not the other's.
     *
     * **Validates: Requirements 6.6, 6.7**
     */
    @Test
    fun controlPanel_buttons_trigger_independent_callbacks() {
        var testClicked = false
        var saveClicked = false

        renderControlPanel(
            onTestClick = { testClicked = true },
            onSaveClick = { saveClicked = true }
        )

        // Click Test_Button — only testClicked should be true
        composeTestRule.onNodeWithText("Probar impresora").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Test button must invoke onTestClick", testClicked)
        assertTrue("Save button must NOT have been invoked yet", !saveClicked)

        // Reset and click Save_Button
        testClicked = false

        composeTestRule.onNodeWithText("Guardar").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Save button must invoke onSaveClick", saveClicked)
        assertTrue("Test button must NOT have been invoked again", !testClicked)
    }
}
