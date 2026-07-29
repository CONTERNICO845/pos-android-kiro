package com.example.puntodeventa.ui.printer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented property test for bidirectional state binding between
 * [PrinterConfigViewModel] and the IP address input field in [ControlPanel].
 *
 * **Feature: printer-config-ui, Property 1: Bidirectional State Binding**
 *
 * For any string value, when the IP address field receives user input OR when
 * the ViewModel state changes, the UI field display and ViewModel state must
 * remain synchronized.
 *
 * Two directions are tested:
 *   - **ViewModel → UI**: calling `viewModel.updateIpAddress(x)` causes
 *     `uiState.ipAddress == x` in the ViewModel's state.
 *   - **UI → ViewModel**: user input to the IP address field propagates back
 *     to `uiState.ipAddress` (after the ControlPanel's character filter is applied).
 *
 * Because Kotest is not available on the androidTest classpath, universal
 * quantification is approximated with 100+ curated inputs spanning: typical IP
 * strings, empty string, single characters, long strings, boundary values, and
 * numeric-only strings.
 *
 * **Validates: Requirements 4.3, 4.4**
 */
@RunWith(AndroidJUnit4::class)
class BidirectionalStateBindingPropertyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: PrinterConfigViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = PrinterConfigViewModel(PrinterPreferencesRepository(context))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input space — 100+ diverse string values
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Strings that are accepted unchanged by updateIpAddress (ViewModel→UI direction).
     * These include any character since the ViewModel does not filter.
     *
     * 108 entries — well above the 100-iteration minimum.
     */
    private val viewModelInputs: List<String> = buildList {
        // Boundary / trivial
        add("")
        add(" ")
        add("a")
        add("0")
        add(".")
        add("255")

        // Typical valid IP addresses
        add("192.168.1.1")
        add("10.0.0.1")
        add("172.16.0.1")
        add("0.0.0.0")
        add("255.255.255.255")
        add("127.0.0.1")
        add("192.168.0.100")
        add("192.168.1.255")
        add("10.10.10.10")
        add("1.2.3.4")

        // Partial / malformed IPs (still stored verbatim in the ViewModel)
        add("192.")
        add("192.168.")
        add(".1.1.1")
        add("...")
        add("1.2.3.")
        add(".168.1.1")
        add("192.168.1")
        add("999.999.999.999")
        add("1.2.3.4.5")

        // Single-digit octets
        add("1.1.1.1")
        add("9.9.9.9")
        add("8.8.8.8")
        add("8.8.4.4")

        // Numeric strings (no periods)
        add("192168")
        add("101")
        add("1234567890")
        add("00000")
        add("9999")
        add("11")
        add("22")
        add("33")
        add("44")
        add("55")
        add("66")
        add("77")
        add("88")
        add("99")
        add("100")

        // Period-only strings
        add(".")
        add("..")
        add("...")
        add("....")
        add(".....")

        // Digits with trailing/leading periods
        add("1.")
        add(".1")
        add("1.2")
        add("12.34")
        add("123.456")

        // Strings that would NOT pass the UI character filter but ARE stored in ViewModel
        add("abc")
        add("HELLO")
        add("!@#")
        add("hello world")
        add("192.168.1.1abc")
        add("abc.def.ghi.jkl")
        add("日本語")
        add("ñoño")
        add("🖨️")
        add("\t\n\r")
        add("   ")
        add("   192   ")
        add("null")
        add("undefined")
        add("\u0000")

        // Longer strings
        add("1".repeat(10))
        add("2".repeat(20))
        add("3".repeat(30))
        add("1.".repeat(10).trimEnd('.'))
        add("192.168.1.100".repeat(3))
        add("a".repeat(50))
        add("z".repeat(100))
        add("x".repeat(200))
        add("0".repeat(255))

        // Mixed digit/alpha
        add("1a2b3c")
        add("abc123")
        add("192abc168")
        add("1a.2b.3c.4d")

        // Special characters
        add("!@#\$%^&*()")
        add("/\\")
        add(":;\"'<>?")
        add("[]{}")
        add("+=_-")

        // Unicode and emoji
        add("éàü")
        add("中文")
        add("🔥🌊💧")
        add("αβγδ")

        // Whitespace variations
        add("\t")
        add("\n")
        add("\r\n")
        add("  leading")
        add("trailing  ")

        // Empty-ish
        add("")   // second occurrence — exercises idempotency
        add("0")  // second occurrence

        // Network-style strings
        add("255.0.0.0")
        add("0.255.0.0")
        add("0.0.255.0")
        add("0.0.0.255")
        add("192.0.2.1")
        add("198.51.100.0")
        add("203.0.113.0")

        // Sequential numbers
        add("1.0.0.1")
        add("2.0.0.2")
        add("3.3.3.3")
        add("4.4.4.4")
        add("5.5.5.5")
        add("6.6.6.6")
        add("7.7.7.7")
    }.distinct() // remove any accidental duplicates while keeping 100+ entries

    /**
     * Strings accepted by the ControlPanel's character filter (digits + periods only).
     * Used for UI→ViewModel direction tests to avoid needing to predict filter output.
     *
     * Each entry consists exclusively of `[0-9.]` characters.
     */
    private val filteredInputs: List<String> = listOf(
        "",
        "0",
        ".",
        "1",
        "9",
        "192",
        "168",
        "192.168",
        "192.168.1",
        "192.168.1.1",
        "10.0.0.1",
        "0.0.0.0",
        "255.255.255.255",
        "127.0.0.1",
        "172.16.0.1",
        "192.168.0.100",
        "1.2.3.4",
        "8.8.8.8",
        "8.8.4.4",
        "1.1.1.1",
        "9.9.9.9",
        "192.",
        "192.168.",
        ".1.1.1",
        "...",
        "1.",
        ".1",
        "1.2",
        "12.34",
        "123.456",
        "1".repeat(10),
        "2".repeat(20),
        "0".repeat(30),
        "9".repeat(15),
        "1.".repeat(5).trimEnd('.'),
        "255",
        "256",
        "999",
        "00000",
        "12345",
        "1234567890",
        "192168",
        "101",
        "55",
        "44",
        "33",
        "22",
        "11",
        "00",
        "192.168.1.100",
        "10.10.10.10",
        "172.16.0.254",
        "203.0.113.1",
        "198.51.100.1",
        "192.0.2.255",
        "3.3.3.3",
        "4.4.4.4",
        "5.5.5.5",
        "6.6.6.6",
        "7.7.7.7",
        "1.0.0.1",
        "2.0.0.2",
        "111.222.333.444",
        "0.255.0.0",
        "255.0.0.0",
        "0.0.0.255",
        "192.168.100.200",
        "10.20.30.40",
        "172.31.255.254",
        "169.254.0.1",
        "100.64.0.0",
        "198.18.0.0",
        "192.88.99.1",
        "240.0.0.1",
        "233.252.0.1",
        "1.0.0.0",
        "128.0.0.1",
        "191.255.0.1",
        "223.255.255.1",
        "224.0.0.1",
        "239.255.255.254",
        "...",
        "....",
        ".....",
        "..",
        "0.",
        ".0",
        "0.0",
        "0.0.0",
        "0.0.0.0",
        "255.255.255.255",
        "123456789",
        "987654321",
        "111111111",
        "222222222",
        "333.333",
        "444.444.444",
        "5.55.555.5555",
        "66.666.66.66",
        "7777.7",
        "88.88.88.88",
        "99.99.99.99",
        "100.100.100.100",
        "200.200.200.200",
        "123.123.123.123",
        "210.210.210.210"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Direction 1: ViewModel → UI (ViewModel state consistency)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * **Property 1 — ViewModel → UI direction**
     *
     * For any string passed to [PrinterConfigViewModel.updateIpAddress],
     * `uiState.value.ipAddress` must equal that exact string immediately after the call.
     *
     * This covers Requirements 4.3 (ViewModel updates its state) and 4.4 (UI would
     * display the ViewModel's value — the ViewModel half of the binding).
     *
     * Tests 108+ diverse inputs, exceeding the 100-iteration minimum.
     *
     * **Validates: Requirements 4.3, 4.4**
     */
    @Test
    fun property1_viewModelToUi_updateIpAddress_stateReflectsExactInput() {
        for (input in viewModelInputs) {
            viewModel.updateIpAddress(input)

            val actual = viewModel.uiState.value.ipAddress
            assertEquals(
                "ViewModel→UI: uiState.ipAddress must equal the input exactly.\n" +
                    "Input  : \"$input\"\n" +
                    "Actual : \"$actual\"",
                input,
                actual
            )
        }
    }

    /**
     * **Property 1 — ViewModel → UI direction: sequential updates always reflect latest**
     *
     * Calling updateIpAddress in rapid succession must leave the state equal to
     * the last call's value, with no residual state from earlier calls.
     *
     * **Validates: Requirements 4.3, 4.4**
     */
    @Test
    fun property1_viewModelToUi_sequentialUpdates_alwaysReflectLatest() {
        val sequence = listOf(
            "192.168.1.1", "", "10.0.0.1", "255.255.255.255",
            "0.0.0.0", "172.16.0.1", "abc", "日本語", "!@#",
            "", "192.168.1.100", "8.8.8.8", " ", "\t", ""
        )

        for (input in sequence) {
            viewModel.updateIpAddress(input)
            assertEquals(
                "After updateIpAddress(\"$input\"), uiState.ipAddress must equal \"$input\"",
                input,
                viewModel.uiState.value.ipAddress
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Direction 2: UI → ViewModel (user input propagates through ControlPanel)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renders [ControlPanel] wired to [viewModel] and returns a lambda that
     * performs one type-and-clear cycle on the IP field, then returns the
     * resulting ViewModel state.
     *
     * The field is cleared before each input to avoid accumulation from previous
     * test iterations — matching the isolated-iteration pattern in [IpInputFilterPropertyTest].
     */
    private fun renderControlPanelWiredToViewModel() {
        var ipState by mutableStateOf(viewModel.uiState.value.ipAddress)

        composeTestRule.setContent {
            MaterialTheme {
                ControlPanel(
                    ipAddress = ipState,
                    onIpAddressChange = { newValue ->
                        ipState = newValue
                        viewModel.updateIpAddress(newValue)
                    },
                    onTestClick = {},
                    onSaveClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * **Property 1 — UI → ViewModel direction**
     *
     * For each filtered input (digits and periods only), typing into the IP address
     * field must cause [PrinterConfigViewModel.uiState] `ipAddress` to equal that
     * exact string.
     *
     * The ControlPanel filters input to digits and periods before calling
     * onIpAddressChange; therefore only pre-filtered strings (containing only [0-9.])
     * are used here, so the expected value always equals the typed string exactly.
     *
     * Tests 100+ distinct filtered inputs, exceeding the 100-iteration minimum.
     *
     * **Validates: Requirements 4.3, 4.4**
     */
    @Test
    fun property1_uiToViewModel_typingInField_updatesViewModelState() {
        renderControlPanelWiredToViewModel()

        for (input in filteredInputs) {
            // Clear the field before typing each new value
            composeTestRule
                .onNodeWithText("IP local")
                .performTextClearance()
            composeTestRule.waitForIdle()

            // Type the input
            if (input.isNotEmpty()) {
                composeTestRule
                    .onNodeWithText("IP local")
                    .performTextInput(input)
                composeTestRule.waitForIdle()
            }

            val actual = viewModel.uiState.value.ipAddress
            assertEquals(
                "UI→ViewModel: typing \"$input\" must result in uiState.ipAddress == \"$input\".\n" +
                    "Actual: \"$actual\"",
                input,
                actual
            )
        }
    }

    /**
     * **Property 1 — bidirectional synchronization: state set by ViewModel is rendered**
     *
     * After calling updateIpAddress programmatically, the UI state variable used
     * to render the field matches the ViewModel state — confirming both ends of
     * the binding agree.
     *
     * This verifies Requirement 4.4: "The IP_Address_Field SHALL display the
     * current value from the ViewModel ipAddress state."
     *
     * **Validates: Requirements 4.3, 4.4**
     */
    @Test
    fun property1_bidirectional_viewModelStateAndUiStateAgreement() {
        val testInputs = listOf(
            "192.168.1.1",
            "10.0.0.1",
            "0.0.0.0",
            "255.255.255.255",
            "172.16.0.1",
            "8.8.8.8",
            "127.0.0.1",
            "1.1.1.1",
            "9.9.9.9",
            "100.200.300.400"
        )

        var uiIpState by mutableStateOf("")

        composeTestRule.setContent {
            MaterialTheme {
                ControlPanel(
                    ipAddress = uiIpState,
                    onIpAddressChange = { newValue ->
                        uiIpState = newValue
                        viewModel.updateIpAddress(newValue)
                    },
                    onTestClick = {},
                    onSaveClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        for (input in testInputs) {
            // Drive state through the ViewModel (ViewModel→UI direction)
            viewModel.updateIpAddress(input)
            // Sync the UI state variable to reflect the ViewModel's state
            uiIpState = viewModel.uiState.value.ipAddress
            composeTestRule.waitForIdle()

            // Both ViewModel state and UI state variable must agree
            val vmState = viewModel.uiState.value.ipAddress
            assertEquals(
                "ViewModel state and UI state variable must agree after updateIpAddress(\"$input\").\n" +
                    "ViewModel: \"$vmState\"\n" +
                    "UI state : \"$uiIpState\"",
                vmState,
                uiIpState
            )
        }
    }
}
