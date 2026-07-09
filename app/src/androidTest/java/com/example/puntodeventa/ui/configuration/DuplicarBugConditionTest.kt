package com.example.puntodeventa.ui.configuration

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug condition exploration test for Bug 3 — Duplication Shared IDs
 * (Incomplete Deep Copy).
 *
 * **Property 1: Bug Condition** — Duplicate Has Fewer Groups Than Original
 *
 * This test is EXPECTED TO FAIL on unfixed code. Failure confirms the bug exists.
 *
 * **Root cause:** The current `ConfigurationViewModel.duplicateProduct()` only calls:
 * ```kotlin
 * productRepository.insert(product.copy(id = UUID.randomUUID().toString()))
 * ```
 * This creates a new `ProductEntity` row with a fresh UUID but does NOT copy the
 * associated `CustomizationGroupEntity` and `CustomizationOptionEntity` rows.
 * As a result, the duplicated product ends up with 0 groups/options, while the
 * original product retains all its customization data.
 *
 * **Test strategy:**
 *   1. Seed an in-memory Room database with:
 *      - Menu → Category → Product ("Tacos al Pastor")
 *      - 2 CustomizationGroupEntity rows (e.g., "Salsa", "Tamaño")
 *      - 3 CustomizationOptionEntity rows for each group (6 options total)
 *   2. Construct `ConfigurationViewModel` backed by that DB.
 *   3. Construct a `Product` domain object from the seeded `ProductEntity`.
 *   4. Call `configViewModel.duplicateProduct(product)`.
 *   5. Wait for the duplication to complete.
 *   6. Query the database for:
 *      - The original product's groups: `groupDao.getGroupsByProductOnce(originalId)`
 *      - The duplicate product's groups: `groupDao.getGroupsByProductOnce(duplicateId)`
 *   7. Assert that:
 *      - Original product still has 2 groups (unchanged)
 *      - **Duplicate product has 2 groups** (FAILS on unfixed code: duplicate has 0 groups)
 *
 * **Documented counterexample (unfixed code):**
 * "Tacos al Pastor (2 groups) duplicated → duplicate.groups.size = 0"
 *
 * **Validates: Requirements 1.5, 1.6**
 */
@RunWith(AndroidJUnit4::class)
class DuplicarBugConditionTest {

    private lateinit var db: AppDatabase
    private lateinit var configViewModel: ConfigurationViewModel
    private lateinit var productRepository: ProductRepository

    // IDs for seeded data — stable across the test run
    private val menuId     = "menu-test-duplicate"
    private val categoryId = "cat-test-duplicate"
    private val productId  = "product-tacos-original"

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
                    description = "Deliciosos tacos al pastor",
                    basePrice   = 45.0,
                    isActive    = true,
                    categoryId  = categoryId
                )
            )

            // Seed 2 customization groups for the product
            val group1Id = "group-salsa"
            val group2Id = "group-tamano"

            db.customizationGroupDao().insertInternal(
                CustomizationGroupEntity(
                    id            = group1Id,
                    productId     = productId,
                    groupName     = "Salsa",
                    selectionType = "single_option"
                )
            )
            db.customizationGroupDao().insertInternal(
                CustomizationGroupEntity(
                    id            = group2Id,
                    productId     = productId,
                    groupName     = "Tamaño",
                    selectionType = "single_option"
                )
            )

            // Seed 3 options for each group (6 options total)
            // Group 1 — Salsa
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-salsa-verde",
                    groupId    = group1Id,
                    optionName = "Salsa Verde",
                    extraPrice = 0.0
                )
            )
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-salsa-roja",
                    groupId    = group1Id,
                    optionName = "Salsa Roja",
                    extraPrice = 0.0
                )
            )
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-salsa-habanero",
                    groupId    = group1Id,
                    optionName = "Salsa Habanero",
                    extraPrice = 5.0
                )
            )

            // Group 2 — Tamaño
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-tamano-chico",
                    groupId    = group2Id,
                    optionName = "Chico",
                    extraPrice = 0.0
                )
            )
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-tamano-mediano",
                    groupId    = group2Id,
                    optionName = "Mediano",
                    extraPrice = 10.0
                )
            )
            db.customizationOptionDao().insert(
                CustomizationOptionEntity(
                    id         = "option-tamano-grande",
                    groupId    = group2Id,
                    optionName = "Grande",
                    extraPrice = 20.0
                )
            )
        }

        // Construct repositories and ViewModel
        productRepository = ProductRepository(
            productDao = db.productDao(),
            groupDao   = db.customizationGroupDao(),
            optionDao  = db.customizationOptionDao(),
            database   = db
        )

        configViewModel = ConfigurationViewModel(
            categoryRepository = CategoryRepository(db.categoryDao()),
            productRepository  = productRepository,
            menuId = menuId
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 1: Bug Condition ─────────────────────────────────────────────

    /**
     * Duplicates "Tacos al Pastor" (which has 2 groups, 6 options) and asserts that:
     * 1. The original product still has 2 groups (unchanged).
     * 2. The duplicate product has 2 groups (FAILS on unfixed code: duplicate has 0).
     *
     * **On UNFIXED code:** Assertion 2 FAILS:
     *   - `duplicateGroups.size` is 0 because `duplicateProduct()` only copies the
     *     `ProductEntity` and never copies the groups/options.
     * → Confirms the bug exists.
     *
     * **On FIXED code:** Both assertions PASS (duplicate has 2 groups with new UUIDs).
     *
     * Counterexample: "Tacos al Pastor (2 groups) duplicated → duplicate.groups.size = 0"
     *
     * **Validates: Requirements 1.5, 1.6**
     */
    @Test
    fun property1_bugCondition_duplicateHasFewerGroupsThanOriginal() = runBlocking {
        // Construct the Product domain object from the seeded entity
        val originalProduct = Product(
            id          = productId,
            emoji       = "🌮",
            name        = testProductName,
            description = "Deliciosos tacos al pastor",
            basePrice   = 45.0,
            isActive    = true,
            categoryId  = categoryId
        )

        // Verify the original product has 2 groups before duplication
        val originalGroupsBefore = db.customizationGroupDao()
            .getGroupsByProductOnce(productId)
        assertEquals(
            "Original product should have 2 groups before duplication",
            2,
            originalGroupsBefore.size
        )

        // Verify the original product has 6 options total (3 per group)
        val originalOptionsBefore = originalGroupsBefore.flatMap { group ->
            db.customizationOptionDao().getOptionsByGroupOnce(group.id)
        }
        assertEquals(
            "Original product should have 6 options total before duplication",
            6,
            originalOptionsBefore.size
        )

        // Call the duplicateProduct method on ConfigurationViewModel
        configViewModel.duplicateProduct(originalProduct)

        // Wait for the duplication coroutine to complete
        // The duplicate product should now exist in the DB with a new UUID
        Thread.sleep(500)  // Short delay to ensure coroutine completes

        // Query all products in the category to find the duplicate
        val allProducts = db.productDao()
            .getProductsByCategory(categoryId)
            .first()

        // The duplicate should be the product with a different ID but same name
        val duplicateProduct = allProducts.firstOrNull { it.id != productId }
            ?: throw AssertionError(
                "DUPLICATION FAILED — No duplicate product found in the database. " +
                "Expected to find a second product with the same name but different ID."
            )

        // ── Assertion 1: Original product still has 2 groups (unchanged) ──────
        val originalGroupsAfter = db.customizationGroupDao()
            .getGroupsByProductOnce(productId)
        assertEquals(
            "Original product should still have 2 groups after duplication",
            2,
            originalGroupsAfter.size
        )

        // ── Assertion 2: Duplicate product has 2 groups ───────────────────────
        // On UNFIXED code: This assertion FAILS because duplicateProduct() only
        // copies the ProductEntity, not the groups/options.
        // Expected: 2, Actual: 0
        val duplicateGroups = db.customizationGroupDao()
            .getGroupsByProductOnce(duplicateProduct.id)

        assertEquals(
            "BUG CONFIRMED — Duplicate product has fewer groups than the original. " +
            "Counterexample: $testProductName (${originalGroupsAfter.size} groups) duplicated → " +
            "duplicate.groups.size = ${duplicateGroups.size} (expected ${originalGroupsAfter.size}). " +
            "Root cause: duplicateProduct() only calls productRepository.insert(product.copy(id = newUUID)), " +
            "which copies the ProductEntity but does NOT copy CustomizationGroupEntity or " +
            "CustomizationOptionEntity rows. A deep copy transaction is needed.",
            originalGroupsAfter.size,  // Expected: 2
            duplicateGroups.size       // Actual: 0 on unfixed code
        )

        // ── Additional verification: Check option counts ──────────────────────
        // If the groups were copied, we expect 6 options total (3 per group)
        if (duplicateGroups.isNotEmpty()) {
            val duplicateOptions = duplicateGroups.flatMap { group ->
                db.customizationOptionDao().getOptionsByGroupOnce(group.id)
            }
            assertEquals(
                "Duplicate product should have 6 options total (3 per group) if groups were copied",
                6,
                duplicateOptions.size
            )
        }
    }
}
