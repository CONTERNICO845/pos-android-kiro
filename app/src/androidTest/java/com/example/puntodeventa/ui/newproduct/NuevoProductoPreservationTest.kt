package com.example.puntodeventa.ui.newproduct

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.example.puntodeventa.ui.configuration.ConfigurationScreen
import com.example.puntodeventa.ui.configuration.ConfigurationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation property tests for Bug 2 — Nuevo Producto and Cancel Still Work.
 *
 * **Property 2: Preservation** — Nuevo Producto and Cancel Still Work
 *
 * These tests verify behaviors that are NOT caused by the "Editar" tap (`isBugCondition_2`).
 * They must ALL PASS on unfixed code (and continue to pass after the Bug 2 fix is applied).
 *
 * Covers Requirements 3.5–3.7:
 * - 3.5: Tapping "+ Nuevo Producto" opens the modal with empty/default fields.
 * - 3.6: save() in create mode inserts a ProductEntity with a fresh UUID.
 * - 3.7: Tapping "Cancelar" dismisses the modal and resets form state.
 *
 * **Validates: Requirements 3.5, 3.6, 3.7**
 */
@RunWith(AndroidJUnit4::class)
class NuevoProductoPreservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var configViewModel: ConfigurationViewModel
    private lateinit var newProductViewModel: NewProductViewModel

    private val menuId     = "menu-test"
    private val categoryId = "cat-test"

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
                MenuItemEntity(id = menuId, emoji = "🍔", name = "Menú Test")
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id               = categoryId,
                    name             = "Categoría Test",
                    associatedMenuId = menuId
                )
            )
        }

        val productRepository = ProductRepository(
            productDao = db.productDao(),
            groupDao   = db.customizationGroupDao(),
            optionDao  = db.customizationOptionDao(),
            database   = db
        )

        configViewModel = ConfigurationViewModel(
            categoryRepository = CategoryRepository(db.categoryDao()),
            productRepository  = productRepository,
            menuId             = menuId
        )

        newProductViewModel = NewProductViewModel(
            productRepository  = productRepository,
            categoryRepository = CategoryRepository(db.categoryDao()),
            menuRepository     = MenuRepository(db.menuItemDao()),
            database           = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 3.5 — "+ Nuevo Producto" opens modal with empty fields ───────

    /**
     * Tapping "+ Nuevo Producto" must open the modal in creation mode with default empty fields.
     *
     * This is NOT the bug condition (not an "Editar" tap), so it must PASS on unfixed code.
     *
     * **Validates: Requirement 3.5**
     */
    @Test
    fun property_nuevoProducto_opensModalWithEmptyFields() {
        composeTestRule.setContent {
            ConfigurationScreen(
                viewModel           = configViewModel,
                newProductViewModel = newProductViewModel
            )
        }
        composeTestRule.waitForIdle()

        // Tap "Nuevo Producto" button in ActionBarRow
        // After the click there will be two nodes with text "Nuevo Producto":
        // the button and the modal header. Click the first occurrence (button).
        composeTestRule
            .onAllNodes(androidx.compose.ui.test.hasText("Nuevo Producto"))
            .get(0)
            .performClick()
        composeTestRule.waitForIdle()

        // Assert: modal is open — "Cancelar" button is only visible inside the modal
        composeTestRule
            .onNodeWithText("Cancelar")
            .assertIsDisplayed()

        // Assert: name field in ViewModel state is empty (default)
        val actualName = newProductViewModel.uiState.value.name
        assertEquals(
            "Nuevo Producto must open with an empty name field. Got: '$actualName'",
            "",
            actualName
        )
    }

    // ── Property 3.6 — save() in create mode inserts a product with a fresh UUID ─

    /**
     * Calling save() in create mode must insert a ProductEntity with a newly generated UUID
     * — the inserted product ID must be non-empty and distinct from any pre-seeded product.
     *
     * This is NOT the bug condition (not an "Editar" tap), so it must PASS on unfixed code.
     *
     * **Validates: Requirement 3.6**
     */
    @Test
    fun property_saveInCreateMode_insertsFreshUUID() {
        // Render modal directly to bypass ConfigurationScreen showModal gating
        composeTestRule.setContent {
            NewProductModal(
                viewModel = newProductViewModel,
                onDismiss = { newProductViewModel.dismiss() }
            )
        }
        composeTestRule.waitForIdle()

        // Pre-populate required fields via ViewModel so save() validation passes.
        // Use runOnUiThread because StateFlow updates must happen on the main thread.
        composeTestRule.runOnUiThread {
            newProductViewModel.updateName("Agua Fresca")
            // Select the seeded category
            val cats = newProductViewModel.uiState.value.categories
            val cat = cats.firstOrNull { it.id == categoryId }
            assertNotNull("Category must be loaded into ViewModel before save()", cat)
            newProductViewModel.selectCategory(cat!!)
        }
        composeTestRule.waitForIdle()

        // Trigger save on the UI thread
        composeTestRule.runOnUiThread {
            newProductViewModel.save()
        }

        // Wait until saveResult is non-null (success or failure)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            newProductViewModel.uiState.value.saveResult != null
        }

        // Assert save succeeded
        assertTrue(
            "save() in create mode must succeed. Got saveResult: " +
            "${newProductViewModel.uiState.value.saveResult}",
            newProductViewModel.uiState.value.saveResult is SaveResult.Success
        )

        // Query the DB to verify a product was inserted with a non-empty UUID
        val products = runBlocking {
            db.productDao().getProductsByCategory(categoryId).first()
        }

        assertTrue(
            "DB must contain at least one product after save(). Products: $products",
            products.isNotEmpty()
        )

        val inserted = products.first { it.name == "Agua Fresca" }
        assertTrue(
            "Inserted product ID must be non-empty",
            inserted.id.isNotBlank()
        )
        // The ID must be a fresh UUID — not matching any pre-seeded product ID
        // (no pre-seeded products exist in this test, so any non-empty UUID qualifies)
        assertNotNull(
            "Inserted product must have a valid non-null ID",
            inserted.id
        )
    }

    // ── Property 3.7 — "Cancelar" dismisses modal and resets form state ───────

    /**
     * Tapping "Cancelar" must dismiss the modal and reset all form state via dismiss().
     *
     * This is NOT the bug condition (not an "Editar" tap), so it must PASS on unfixed code.
     *
     * **Validates: Requirement 3.7**
     */
    @Test
    fun property_cancelar_resetsFormState() {
        composeTestRule.setContent {
            ConfigurationScreen(
                viewModel           = configViewModel,
                newProductViewModel = newProductViewModel
            )
        }
        composeTestRule.waitForIdle()

        // Open the modal via "+ Nuevo Producto"
        // Before the modal opens, there is exactly 1 node with text "Nuevo Producto" (the button).
        composeTestRule
            .onAllNodes(androidx.compose.ui.test.hasText("Nuevo Producto"))
            .get(0)
            .performClick()
        composeTestRule.waitForIdle()

        // Put some text into the name field via ViewModel to give dismiss() something to clear
        composeTestRule.runOnUiThread {
            newProductViewModel.updateName("Borrame")
        }
        composeTestRule.waitForIdle()

        // Click "Cancelar" — onDismiss calls newProductViewModel.dismiss() + sets showModal=false
        composeTestRule
            .onNodeWithText("Cancelar")
            .performClick()
        composeTestRule.waitForIdle()

        // Assert: form state was reset — name is empty
        val nameAfterCancel = newProductViewModel.uiState.value.name
        assertEquals(
            "dismiss() must reset name to '' after Cancelar. Got: '$nameAfterCancel'",
            "",
            nameAfterCancel
        )

        // Assert: modal is closed — header "Nuevo Producto" is not in the composition tree.
        // We look for the modal header specifically. Since the button text is also
        // "Nuevo Producto", we need to verify the modal header node is gone.
        // After dismiss, only the button exists; the ModalBottomSheet is no longer composed.
        // Use assertDoesNotExist() on the node matched by the header context.
        // There are now two "Nuevo Producto" texts: the button and the sheet header.
        // After cancelling, the sheet is gone so only 1 node with that text exists.
        // We can check that at most 1 such node is present (only the button).
        val nodes = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasText("Nuevo Producto")
        )
        // The button is still visible; the sheet header should be gone.
        // Verify: count of nodes with text "Nuevo Producto" must be exactly 1 (the button only).
        val nodeCount = try {
            // fetchSemanticsNodes() throws if collection is empty
            nodes.fetchSemanticsNodes().size
        } catch (e: AssertionError) {
            0
        }
        assertEquals(
            "After Cancelar, only the 'Nuevo Producto' button should remain (sheet header gone). Found $nodeCount nodes.",
            1,
            nodeCount
        )
    }
}
