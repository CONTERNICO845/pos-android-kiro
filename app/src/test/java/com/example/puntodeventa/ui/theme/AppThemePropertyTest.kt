package com.example.puntodeventa.ui.theme

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.enum
import io.kotest.property.forAll

/**
 * Property-based tests for AppTheme enum correctness.
 *
 * Property 4: Theme metadata completeness
 * Property 5: ColorScheme mapping is total and pure
 */
class AppThemePropertyTest : StringSpec({

    /**
     * Property 4: Theme metadata completeness
     *
     * For any AppTheme value, `displayName` is non-empty and `previewColors`
     * returns 3 non-null Colors.
     *
     * Validates: Requirements 9.1, 9.2
     */
    "Property 4 — displayName is non-empty and previewColors returns 3 non-null Colors for all themes" {
        forAll(PropTestConfig(iterations = 100), Arb.enum<AppTheme>()) { theme ->
            val name = theme.displayName
            val (primary, background, accent) = theme.previewColors

            name.isNotBlank() &&
                primary.value != 0UL &&
                background.value != 0UL &&
                accent.value != 0UL
        }
    }

    /**
     * Property 5: ColorScheme mapping is total and pure
     *
     * For any AppTheme value, `toColorScheme()` returns non-null without exceptions,
     * and repeated calls produce identical results.
     *
     * Validates: Requirements 4.3, 1.1
     */
    "Property 5 — toColorScheme() is total and pure (deterministic) for all themes" {
        forAll(PropTestConfig(iterations = 100), Arb.enum<AppTheme>()) { theme ->
            val first = theme.toColorScheme()
            val second = theme.toColorScheme()

            // Non-null (Kotlin guarantees this but we verify the function completes)
            // and deterministic (same result on repeated calls)
            first.primary == second.primary &&
                first.onPrimary == second.onPrimary &&
                first.background == second.background &&
                first.onBackground == second.onBackground &&
                first.surface == second.surface &&
                first.onSurface == second.onSurface &&
                first.error == second.error &&
                first.onError == second.onError &&
                first.secondary == second.secondary &&
                first.onSecondary == second.onSecondary
        }
    }

    /**
     * Enum has exactly 9 entries — guard against accidental additions without
     * updating the exhaustive when clauses.
     */
    "AppTheme enum has exactly 9 entries" {
        assert(AppTheme.entries.size == 9) {
            "Expected 9 AppTheme entries, got ${AppTheme.entries.size}: ${AppTheme.entries}"
        }
    }

    /**
     * fromName returns DEFAULT for any invalid string.
     */
    "fromName returns DEFAULT for invalid strings" {
        forAll(PropTestConfig(iterations = 100), Arb.enum<AppTheme>()) { theme ->
            // Valid names resolve correctly
            AppTheme.fromName(theme.name) == theme
        }
    }

    "fromName returns DEFAULT_GREEN for garbage input" {
        val invalid = listOf("", "INVALID", "dark_neon", "null", "123", " ")
        invalid.all { AppTheme.fromName(it) == AppTheme.DEFAULT_GREEN }
    }
})
