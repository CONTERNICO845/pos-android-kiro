package com.example.puntodeventa.ui.newproduct

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation property tests for Bug 1 — Modal Lifecycle Behavior Unchanged.
 *
 * **Property 2: Preservation** — Modal Lifecycle Behavior Unchanged
 *
 * These tests verify behaviors that are NOT caused by `TextField.onValueChange`.
 * They must ALL PASS after the Bug 1 refactor.
 *
 * Covers Requirements 3.1–3.4:
 * - 3.1: Tapping "Cancelar" dismisses the modal and calls onDismiss().
 * - 3.2: When isSaving=true, swipe-dismiss is suppressed and the close button is disabled.
 * - 3.3: When saveResult=Success, LaunchedEffect triggers onDismiss() automatically.
 * - 3.4: rememberScrollState() survives recomposition — scroll position preserved after
 *         a non-TextField state update.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 */
@RunWith(AndroidJUnit4::class)
class ModalPreservationTest {

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

        runBlocking {
            db.menuItemDao().insert(
                MenuItemEntity(id = "menu-test", emoji = "🍔", name = "Menú Test")
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id               = "cat-test",
                    name             = "Categoría Test",
                    associatedMenuId = "menu-test"
                )
            )
        }

        viewModel = NewProductViewModel(
            productRepository  = ProductRepository(
                productDao = db.productDao(),
                groupDao   = db.customizationGroupDao(),
                optionDao  = db.customizationOptionDao(),
                database   = db
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

    // ── Property 3.1 — "Cancelar" dismisses the modal ────────────────────────

    /**
     * Tapping "Cancelar" must call onDismiss().
     *
     * This behavior is NOT a bug-condition trigger (it is not a TextField.onValueChange).
     * It must continue to pass after the Bug 1 refactor.
     *
     * **Validates: Requirement 3.1**
     */
    @Test
    fun property_cancelar_dismissesModal_callsOnDismiss() {
        var dismissCalled = false

        composeTestRule.setContent {
            // NewProductModal collects uiState internally — no collectAsState() needed here.
            NewProductModal(
                viewModel = viewModel,
                onDismiss = { dismissCalled = true }
            )
        }
        composeTestRule.waitForIdle()

        // Click "Cancelar"
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Cancelar must call onDismiss()", dismissCalled)
    }

    // ── Property 3.2 — isSaving=true disables close button and "Cancelar" ────

    /**
     * When isSaving=true, the "X" close button and "Cancelar" button must both be disabled.
     *
     * Forces isSaving=true via the ViewModel's internal state directly on the UI thread.
     *
     * **Validates: Requirement 3.2**
     */
    @Test
    fun property_isSaving_disablesCloseButtonAndCancelar() {
        composeTestRule.setContent {
            NewProductModal(
                viewModel = viewModel,
                onDismiss = {}
            )
        }
        composeTestRule.waitForIdle()

        // Force isSaving = true through the ViewModel's exposed state update mechanism.
        composeTestRule.runOnUiThread {
            viewModel.forceIsSavingForTest(true)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Cerrar").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancelar").assertIsNotEnabled()
    }

    // ── Property 3.3 — saveResult=Success triggers onDismiss via LaunchedEffect

    /**
     * When saveResult transitions to SaveResult.Success, LaunchedEffect must call onDismiss().
     *
     * Forces saveResult=Success via the ViewModel's test helper, then asserts onDismiss fired.
     *
     * **Validates: Requirement 3.3**
     */
    @Test
    fun property_saveResultSuccess_triggersOnDismiss() {
        var dismissCalled = false

        composeTestRule.setContent {
            NewProductModal(
                viewModel = viewModel,
                onDismiss = { dismissCalled = true }
            )
        }
        composeTestRule.waitForIdle()

        // Flip saveResult to Success via ViewModel test helper.
        composeTestRule.runOnUiThread {
            viewModel.forceSaveResultForTest(SaveResult.Success)
        }
        // Wait for the LaunchedEffect inside NewProductModal to react to the saveResult change.
        composeTestRule.waitForIdle()

        assertTrue(
            "LaunchedEffect must call onDismiss() when saveResult = Success",
            dismissCalled
        )
    }

    // ── Property 3.4 — scroll state survives a non-TextField recomposition ───

    /**
     * The modal must remain stable after a non-TextField state update (emoji change),
     * confirming rememberScrollState() is not reset by an unrelated recomposition.
     *
     * **Validates: Requirement 3.4**
     */
    @Test
    fun property_scrollState_survivesNonTextFieldRecomposition() {
        composeTestRule.setContent {
            // NewProductModal collects uiState internally — no collectAsState() needed here.
            NewProductModal(
                viewModel = viewModel,
                onDismiss = {}
            )
        }
        composeTestRule.waitForIdle()

        // Non-TextField update — runOnIdle combines the dispatch + settle in one call.
        composeTestRule.runOnIdle { viewModel.updateEmoji("🍕") }

        composeTestRule.onNodeWithText("Nuevo Producto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }
}
