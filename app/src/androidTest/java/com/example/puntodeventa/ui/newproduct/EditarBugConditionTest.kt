package com.example.puntodeventa.ui.newproduct

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import com.example.puntodeventa.ui.configuration.ConfigurationScreen
import com.example.puntodeventa.ui.configuration.ConfigurationViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug condition exploration test for Bug 2 — Inactive "Editar" Button.
 *
 * **Property 1: Bug Condition** — Editar Does Not Open Pre-Populated Modal
 *
 * This test is EXPECTED TO FAIL on unfixed code. Failure confirms the bug exists.
 *
 * **Root cause:** The `onEditar` lambda in `ConfigurationScreen` only logs
 * `"Editar: <productId>"` and calls `viewModel.setExpandedProductMenu(null)`.
 * It never calls `newProductViewModel.loadForEdit(product)` nor sets `showModal = true`.
 * As a result:
 *   - `showModal` remains `false` → the modal is never rendered
 *   - `newProductViewModel.uiState.name` remains `""` → fields are never populated
 *
 * **Test strategy:**
 *   1. Seed an in-memory Room database with a menu, category, and a test product
 *      ("Tacos al Pastor").
 *   2. Construct `ConfigurationViewModel` and `NewProductViewModel` backed by that DB.
 *   3. Render `ConfigurationScreen` with both ViewModels.
 *   4. Wait for the product list to appear, then open the product's settings DropdownMenu.
 *   5. Tap the "Editar" DropdownMenuItem.
 *   6. Assert that:
 *      a. `newProductViewModel.uiState.name == "Tacos al Pastor"` — pre-populated ViewModel
 *         state (FAILS on unfixed code: name stays "")
 *      b. The modal header "Nuevo Producto" is visible — modal is rendered
 *         (FAILS on unfixed code: showModal stays false, modal never appears)
 *
 * **Documented counterexample (unfixed code):**
 * "Editar on 'Tacos al Pastor' → showModal=false, uiState.name=''"
 *
 * **Validates: Requirements 1.3, 1.4**
 */
@RunWith(AndroidJUnit4::class)
class EditarBugConditionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var configViewModel: ConfigurationViewModel
    private lateinit var newProductViewModel: NewProductViewModel

    // IDs for seeded data — stable across the test run
    private val menuId     = "menu-test"
    private val categoryId = "cat-test"
    private val productId  = "product-tacos"

    /** The test product injected into the ViewModel / DB. */
    private val testProductName = "Tacos al Pastor"

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

        // Seed: menu → category → product (respecting FK order)
        runBlocking {
            db.menuItemDao().insert(
                MenuItemEntity(id = menuId, emoji = "🌮", name = "Menú Test")
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id               = categoryId,
                    name             = "Categoría Test",
                    associatedMenuId = menuId
                )
            )
            db.productDao().insert(
                ProductEntity(
                    id          = productId,
                    emoji       = "🌮",
                    name        = testProductName,
                    description = "Deliciosos tacos",
                    basePrice   = 45.0,
                    isActive    = true,
                    categoryId  = categoryId
                )
            )
        }

        // ConfigurationViewModel scoped to menuId so it loads products under menuId
        configViewModel = ConfigurationViewModel(
            categoryRepository = CategoryRepository(db.categoryDao()),
            productRepository  = ProductRepository(
                productDao = db.productDao(),
                groupDao   = db.customizationGroupDao(),
                optionDao  = db.customizationOptionDao()
            ),
            menuId = menuId
        )

        newProductViewModel = NewProductViewModel(
            productRepository  = ProductRepository(
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
     * Taps "Editar" on the product card for "Tacos al Pastor" and asserts that:
     * 1. `newProductViewModel.uiState.name` equals `testProductName`.
     * 2. The modal is rendered (header text "Nuevo Producto" is displayed).
     *
     * **On UNFIXED code:** BOTH assertions FAIL:
     *   - `uiState.name` stays `""` (loadForEdit is never called)
     *   - The modal header is never displayed (showModal stays false)
     * → Confirms the bug exists.
     *
     * **On FIXED code:** BOTH assertions PASS.
     *
     * Counterexample: "Editar on 'Tacos al Pastor' → showModal=false, uiState.name=''"
     *
     * **Validates: Requirements 1.3, 1.4**
     */
    @Test
    fun property1_bugCondition_editarDoesNotOpenPrePopulatedModal() {
        composeTestRule.setContent {
            ConfigurationScreen(
                viewModel            = configViewModel,
                newProductViewModel  = newProductViewModel
            )
        }

        // Wait for the product list to load and the product card to appear.
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(testProductName)
            .assertIsDisplayed()

        // Open the settings DropdownMenu on the product card.
        // The settings IconButton has contentDescription "Opciones de <product.name>".
        composeTestRule
            .onNodeWithContentDescription("Opciones de $testProductName")
            .performClick()
        composeTestRule.waitForIdle()

        // Tap "Editar" in the DropdownMenu.
        composeTestRule
            .onNodeWithText("Editar")
            .performClick()
        composeTestRule.waitForIdle()

        // Wait for loadForEdit coroutine to complete and modal to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            newProductViewModel.uiState.value.name == testProductName
        }
        composeTestRule.waitForIdle()

        // ── Assertion 1: ViewModel uiState.name must be populated ────────────
        // On UNFIXED code: name stays "" because loadForEdit is never called.
        // FAILS with: expected:<Tacos al Pastor> but was:<>
        val actualName = newProductViewModel.uiState.value.name
        assertEquals(
            "BUG CONFIRMED — uiState.name was not pre-populated after tapping 'Editar'. " +
            "Counterexample: Editar on '$testProductName' → uiState.name='$actualName' (expected '$testProductName'). " +
            "Root cause: onEditar lambda only logs and dismisses menu; loadForEdit() is never called.",
            testProductName,
            actualName
        )

        // ── Assertion 2: ViewModel isEditMode must be true ───────────────────
        // On UNFIXED code: loadForEdit is never called so isEditMode stays false.
        // On FIXED code: loadForEdit sets isEditMode = true.
        val actualIsEditMode = newProductViewModel.uiState.value.isEditMode
        assertTrue(
            "BUG CONFIRMED — uiState.isEditMode was not set to true after tapping 'Editar'. " +
            "Counterexample: Editar on '$testProductName' → isEditMode=$actualIsEditMode (expected true). " +
            "Root cause: loadForEdit() is never called.",
            actualIsEditMode
        )

        // ── Assertion 3: Modal must be visible ───────────────────────────────
        // Wait for the ModalBottomSheet animation to complete before asserting UI.
        // ConfigurationScreen renders NewProductModal only when showModal=true.
        // On UNFIXED code: showModal stays false, so the modal never renders.
        // On FIXED code: isEditMode=true → modal header shows "Editar Producto".
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule
                    .onNodeWithText("Editar Producto")
                    .assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }
        composeTestRule
            .onNodeWithText("Editar Producto")
            .assertIsDisplayed()
    }
}
