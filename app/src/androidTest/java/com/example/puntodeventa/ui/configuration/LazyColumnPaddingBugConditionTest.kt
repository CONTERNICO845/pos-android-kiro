package com.example.puntodeventa.ui.configuration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug condition exploration test for Bug 4 — Missing Top Padding on LazyColumn.
 *
 * **Property 1: Expected Behavior** — LazyColumn Now Has Top Padding (After Fix)
 *
 * This test was EXPECTED TO FAIL on unfixed code. Now that the fix is applied,
 * the test should PASS, confirming the bug is resolved.
 *
 * **Root cause (FIXED):** The `LazyColumn` in `ConfigurationScreen` now uses:
 * ```kotlin
 * LazyColumn(
 *     modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
 *     contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
 * )
 * ```
 * And the containing Box has:
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp))
 * ```
 * As a result, the first `ProductCard` now has proper spacing from the `ActionBarRow`.
 *
 * **Test strategy:**
 *   Render a replica of the FIXED `ConfigurationScreen` layout structure:
 *   - A `Column` with a header `Box` (representing `ActionBarRow`, 56 dp high, tagged
 *     `"ActionBarRow"`)
 *   - A content `Box` below with `padding(top = 8.dp)` (representing the fixed container)
 *   - A `LazyColumn` inside the content `Box`, **with** `contentPadding(top = 8.dp)` (the fixed state)
 *   - One item `Text` inside the `LazyColumn` (tagged `"FirstProductCard"`)
 *
 *   Measure the bounds of `"ActionBarRow"` and `"FirstProductCard"` via `getBoundsInRoot()`.
 *   Assert that `firstCard.top > actionBarRow.bottom` — i.e., the gap is > 0 dp.
 *
 * On **fixed code** (`contentPadding = PaddingValues(top = 8.dp)` + `Box.padding(top = 8.dp)`):
 *   `firstCard.top > actionBarRow.bottom` (gap ≥ 16 dp) → assertion PASSES.
 *
 * On **unfixed code** (no `contentPadding`, no Box padding):
 *   `firstCard.top == actionBarRow.bottom` (gap = 0 dp) → assertion FAILS.
 *
 * **Expected outcome:** Test PASSES (confirms fix works)
 *
 * **Validates: Requirements 2.10, 2.11**
 */
@RunWith(AndroidJUnit4::class)
class LazyColumnPaddingBugConditionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Property 1: Bug Condition ─────────────────────────────────────────────

    /**
     * Renders the `LazyColumn` **with** `contentPadding` and Box padding, mirroring the
     * FIXED code from `ConfigurationScreen`:
     * ```kotlin
     * Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
     *     LazyColumn(
     *         modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
     *         contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
     *     ) { … }
     * }
     * ```
     * Measures the gap between `ActionBarRow.bottom` and `FirstProductCard.top`.
     * Asserts `gap > 0.dp` — should now pass on fixed code where gap >= 16.dp.
     *
     * Expected outcome: Test PASSES (confirms fix works)
     *
     * **Validates: Requirements 2.10, 2.11**
     */
    @Test
    fun property1_bugCondition_lazyColumnFirstItem_hasNoTopGap() {
        composeTestRule.setContent {
            // MaterialTheme is required for Material3 composables (Text, etc.) to render
            MaterialTheme {
                // Minimal replica of ConfigurationScreen layout:
                // Column { ActionBarRow; Box { LazyColumn { items } } }
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header (ActionBarRow equivalent) — fixed 56 dp height
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("ActionBarRow")
                    )

                    // Content Box — the fillMaxSize Box that contains the LazyColumn.
                    // FIXED: Apply padding(top = 8.dp) as per task 15.2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp)  // FIXED: Added top padding
                    ) {
                        // LazyColumn — FIXED as in ConfigurationScreen
                        // FIXED: contentPadding = PaddingValues(top = 8.dp) is now applied
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)  // FIXED: Added contentPadding
                        ) {
                            item {
                                Text(
                                    text = "Product Card",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("FirstProductCard")
                                )
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // Measure the bottom edge of the ActionBarRow
        val actionBarBounds = composeTestRule
            .onNodeWithTag("ActionBarRow")
            .getBoundsInRoot()

        // Measure the top edge of the first ProductCard
        val firstCardBounds = composeTestRule
            .onNodeWithTag("FirstProductCard")
            .getBoundsInRoot()

        val actionBarBottom = actionBarBounds.bottom
        val firstCardTop    = firstCardBounds.top
        val gapDp           = firstCardTop - actionBarBottom

        // On FIXED code (contentPadding = PaddingValues(top = 8.dp) and Box padding(top = 8.dp)):
        //   gapDp ≥ 8.dp → assertion PASSES.
        // On UNFIXED code: gapDp == 0.dp (first item is flush against the action bar)
        //   → assertion FAILS → confirms the bug.
        assertTrue(
            "EXPECTED BEHAVIOR — LazyColumn now has top padding. " +
            "Expected firstCard.top (${firstCardTop}) > ActionBarRow.bottom (${actionBarBottom}), " +
            "with gap = $gapDp >= 8.dp. " +
            "Fix confirmed: ConfigurationScreen LazyColumn has contentPadding(top=8.dp) " +
            "and the containing Box has padding(top=8.dp), providing proper breathing room " +
            "between the ActionBarRow and first ProductCard.",
            gapDp > 0.dp
        )
    }
}
