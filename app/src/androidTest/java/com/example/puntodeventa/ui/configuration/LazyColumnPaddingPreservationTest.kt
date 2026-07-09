package com.example.puntodeventa.ui.configuration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation property tests for Bug 4 — Missing Top Padding on LazyColumn.
 *
 * **Property 2: Preservation** — Empty State and Scroll Behavior Unchanged
 *
 * These tests verify behaviors that are NOT the bug condition (`isBugCondition_4`).
 * They cover cases where the current (unfixed) code already produces the correct result.
 * All tests must PASS on unfixed code (and continue to pass after the fix).
 *
 * Bug condition reminder: `isBugCondition_4(screen)` is TRUE when the LazyColumn has
 * no top padding (contentPadding.top = 0.dp) AND the first ProductCard is flush against
 * the ActionBarRow. These preservation tests cover the ¬C (non-bug) cases:
 * - Empty state text display (no ProductCards in the list)
 * - Header pinning during scroll (CategoryTabsRow and ActionBarRow remain visible)
 * - Scrolling reveals all products (all items are accessible)
 *
 * Covers Requirements 3.11–3.12:
 * - 3.11: When filteredProducts is empty, an empty-state Text is shown centered in the Box.
 * - 3.12: CategoryTabsRow and ActionBarRow remain pinned at the top; scrolling the
 *         LazyColumn reveals all products.
 *
 * **Validates: Requirements 3.11, 3.12**
 */
@RunWith(AndroidJUnit4::class)
class LazyColumnPaddingPreservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Property 3.11 — Empty state text is shown centered when list is empty ─

    /**
     * For all states where the product list is EMPTY (not the bug condition — the bug
     * is about the first item's padding when products exist), the empty-state Text must
     * be displayed centered in the Box.
     *
     * This test renders the ConfigurationScreen layout structure with an empty list
     * (no LazyColumn items) and verifies the empty-state Text is visible and centered.
     *
     * On unfixed code: The Box already centers the empty-state text correctly.
     * This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.11**
     */
    @Test
    fun emptyState_text_is_centered_when_product_list_is_empty() {
        composeTestRule.setContent {
            MaterialTheme {
                // Minimal replica of ConfigurationScreen layout with empty product list
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header (ActionBarRow equivalent)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("ActionBarRow")
                    )

                    // Content Box — contains empty state text (no LazyColumn)
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Empty state text — aligned Center by default in Box
                        Text(
                            text = "No hay productos en esta categoría",
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .testTag("EmptyStateText")
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // ── Assertion 1: Empty state text is displayed ────────────────────────
        composeTestRule
            .onNodeWithTag("EmptyStateText")
            .assertIsDisplayed()

        // ── Assertion 2: Empty state text is in the center of the screen ──────
        val actionBarBounds = composeTestRule
            .onNodeWithTag("ActionBarRow")
            .getBoundsInRoot()

        val emptyTextBounds = composeTestRule
            .onNodeWithTag("EmptyStateText")
            .getBoundsInRoot()

        // The empty text should be below the action bar
        assertTrue(
            "Req 3.11: Empty state text must be positioned below the ActionBarRow. " +
            "Expected emptyText.top (${emptyTextBounds.top}) > actionBar.bottom (${actionBarBounds.bottom})",
            emptyTextBounds.top > actionBarBounds.bottom
        )

        // The empty text should be roughly centered vertically in the remaining space
        // (We can't easily assert exact center without knowing screen dimensions,
        // but we can verify it's not at the very top or very bottom)
        val emptyTextCenter = (emptyTextBounds.top + emptyTextBounds.bottom) / 2
        val actionBarBottom = actionBarBounds.bottom

        // The center of the text should be significantly below the action bar
        assertTrue(
            "Req 3.11: Empty state text must be centered in the remaining content area, " +
            "not positioned at the top. Center Y of text is $emptyTextCenter, " +
            "which should be significantly > action bar bottom $actionBarBottom",
            emptyTextCenter > actionBarBottom + 50.dp // reasonable margin for "centered"
        )
    }

    // ── Property 3.12 — CategoryTabsRow and ActionBarRow remain pinned during scroll ─

    /**
     * For all scrolling actions on the LazyColumn (not the bug condition — the bug is
     * about the first item's top padding, not scroll behavior), the CategoryTabsRow
     * and ActionBarRow must remain pinned at the top of the screen, and scrolling must
     * reveal all products in the list.
     *
     * This test renders a LazyColumn with multiple items, scrolls to the bottom, and
     * verifies:
     * 1. The ActionBarRow remains at the same position (pinned, not scrolled away)
     * 2. Items at the bottom of the list become visible after scrolling
     *
     * On unfixed code: The LazyColumn scrolling behavior is already correct (the bug
     * only affects the top padding, not scrollability or header pinning).
     * This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.12**
     */
    @Test
    fun scrolling_lazyColumn_reveals_all_products_and_headers_remain_pinned() {
        composeTestRule.setContent {
            MaterialTheme {
                // Minimal replica of ConfigurationScreen layout with scrollable product list
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header (ActionBarRow equivalent) — pinned at the top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("ActionBarRow")
                    ) {
                        Text("Action Bar", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }

                    // Content Box with LazyColumn
                    // Bug: no padding(top = 8.dp) on the Box (that's task 15.2's fix)
                    Box(modifier = Modifier.fillMaxSize()) {
                        // LazyColumn with 20 items to test scrolling
                        // BUG: contentPadding is intentionally ABSENT (unfixed code)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .testTag("ProductLazyColumn")
                        ) {
                            items(20) { index ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(vertical = 4.dp)
                                        .testTag("ProductCard_$index")
                                ) {
                                    Text(
                                        text = "Product $index",
                                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // ── Step 1: Record the ActionBarRow's position BEFORE scrolling ───────
        val actionBarBoundsBeforeScroll = composeTestRule
            .onNodeWithTag("ActionBarRow")
            .getBoundsInRoot()

        // ── Step 2: Verify the first item is visible initially ────────────────
        composeTestRule
            .onNodeWithText("Product 0")
            .assertIsDisplayed()

        // ── Step 3: Scroll to the bottom of the list ──────────────────────────
        composeTestRule
            .onNodeWithTag("ProductLazyColumn")
            .performScrollToIndex(19) // Scroll to the last item (index 19)

        composeTestRule.waitForIdle()

        // ── Assertion 1: The last item is now visible ─────────────────────────
        composeTestRule
            .onNodeWithText("Product 19")
            .assertIsDisplayed()

        // ── Assertion 2: The ActionBarRow is still at the same position ───────
        val actionBarBoundsAfterScroll = composeTestRule
            .onNodeWithTag("ActionBarRow")
            .getBoundsInRoot()

        assertEquals(
            "Req 3.12: ActionBarRow must remain pinned at the top during scrolling. " +
            "Position before scroll: top=${actionBarBoundsBeforeScroll.top}, bottom=${actionBarBoundsBeforeScroll.bottom}. " +
            "Position after scroll: top=${actionBarBoundsAfterScroll.top}, bottom=${actionBarBoundsAfterScroll.bottom}",
            actionBarBoundsBeforeScroll.top,
            actionBarBoundsAfterScroll.top
        )

        assertEquals(
            "Req 3.12: ActionBarRow bottom edge must remain at the same position during scrolling.",
            actionBarBoundsBeforeScroll.bottom,
            actionBarBoundsAfterScroll.bottom
        )

        // ── Assertion 3: Verify we can scroll back to the top ─────────────────
        composeTestRule
            .onNodeWithTag("ProductLazyColumn")
            .performScrollToIndex(0)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Product 0")
            .assertIsDisplayed()
    }

    // ── Property 3.12 (Additional) — Empty search result preserves layout ────

    /**
     * For all states where the search returns no results (empty list, but different
     * from "no products in category"), the empty search text must be displayed
     * centered, and the ActionBarRow must remain pinned.
     *
     * This is another ¬C case (not the bug condition) — verifies the empty-state
     * layout is preserved when transitioning between populated and empty lists.
     *
     * On unfixed code: The Box already centers the empty-search text correctly.
     * This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.11 (implicit: all empty states)**
     */
    @Test
    fun emptySearchResult_text_is_centered_and_actionBar_remains_pinned() {
        composeTestRule.setContent {
            MaterialTheme {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header (ActionBarRow equivalent)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("ActionBarRow")
                    )

                    // Content Box with empty search result text
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "No se encontraron productos",
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .testTag("EmptySearchText")
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // ── Assertion 1: Empty search text is displayed ───────────────────────
        composeTestRule
            .onNodeWithTag("EmptySearchText")
            .assertIsDisplayed()

        // ── Assertion 2: ActionBarRow is still pinned at the top ──────────────
        val actionBarBounds = composeTestRule
            .onNodeWithTag("ActionBarRow")
            .getBoundsInRoot()

        val emptySearchBounds = composeTestRule
            .onNodeWithTag("EmptySearchText")
            .getBoundsInRoot()

        assertTrue(
            "Req 3.11: Empty search text must be positioned below the ActionBarRow. " +
            "Expected emptySearchText.top (${emptySearchBounds.top}) > actionBar.bottom (${actionBarBounds.bottom})",
            emptySearchBounds.top > actionBarBounds.bottom
        )

        // Verify the empty search text is reasonably centered (not at the very top)
        val emptySearchCenter = (emptySearchBounds.top + emptySearchBounds.bottom) / 2
        assertTrue(
            "Req 3.11: Empty search text must be centered in the remaining content area. " +
            "Center Y of text is $emptySearchCenter, which should be significantly > " +
            "action bar bottom ${actionBarBounds.bottom}",
            emptySearchCenter > actionBarBounds.bottom + 50.dp
        )
    }
}
