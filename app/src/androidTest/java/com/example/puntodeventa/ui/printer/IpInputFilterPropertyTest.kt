package com.example.puntodeventa.ui.printer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented property test for IP address input character filtering in [ControlPanel].
 *
 * **Property 2: Input Character Filtering**
 *
 * For any character input to the IP address field, only digits (0-9) and periods (.)
 * shall be accepted; all other characters shall be filtered out.
 *
 * **Validates: Requirements 4.6**
 */
@RunWith(AndroidJUnit4::class)
class IpInputFilterPropertyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders ControlPanel with a mutable state wired for IP address changes and
     * returns a lambda that provides the current filtered value.
     */
    private fun renderAndGetState(): () -> String {
        var ipState by mutableStateOf("")
        composeTestRule.setContent {
            MaterialTheme {
                ControlPanel(
                    ipAddress         = ipState,
                    onIpAddressChange = { ipState = it },
                    onTestClick       = {},
                    onSaveClick       = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        return { ipState }
    }

    /**
     * Helper: type [input] into the "IP local" field and assert the resulting state
     * satisfies [assertion].
     */
    private fun typeAndAssert(
        input: String,
        assertion: (result: String) -> Boolean,
        message: String
    ) {
        val getState = renderAndGetState()

        composeTestRule
            .onNodeWithText("IP local")
            .performTextInput(input)
        composeTestRule.waitForIdle()

        val result = getState()
        assertTrue("Input: \"$input\" — $message (got: \"$result\")", assertion(result))
    }

    // ── Core invariant helper ─────────────────────────────────────────────────

    /**
     * Asserts that [value] contains only characters that are digits or periods.
     */
    private fun onlyDigitsAndPeriods(value: String): Boolean =
        value.all { it.isDigit() || it == '.' }

    // ── Property tests: diverse inputs ───────────────────────────────────────

    /** Pure alphabetic ASCII — all characters must be filtered out. */
    @Test
    fun filter_pureAlpha_lowercase_yieldsEmpty() {
        typeAndAssert(
            input     = "abcdef",
            assertion = { it.isEmpty() },
            message   = "Pure lowercase alpha must be fully filtered"
        )
    }

    /** Pure alphabetic uppercase — all characters must be filtered out. */
    @Test
    fun filter_pureAlpha_uppercase_yieldsEmpty() {
        typeAndAssert(
            input     = "GHIJKL",
            assertion = { it.isEmpty() },
            message   = "Pure uppercase alpha must be fully filtered"
        )
    }

    /** Pure digit string — must pass through unchanged. */
    @Test
    fun filter_pureDigits_passThrough() {
        typeAndAssert(
            input     = "1234567890",
            assertion = { it == "1234567890" },
            message   = "Pure digits must pass through unchanged"
        )
    }

    /** Only periods — must pass through unchanged. */
    @Test
    fun filter_onlyPeriods_passThrough() {
        typeAndAssert(
            input     = "...",
            assertion = { it == "..." },
            message   = "Only periods must pass through unchanged"
        )
    }

    /** Typical valid IP address — must pass through unchanged. */
    @Test
    fun filter_validIpAddress_passThrough() {
        typeAndAssert(
            input     = "192.168.1.100",
            assertion = { it == "192.168.1.100" },
            message   = "Valid IP address must pass through unchanged"
        )
    }

    /** Mixed digits and letters — letters filtered, digits retained. */
    @Test
    fun filter_mixedDigitsAndLetters_retainsOnlyDigits() {
        typeAndAssert(
            input     = "1a2b3c",
            assertion = { it == "123" },
            message   = "Mixed digits+letters: only digits must remain"
        )
    }

    /** IP-like string with extra letters — letters filtered. */
    @Test
    fun filter_ipLikeWithLetters_retainsDigitsAndPeriods() {
        typeAndAssert(
            input     = "192.168.abc.1",
            assertion = { it == "192.168..1" },
            message   = "IP-like string: letters must be removed, digits and periods kept"
        )
    }

    /** Special characters — must all be filtered out. */
    @Test
    fun filter_specialChars_yieldsEmpty() {
        typeAndAssert(
            input     = "!@#\$%^&*()",
            assertion = { it.isEmpty() },
            message   = "Special characters must be fully filtered"
        )
    }

    /** Whitespace characters — spaces and tabs must be filtered out. */
    @Test
    fun filter_whitespace_yieldsEmpty() {
        typeAndAssert(
            input     = "   \t",
            assertion = { it.isEmpty() },
            message   = "Whitespace must be fully filtered"
        )
    }

    /** Unicode letters — must be filtered out. */
    @Test
    fun filter_unicodeLetters_yieldsEmpty() {
        typeAndAssert(
            input     = "ñáéíóú",
            assertion = { it.isEmpty() },
            message   = "Unicode letters must be fully filtered"
        )
    }

    /** Digits with surrounding spaces — spaces filtered, digits retained. */
    @Test
    fun filter_digitsWithSpaces_retainsOnlyDigits() {
        typeAndAssert(
            input     = " 192 168 ",
            assertion = { it == "192168" },
            message   = "Digits surrounded by spaces: spaces filtered, digits kept"
        )
    }

    /** Mixed special chars and digits — only digits survive. */
    @Test
    fun filter_specialCharsAndDigits_retainsDigits() {
        typeAndAssert(
            input     = "1!2@3#4",
            assertion = { it == "1234" },
            message   = "Special chars mixed with digits: only digits retained"
        )
    }

    /** Empty string input — result must remain empty. */
    @Test
    fun filter_emptyString_yieldsEmpty() {
        typeAndAssert(
            input     = "",
            assertion = { it.isEmpty() },
            message   = "Empty input must yield empty result"
        )
    }

    /** Slash and backslash — must be filtered out. */
    @Test
    fun filter_slashCharacters_yieldsEmpty() {
        typeAndAssert(
            input     = "/\\",
            assertion = { it.isEmpty() },
            message   = "Slash characters must be fully filtered"
        )
    }

    /** Digits interspersed with all types of non-allowed chars — only digits and periods survive. */
    @Test
    fun filter_complexMixedInput_retainsOnlyDigitsAndPeriods() {
        typeAndAssert(
            input     = "1a.2b!3c@4.5",
            assertion = { it == "1.23.45" },
            message   = "Complex mixed input: only digits and periods must remain"
        )
    }

    // ── Universal invariant: all outputs contain only valid chars ─────────────

    /**
     * Parameterised universal check: for each of the 15 diverse inputs, the filtered
     * result must only contain digits or periods — no other character is ever present.
     *
     * **Validates: Requirement 4.6**
     */
    @Test
    fun property2_forAllInputs_resultContainsOnlyDigitsAndPeriods() {
        val inputs = listOf(
            "abcdef",
            "GHIJKL",
            "1234567890",
            "...",
            "192.168.1.100",
            "1a2b3c",
            "192.168.abc.1",
            "!@#\$%^&*()",
            "   \t",
            "ñáéíóú",
            " 192 168 ",
            "1!2@3#4",
            "",
            "/\\",
            "1a.2b!3c@4.5"
        )

        inputs.forEach { input ->
            val getState = renderAndGetState()

            composeTestRule
                .onNodeWithText("IP local")
                .performTextInput(input)
            composeTestRule.waitForIdle()

            val result = getState()
            assertTrue(
                "Input \"$input\" produced \"$result\" which contains non-digit/period characters",
                onlyDigitsAndPeriods(result)
            )
        }
    }
}
