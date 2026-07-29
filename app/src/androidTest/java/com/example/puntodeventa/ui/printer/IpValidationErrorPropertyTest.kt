package com.example.puntodeventa.ui.printer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Property-based instrumented tests for IP validation error display in [ControlPanel].
 *
 * **Property 3: IP Format Validation Error Display**
 *
 * For any malformed IP address string, the IP address field shall display an error state
 * (the "Formato de IP inválido" message is shown). For valid IP strings and for an empty
 * string, the error message must NOT appear.
 *
 * **Validates: Requirement 4.7**
 */
@RunWith(AndroidJUnit4::class)
class IpValidationErrorPropertyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val errorText = "Formato de IP inválido"

    // ── Invalid IP samples ────────────────────────────────────────────────────

    private val invalidIpAddresses = listOf(
        "abc",                // letters only
        "999.999.999.999",    // each octet out of range
        "192.168.1",          // only 3 octets
        "192.168.1.1.1",      // 5 octets
        "1.2.3",              // only 3 octets
        "256.0.0.1",          // first octet out of range
        "192.168.1.",         // trailing dot, 4th octet empty
        ".1.2.3",             // leading dot, first octet empty
        "1..2.3",             // consecutive dots → empty octet
        "192.168.-1.1",       // negative octet
        "192.168.1.a",        // letter in last octet
        "....",               // only dots
        "1",                  // single octet
        "1.2",                // two octets
        "192.168",            // two octets
        "300.1.1.1",          // first octet > 255
        "192.168.1.256",      // last octet > 255
        "1.2.3.4.5",          // five octets
        "...1"                // leading dots with single digit
    )

    // ── Valid IP samples ──────────────────────────────────────────────────────

    private val validIpAddresses = listOf(
        "192.168.1.100",
        "0.0.0.0",
        "255.255.255.255",
        "10.0.0.1",
        "127.0.0.1",
        "1.1.1.1"
    )

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Sets [ControlPanel] content with a controllable [ipAddress] state.
     * Returns a setter so individual tests can update the displayed value.
     */
    private fun setContent(initialIp: String): (String) -> Unit {
        var ip by mutableStateOf(initialIp)
        composeTestRule.setContent {
            MaterialTheme {
                ControlPanel(
                    ipAddress         = ip,
                    onIpAddressChange = { ip = it },
                    onTestClick       = {},
                    onSaveClick       = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        return { newIp -> ip = newIp }
    }

    // ── Property: invalid inputs always show the error message ───────────────

    /**
     * For every malformed IP string the error label must be visible.
     *
     * **Validates: Requirement 4.7**
     */
    @Test
    fun property_invalidIp_always_shows_error_message() {
        val setIp = setContent(invalidIpAddresses.first())

        for (ip in invalidIpAddresses) {
            setIp(ip)
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(errorText)
                .assertIsDisplayed()
        }
    }

    // ── Property: valid inputs never show the error message ──────────────────

    /**
     * For every well-formed IP string the error label must NOT be present in the
     * composition.
     *
     * **Validates: Requirement 4.7**
     */
    @Test
    fun property_validIp_never_shows_error_message() {
        val setIp = setContent(validIpAddresses.first())

        for (ip in validIpAddresses) {
            setIp(ip)
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(errorText)
                .assertDoesNotExist()
        }
    }

    // ── Property: empty string never shows the error message ─────────────────

    /**
     * An empty IP string is the initial / blank state and must not trigger the
     * error label.
     *
     * **Validates: Requirement 4.7**
     */
    @Test
    fun property_emptyIp_does_not_show_error_message() {
        setContent("")

        composeTestRule
            .onNodeWithText(errorText)
            .assertDoesNotExist()
    }

    // ── Transition property: switching between valid and invalid ─────────────

    /**
     * The error state must update reactively: setting an invalid value after a
     * valid one must show the error, and clearing the field must hide it again.
     *
     * **Validates: Requirement 4.7**
     */
    @Test
    fun property_error_updates_reactively_on_ip_change() {
        val setIp = setContent("192.168.1.1") // start valid → no error

        composeTestRule.onNodeWithText(errorText).assertDoesNotExist()

        // Switch to invalid
        setIp("abc")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()

        // Switch back to valid
        setIp("10.0.0.1")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(errorText).assertDoesNotExist()

        // Clear the field
        setIp("")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(errorText).assertDoesNotExist()
    }
}
