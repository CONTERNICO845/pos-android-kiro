package com.example.puntodeventa.ui.printer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Property-style instrumented tests for [PrinterConfigViewModel] state consistency.
 *
 * Uses the real [PrinterPreferencesRepository] with the instrumentation target
 * context.  The property under test — updateIpAddress — only mutates the
 * in-memory StateFlow and never touches SharedPreferences, so the on-disk
 * state has no effect on these assertions.
 *
 * **Property 7: ViewModel State Update Consistency**
 * For any string input to updateIpAddress, the ViewModel shall update ipAddress
 * in uiState to exactly match the input.
 *
 * **Validates: Requirements 10.5**
 */
@RunWith(AndroidJUnit4::class)
class PrinterConfigViewModelTest {

    private lateinit var viewModel: PrinterConfigViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = PrinterPreferencesRepository(context)
        viewModel = PrinterConfigViewModel(repo)
    }

    // ── Diverse string inputs for property-style coverage ─────────────────────

    /**
     * Representative set of diverse string inputs that probe boundary and
     * interesting cases: empty string, single char, long string, IP-like
     * values, special characters, and Unicode text.
     */
    private val propertyInputs: List<String> = listOf(
        // Boundary / empty
        "",
        " ",
        // Single character
        "a",
        "0",
        // Typical IP-like strings
        "192.168.1.1",
        "0.0.0.0",
        "255.255.255.255",
        "999.999.999.999",
        // Partial / malformed IPs
        "192.",
        "192.168.",
        ".1.1.1",
        "...",
        // Long string (256 chars)
        "a".repeat(256),
        // Special characters
        "!@#\$%^&*()",
        "\t\n\r",
        "hello world",
        // Unicode
        "日本語",
        "ñoño",
        "🖨️printer",
        // Whitespace-padded
        "   192.168.1.1   ",
        // Null-byte embedded
        "\u0000null-byte",
    )

    // ── Property 7: ViewModel State Update Consistency ────────────────────────

    /**
     * **Property 7: ViewModel State Update Consistency**
     *
     * After calling [PrinterConfigViewModel.updateIpAddress] with any string,
     * [PrinterConfigViewModel.uiState].value.ipAddress must equal that exact string.
     *
     * Tests a diverse range of inputs to approximate universal quantification
     * over all String values.
     *
     * **Validates: Requirements 10.5**
     */
    @Test
    fun property7_updateIpAddress_uiState_reflects_exact_input_for_diverse_inputs() {
        for (input in propertyInputs) {
            viewModel.updateIpAddress(input)

            val actual = viewModel.uiState.value.ipAddress
            assertEquals(
                "uiState.ipAddress should equal the input exactly.\n" +
                    "Input  : \"$input\"\n" +
                    "Actual : \"$actual\"",
                input,
                actual
            )
        }
    }

    /**
     * **Property 7 — sequential updates**
     *
     * Calling updateIpAddress multiple times must always leave uiState reflecting
     * the most-recent call, with no residual state from earlier calls.
     *
     * **Validates: Requirements 10.5**
     */
    @Test
    fun property7_sequential_updates_always_reflect_latest_input() {
        val sequence = listOf("192.168.1.1", "", "10.0.0.1", "abc", "255.255.255.255")

        for (input in sequence) {
            viewModel.updateIpAddress(input)
            assertEquals(
                "After updateIpAddress(\"$input\"), uiState.ipAddress must equal \"$input\"",
                input,
                viewModel.uiState.value.ipAddress
            )
        }
    }

    /**
     * **Property 7 — immutability of unrelated state fields**
     *
     * Calling updateIpAddress must only mutate ipAddress; all other UiState
     * fields (isLoading, errorMessage, connectionStatus, lastTestResult) must
     * retain their values from before the call.
     *
     * **Validates: Requirements 10.5**
     */
    @Test
    fun property7_updateIpAddress_does_not_mutate_other_uiState_fields() {
        val stateBefore = viewModel.uiState.value

        viewModel.updateIpAddress("192.168.0.100")

        val stateAfter = viewModel.uiState.value
        assertEquals("isLoading must not change", stateBefore.isLoading, stateAfter.isLoading)
        assertEquals("errorMessage must not change", stateBefore.errorMessage, stateAfter.errorMessage)
        assertEquals(
            "connectionStatus must not change",
            stateBefore.connectionStatus,
            stateAfter.connectionStatus
        )
        assertEquals("lastTestResult must not change", stateBefore.lastTestResult, stateAfter.lastTestResult)
    }
}
