package com.example.puntodeventa.ui.printer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Property-based compliance test for the Printer_Config_Screen color palette.
 *
 * **Feature: printer-config-ui, Property 8: Color Palette Compliance**
 *
 * For any color reference used in the Printer_Config_Screen components, the color
 * shall be defined in Color.kt rather than hardcoded inline.
 *
 * This test performs static source-code analysis on the printer UI Kotlin source files.
 * It scans each composable file for raw inline [Color(0xXX...)] constructor calls —
 * the canonical form of a "hardcoded color value" — and asserts that none are present.
 *
 * ## Rationale for static analysis approach
 *
 * Compose does not expose a runtime color registry that would let a UI test interrogate
 * *where* a color value came from (token vs. inline literal). Static analysis of the
 * source files is therefore the most reliable and practical way to enforce this invariant.
 *
 * ## What counts as a "hardcoded color"
 *
 * A hardcoded color is any call to the [androidx.compose.ui.graphics.Color] constructor
 * with a literal hex argument, such as:
 *
 *   `Color(0xFF1A1A1A)` — raw ARGB long literal
 *   `Color(0xFFRRGGBB)` — any 0x-prefixed hex constant
 *
 * Named color tokens imported from Color.kt (e.g. `CardBackground`, `InputBorder`)
 * and Compose built-ins accessed via the Color companion object (e.g. `Color.White`,
 * `Color.Red`, `Color.Transparent`) are not flagged by this check.  The test
 * intentionally focuses on the most unambiguous violation — raw hex literals — which
 * cannot be confused with token references.
 *
 * ## Files scanned (input space)
 *
 * The printer screen is composed of the following files; the property must hold for
 * *every* file in this set (universal quantification over the file space):
 *
 *   - ControlPanel.kt
 *   - StatusPanel.kt
 *   - StaticSettingRow.kt
 *   - StatusInfoRow.kt
 *   - PrinterScreen.kt
 *
 * **Validates: Requirements 11.4**
 */
@RunWith(AndroidJUnit4::class)
class ColorPaletteCompliancePropertyTest {

    // ── Source files under test ───────────────────────────────────────────────

    /**
     * Relative paths of the Printer_Config_Screen composable source files that must
     * not contain inline [Color(0xXX...)] constructor calls.
     *
     * Paths are relative to the project root so the test works regardless of the
     * device/emulator data partition by locating them via the instrumentation context.
     *
     * The test resolves the real filesystem path from the APK's source directory
     * embedded at build time via a known resource file, or falls back to reading the
     * source files via the InstrumentationRegistry context's assets if they were
     * bundled. In practice the simplest robust approach for an instrumented test on a
     * connected device or emulator is to bundle the relevant source files as test
     * assets and read them at runtime.
     */
    private val printerSourceFiles = listOf(
        "ControlPanel.kt",
        "StatusPanel.kt",
        "StaticSettingRow.kt",
        "StatusInfoRow.kt",
        "PrinterScreen.kt"
    )

    /**
     * Regex that matches raw hexadecimal Color constructor calls.
     *
     * Matches: Color(0xFF______) where the argument starts with 0x or 0X
     * Does NOT match: Color.White, Color.Red, CardBackground, etc.
     *
     * The pattern uses a word boundary after "Color" to avoid matching
     * colour-named tokens that happen to start with "Color" (e.g. no such tokens
     * exist in this project but it is defensive).
     */
    private val hardcodedColorPattern = Regex(
        """(?<![A-Za-z_])Color\s*\(\s*0[xX][0-9A-Fa-f]+"""
    )

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Returns the content of a printer source file by reading it from the
     * test assets bundled in the androidTest APK.
     *
     * The files are placed in `androidTest/assets/printer_sources/` by the build
     * configuration and read here at runtime.
     */
    private fun readSourceFileFromAssets(fileName: String): String {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets
            .open("printer_sources/$fileName")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    // ── Property 8 ────────────────────────────────────────────────────────────

    /**
     * **Property 8: Color Palette Compliance**
     *
     * For every Printer_Config_Screen source file, scans the source text and
     * asserts that no raw [Color(0xXX...)] constructor calls appear.
     *
     * Input space: all five printer composable source files (universal quantification).
     *
     * Expected outcome on compliant code: zero matches in every file — test PASSES.
     * Expected outcome on non-compliant code: ≥1 match found — test FAILS with a
     * descriptive counterexample listing the file name and the offending line(s).
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_noHardcodedColorConstructorCalls_inAnyPrinterSourceFile() {
        val violations = mutableListOf<String>()

        for (fileName in printerSourceFiles) {
            val source = readSourceFileFromAssets(fileName)
            val lines = source.lines()

            lines.forEachIndexed { lineIndex, line ->
                // Skip single-line comments
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//")) return@forEachIndexed

                val matchResult = hardcodedColorPattern.find(line)
                if (matchResult != null) {
                    violations += "$fileName:${lineIndex + 1}: ${line.trim()}"
                }
            }
        }

        assertTrue(
            buildString {
                appendLine(
                    "PROPERTY 8 VIOLATION — Hardcoded Color(0xXX...) constructor calls found " +
                    "in Printer_Config_Screen source files."
                )
                appendLine(
                    "All colors used in printer composables must be defined as named tokens " +
                    "in Color.kt and referenced by name (e.g. CardBackground, InputBorder)."
                )
                appendLine()
                appendLine("Counterexamples (${violations.size} violation(s)):")
                violations.forEach { appendLine("  • $it") }
                appendLine()
                appendLine(
                    "Fix: replace each Color(0xXX...) literal with the corresponding token " +
                    "from Color.kt, or add a new named token if one does not yet exist."
                )
            },
            violations.isEmpty()
        )
    }

    /**
     * **Property 8 (per-file sub-check): ControlPanel has no hardcoded hex colors**
     *
     * Provides a focused failure message specifically for ControlPanel.kt, which is
     * the most likely file to contain inline color values given its complex styling.
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_controlPanel_hasNoHardcodedHexColorConstructors() {
        assertNoHardcodedColors("ControlPanel.kt")
    }

    /**
     * **Property 8 (per-file sub-check): StatusPanel has no hardcoded hex colors**
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_statusPanel_hasNoHardcodedHexColorConstructors() {
        assertNoHardcodedColors("StatusPanel.kt")
    }

    /**
     * **Property 8 (per-file sub-check): StaticSettingRow has no hardcoded hex colors**
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_staticSettingRow_hasNoHardcodedHexColorConstructors() {
        assertNoHardcodedColors("StaticSettingRow.kt")
    }

    /**
     * **Property 8 (per-file sub-check): StatusInfoRow has no hardcoded hex colors**
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_statusInfoRow_hasNoHardcodedHexColorConstructors() {
        assertNoHardcodedColors("StatusInfoRow.kt")
    }

    /**
     * **Property 8 (per-file sub-check): PrinterScreen has no hardcoded hex colors**
     *
     * **Validates: Requirements 11.4**
     */
    @Test
    fun property8_printerScreen_hasNoHardcodedHexColorConstructors() {
        assertNoHardcodedColors("PrinterScreen.kt")
    }

    // ── Shared assertion helper ───────────────────────────────────────────────

    private fun assertNoHardcodedColors(fileName: String) {
        val source = readSourceFileFromAssets(fileName)
        val lines = source.lines()
        val violations = mutableListOf<String>()

        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("//")) return@forEachIndexed
            val match = hardcodedColorPattern.find(line)
            if (match != null) {
                violations += "  Line ${lineIndex + 1}: ${line.trim()}"
            }
        }

        assertTrue(
            buildString {
                appendLine("PROPERTY 8 VIOLATION in $fileName:")
                appendLine(
                    "Found ${violations.size} hardcoded Color(0xXX...) call(s). " +
                    "All colors must be named tokens from Color.kt."
                )
                appendLine()
                violations.forEach { appendLine(it) }
            },
            violations.isEmpty()
        )
    }
}
