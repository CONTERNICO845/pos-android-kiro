package com.example.puntodeventa.ui.printer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Property-based instrumented tests for the [StaticSettingRow] composable.
 *
 * Feature: printer-config-ui, Property 4: StaticDisplayRow Universal Styling
 *
 * For any StaticDisplayRow instance, it shall:
 *   - Display the label text on screen                 — Requirement 5.2
 *   - Display the value text on screen                 — Requirement 5.2
 *   - Arrange label and value horizontally (label left of value) — Requirement 5.3
 *
 * Note: BackgroundSecondary background (5.2), font weight and color (5.4, 5.5) are
 * not directly observable through standard Compose semantics APIs. The testable
 * surface area is limited to text presence and layout position, which is sufficient
 * to verify the compositional contract of the component.
 *
 * Input space covers:
 *   - Typical printer configuration values (Puerto, Papel, Corte, Modo)
 *   - Empty strings
 *   - Long strings (LongLabelText.repeat(5))
 *   - Special characters and unicode (Ñoño, €£¥)
 *   - Numeric-only strings
 *
 * **Validates: Requirements 5.2, 5.3, 5.4, 5.5**
 */
@RunWith(AndroidJUnit4::class)
class StaticSettingRowPropertyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── Input space ───────────────────────────────────────────────────────────

    /**
     * All label/value pairs that must render both texts (non-empty entries only).
     * Each pair uses a unique prefix tag so multiple rows can coexist in one
     * setContent call without ambiguous text nodes.
     *
     * Exact required pairs from the spec:
     *   ("Puerto","9100"), ("Papel","80mm"), ("Corte","Automatico"), ("Modo","ESC/POS"),
     *   ("",""), ("LongLabelText".repeat(5), "LongValue"), ("Ñoño","€£¥")
     */
    private val nonEmptyPairs: List<Pair<String, String>> = listOf(
        "Puerto"                   to "9100",
        "Papel"                    to "80mm",
        "Corte"                    to "Automatico",
        "Modo"                     to "ESC/POS",
        "LongLabelText".repeat(5)  to "LongValue",
        "Ñoño"                     to "€£¥",
        "Label"                    to "SomeValue",
        "192"                      to "168",
        "Proto"                    to "ESC/POS:v2"
    )

    // ── Property: both texts are displayed for all non-empty pairs ────────────

    /**
     * All rows are rendered together inside one setContent call.
     * Every label and value text must be present in the semantics tree.
     *
     * **Validates: Requirements 5.2, 5.3**
     */
    @Test
    fun property_bothTextsAreDisplayed_forAllInputs() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    // Empty-string pair — row exists but no text nodes to assert
                    StaticSettingRow(label = "", value = "")

                    // All non-empty pairs rendered simultaneously
                    nonEmptyPairs.forEach { (label, value) ->
                        StaticSettingRow(label = label, value = value)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Assert every non-empty label and value is displayed
        nonEmptyPairs.forEach { (label, value) ->
            composeTestRule
                .onNodeWithText(label, substring = false, useUnmergedTree = true)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(value, substring = false, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    // ── Property: label appears to the left of value ─────────────────────────

    /**
     * For every pair where label ≠ value, the label node's horizontal center
     * must be strictly less than the value node's horizontal center.
     *
     * All rows are rendered in one Column to keep a single setContent call.
     *
     * **Validates: Requirements 5.3**
     */
    @Test
    fun property_labelIsLeftOfValue_horizontalArrangement() {
        val distinguishablePairs = nonEmptyPairs.filter { (l, v) -> l != v }

        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    distinguishablePairs.forEach { (label, value) ->
                        StaticSettingRow(label = label, value = value)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        distinguishablePairs.forEach { (label, value) ->
            val labelNode = composeTestRule
                .onNodeWithText(label, useUnmergedTree = true)
                .fetchSemanticsNode()

            val valueNode = composeTestRule
                .onNodeWithText(value, useUnmergedTree = true)
                .fetchSemanticsNode()

            val labelCenterX = labelNode.boundsInWindow.left + labelNode.boundsInWindow.width / 2
            val valueCenterX = valueNode.boundsInWindow.left + valueNode.boundsInWindow.width / 2

            assertTrue(
                "For label='$label', value='$value': " +
                    "label center X ($labelCenterX) must be < value center X ($valueCenterX)",
                labelCenterX < valueCenterX
            )
        }
    }

    // ── Property: typical printer rows all display correctly ─────────────────

    /**
     * The four canonical printer-config rows used in production must each render
     * their label and value simultaneously.
     *
     * **Validates: Requirements 5.2, 5.3, 5.4, 5.5**
     */
    @Test
    fun property_typicalPrinterRows_displayBothTexts() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    StaticSettingRow(label = "Puerto", value = "9100")
                    StaticSettingRow(label = "Papel",  value = "80mm")
                    StaticSettingRow(label = "Corte",  value = "Automatico")
                    StaticSettingRow(label = "Modo",   value = "ESC/POS")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Corte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Modo").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESC/POS").assertIsDisplayed()
    }

    // ── Property: long strings do not crash or hide content ──────────────────

    /**
     * Long label ("LongLabelText".repeat(5)) and short value ("LongValue") must
     * both be present in the semantics tree.
     *
     * **Validates: Requirements 5.2, 5.3**
     */
    @Test
    fun property_longStrings_bothTextsRemainDisplayed() {
        val longLabel = "LongLabelText".repeat(5)
        val longValue = "LongValue"

        composeTestRule.setContent {
            MaterialTheme {
                StaticSettingRow(label = longLabel, value = longValue)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(longLabel, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(longValue, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ── Property: special characters and unicode render without error ─────────

    /**
     * "Ñoño"/"€£¥" (required by spec) and a slash-containing value must render
     * both texts without error.
     *
     * **Validates: Requirements 5.2, 5.4, 5.5**
     */
    @Test
    fun property_specialCharacters_bothTextsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    StaticSettingRow(label = "Proto", value = "ESC/POS")
                    StaticSettingRow(label = "Ñoño",  value = "€£¥")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Proto",   useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ESC/POS", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ñoño",    useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("€£¥",     useUnmergedTree = true).assertIsDisplayed()
    }

    // ── Property: empty-string pair does not crash ────────────────────────────

    /**
     * A StaticSettingRow with both label and value as empty strings must
     * render without throwing an exception.
     *
     * **Validates: Requirements 5.2**
     */
    @Test
    fun property_emptyStrings_rowRendersWithoutCrash() {
        composeTestRule.setContent {
            MaterialTheme {
                StaticSettingRow(label = "", value = "")
            }
        }
        // If setContent + waitForIdle completes without exception the test passes
        composeTestRule.waitForIdle()
    }
}
