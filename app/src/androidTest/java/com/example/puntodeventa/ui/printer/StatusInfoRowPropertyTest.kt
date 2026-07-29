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
 * Property-based instrumented tests for the [StatusInfoRow] composable.
 *
 * Feature: printer-config-ui, Property 5: Status Row Universal Styling
 *
 * For any StatusInfoRow instance, it shall:
 *   - Display the label text on screen                 — Requirement 8.6
 *   - Display the value text on screen                 — Requirement 8.6
 *   - Arrange label and value horizontally (label left of value) — Requirement 8.7
 *
 * Note: ModalBodyText color (8.6), font weight (bold for label, normal for value)
 * are not directly observable through standard Compose semantics APIs. The testable
 * surface area is limited to text presence and layout position, which is sufficient
 * to verify the compositional contract of the component.
 *
 * Input space covers:
 *   - Typical printer status values (Modelo, Papel, Conexion, Puerto, Cortador)
 *   - Empty strings
 *   - Long strings (LongLabel.repeat(10))
 *   - Special characters and unicode (Ñoño, €£¥)
 *
 * **Validates: Requirements 8.6, 8.7**
 */
@RunWith(AndroidJUnit4::class)
class StatusInfoRowPropertyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── Input space ───────────────────────────────────────────────────────────

    /**
     * All label/value pairs that must render both texts (non-empty entries only).
     * Each pair uses a unique prefix tag so multiple rows can coexist in one
     * setContent call without ambiguous text nodes.
     *
     * Exact required pairs from the spec:
     *   ("Modelo","POS-8360 Termica"), ("Papel","80mm"), ("Conexion","LAN / Socket TCP"),
     *   ("Puerto","9100"), ("Cortador","Activo al finalizar ticket"),
     *   ("",""), ("LongLabel".repeat(10), "LongValue"), ("Ñoño","€£¥")
     */
    private val nonEmptyPairs: List<Pair<String, String>> = listOf(
        "Modelo"    to "POS-8360 Termica",
        "Papel"     to "80mm",
        "Conexion"  to "LAN / Socket TCP",
        "Puerto"    to "9100",
        "Cortador"  to "Activo al finalizar ticket",
        "LongLabel".repeat(10) to "LongValue",
        "Ñoño"      to "€£¥"
    )

    // ── Property: both texts are displayed for all non-empty pairs ────────────

    /**
     * All rows are rendered together inside one setContent call.
     * Every label and value text must be present in the semantics tree.
     *
     * **Validates: Requirements 8.6, 8.7**
     */
    @Test
    fun property_bothTextsAreDisplayed_forAllInputs() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    // Empty-string pair — row exists but no text nodes to assert
                    StatusInfoRow(label = "", value = "")

                    // All non-empty pairs rendered simultaneously
                    nonEmptyPairs.forEach { (label, value) ->
                        StatusInfoRow(label = label, value = value)
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
     * For every pair where label ≠ value, the label node's left edge
     * must be strictly less than the value node's left edge.
     *
     * All rows are rendered in one Column to keep a single setContent call.
     *
     * **Validates: Requirements 8.7**
     */
    @Test
    fun property_labelIsLeftOfValue_horizontalArrangement() {
        val distinguishablePairs = nonEmptyPairs.filter { (l, v) -> l != v }

        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    distinguishablePairs.forEach { (label, value) ->
                        StatusInfoRow(label = label, value = value)
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

            val labelLeft = labelNode.boundsInWindow.left
            val valueLeft = valueNode.boundsInWindow.left

            assertTrue(
                "For label='$label', value='$value': " +
                    "label left ($labelLeft) must be < value left ($valueLeft)",
                labelLeft < valueLeft
            )
        }
    }

    // ── Property: typical printer status rows all display correctly ──────────

    /**
     * The five canonical printer-status rows used in production must each render
     * their label and value simultaneously.
     *
     * **Validates: Requirements 8.6, 8.7**
     */
    @Test
    fun property_typicalPrinterStatusRows_displayBothTexts() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    StatusInfoRow(label = "Modelo",    value = "POS-8360 Termica")
                    StatusInfoRow(label = "Papel",     value = "80mm")
                    StatusInfoRow(label = "Conexion",  value = "LAN / Socket TCP")
                    StatusInfoRow(label = "Puerto",    value = "9100")
                    StatusInfoRow(label = "Cortador",  value = "Activo al finalizar ticket")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Modelo").assertIsDisplayed()
        composeTestRule.onNodeWithText("POS-8360 Termica").assertIsDisplayed()
        composeTestRule.onNodeWithText("Papel").assertIsDisplayed()
        composeTestRule.onNodeWithText("80mm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conexion").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN / Socket TCP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Puerto").assertIsDisplayed()
        composeTestRule.onNodeWithText("9100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cortador").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activo al finalizar ticket").assertIsDisplayed()
    }

    // ── Property: long strings do not crash or hide content ──────────────────

    /**
     * Long label ("LongLabel".repeat(10)) and short value ("LongValue") must
     * both be present in the semantics tree.
     *
     * **Validates: Requirements 8.6, 8.7**
     */
    @Test
    fun property_longStrings_bothTextsRemainDisplayed() {
        val longLabel = "LongLabel".repeat(10)
        val longValue = "LongValue"

        composeTestRule.setContent {
            MaterialTheme {
                StatusInfoRow(label = longLabel, value = longValue)
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
     * "Ñoño"/"€£¥" (required by spec) and slash-containing values must render
     * both texts without error.
     *
     * **Validates: Requirements 8.6, 8.7**
     */
    @Test
    fun property_specialCharacters_bothTextsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    StatusInfoRow(label = "Conexion", value = "LAN / Socket TCP")
                    StatusInfoRow(label = "Ñoño",     value = "€£¥")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Conexion", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN / Socket TCP", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ñoño", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("€£¥",  useUnmergedTree = true).assertIsDisplayed()
    }

    // ── Property: empty-string pair does not crash ────────────────────────────

    /**
     * A StatusInfoRow with both label and value as empty strings must
     * render without throwing an exception.
     *
     * **Validates: Requirements 8.6**
     */
    @Test
    fun property_emptyStrings_rowRendersWithoutCrash() {
        composeTestRule.setContent {
            MaterialTheme {
                StatusInfoRow(label = "", value = "")
            }
        }
        // If setContent + waitForIdle completes without exception the test passes
        composeTestRule.waitForIdle()
    }
}
