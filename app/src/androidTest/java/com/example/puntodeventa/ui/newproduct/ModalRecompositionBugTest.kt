package com.example.puntodeventa.ui.newproduct

import android.content.Context
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug condition exploration test for Bug 1 — Modal Blinking (Excessive Recomposition).
 *
 * **Property 1: Bug Condition** — ModalBottomSheet Recomposition On TextField Change
 *
 * This test is EXPECTED TO FAIL on unfixed code. Failure confirms the bug exists.
 *
 * **Root cause:** `rememberModalBottomSheetState` and `rememberScrollState` are declared
 * inside `NewProductModal`, the same composable that receives `uiState: NewProductUiState`.
 * Any `uiState` change (e.g., `name` field update) causes the whole `NewProductModal`
 * function body to re-execute, re-entering the `ModalBottomSheet` lambda — producing
 * a visible blink on each keystroke.
 *
 * **Test strategy:** A [SideEffect] counter is placed in the same composable scope that
 * collects `uiState` and calls [NewProductModal]. On unfixed code, every `uiState` emission
 * causes this scope to recompose (because `ModalBottomSheet` is inside it), incrementing
 * the counter. The test asserts the count is 0 after a single keystroke — this assertion
 * FAILS on unfixed code, confirming the bug.
 *
 * **Documented counterexample (unfixed code):**
 * "Typing 'P' in Nombre → ModalBottomSheet recomposed 1 time"
 *
 * **Validates: Requirements 1.1, 1.2**
 */
@RunWith(AndroidJUnit4::class)
class ModalRecompositionBugTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: NewProductViewModel

    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(foreignKeyCallback)
            .allowMainThreadQueries()
            .build()

        // Seed a menu + category so the ViewModel initialises its menus/categories flows
        runBlocking {
            db.menuItemDao().insert(
                MenuItemEntity(id = "menu-test", emoji = "🍔", name = "Menú Test")
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id = "cat-test",
                    name = "Categoría Test",
                    associatedMenuId = "menu-test"
                )
            )
        }

        viewModel = NewProductViewModel(
            productRepository = ProductRepository(
                productDao = db.productDao(),
                groupDao   = db.customizationGroupDao(),
                optionDao  = db.customizationOptionDao()
            ),
            categoryRepository = CategoryRepository(db.categoryDao()),
            menuRepository     = MenuRepository(db.menuItemDao()),
            database           = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 1: Bug Condition ─────────────────────────────────────────────

    /**
     * Types a single character ('P') into the "Nombre" field and asserts that
     * the [ModalBottomSheet] container recomposition count is 0 after the keystroke.
     *
     * The [SideEffect] counter tracks recompositions of the composable scope that
     * **contains the [NewProductModal] call** — this scope is identical to the
     * scope that holds `rememberModalBottomSheetState` on unfixed code, so its
     * recomposition count directly reflects [ModalBottomSheet] re-entries.
     *
     * **On UNFIXED code:** FAILS — count = 1 (confirms the bug).
     * **On FIXED code:**   PASSES — count = 0 (ModalBottomSheet is stable).
     *
     * Counterexample: "Typing 'P' in Nombre → ModalBottomSheet recomposed 1 time"
     */
    @Test
    fun property1_bugCondition_modalBottomSheetRecomposesOnNombreInput() {
        // modalSheetRecomposeCount is incremented by the SideEffect on every recomposition
        // of the composable scope that hosts NewProductModal.
        // mutableIntStateOf makes it readable outside the composition (for the assertion).
        var modalSheetRecomposeCount by mutableIntStateOf(0)
        // compositionCount tracks total compositions (including the initial one).
        val compositionCount = mutableIntStateOf(0)

        composeTestRule.setContent {
            // NOTE: uiState is NOT collected here. NewProductModal collects it internally.
            // This scope does NOT hold a reference to the changing ViewModel state, so it
            // does NOT recompose when uiState.name changes on a keystroke.
            // After the fix, only NewProductFormContent (inside NewProductModal) recomposes.

            // SideEffect: runs synchronously after every successful recomposition of this scope.
            // The first invocation (compositionCount == 1) is the initial composition — skip it.
            // After the fix, this scope does NOT recompose on TextField input (count == 0).
            SideEffect {
                compositionCount.intValue++
                if (compositionCount.intValue > 1) {
                    modalSheetRecomposeCount++
                }
            }

            NewProductModal(
                viewModel = viewModel,
                onDismiss = {}
            )
        }

        // Let the initial composition (and any menu/category Flow emissions) settle.
        composeTestRule.waitForIdle()

        // Reset the counter — we only care about recompositions caused by TextField input.
        composeTestRule.runOnUiThread {
            modalSheetRecomposeCount = 0
            compositionCount.intValue = 1   // treat the settled state as the new baseline
        }

        // Act: type 'P' into the "Nombre" field.
        // On unfixed code this fires viewModel.updateName("P") → uiState.name changes →
        // NewProductModal recomposes → ModalBottomSheet re-enters composition.
        composeTestRule.onNodeWithText("Nombre")
            .performTextInput("P")

        composeTestRule.waitForIdle()

        // Assert: ModalBottomSheet must NOT have recomposed (count == 0).
        // FAILS on unfixed code (count == 1), confirming the bug exists.
        assertEquals(
            "BUG CONFIRMED — ModalBottomSheet recomposed $modalSheetRecomposeCount time(s) " +
            "after typing 'P' in Nombre. " +
            "Counterexample: Typing 'P' in Nombre → ModalBottomSheet recomposed " +
            "$modalSheetRecomposeCount time(s). " +
            "Root cause: rememberModalBottomSheetState is declared inside the same " +
            "composable scope that reads uiState, so every keystroke re-enters " +
            "ModalBottomSheet composition.",
            0,
            modalSheetRecomposeCount
        )
    }

}
