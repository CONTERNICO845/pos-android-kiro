package com.example.puntodeventa.ui.configuration

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation property tests for Bug 3 — Duplication Shared IDs (Incomplete Deep Copy).
 *
 * **Property 2: Preservation** — Products With No Groups and Active-Toggle Independence
 *
 * These tests verify behaviors that are NOT the bug condition (`isBugCondition_3`):
 * i.e., they cover cases where the current (unfixed) code already produces the correct
 * result. All three tests must PASS on unfixed code (and continue to pass after the fix).
 *
 * Bug condition reminder: `isBugCondition_3(P)` is TRUE when P has ≥1 groups AND the
 * duplicate has fewer groups. These tests all operate on the ¬C (non-bug) cases:
 * - Products with 0 groups (duplication is already correct)
 * - Toggle isActive independence between original and duplicate
 * - Delete original does not cascade to duplicate
 *
 * Covers Requirements 3.8–3.10:
 * - 3.8: Duplicating a product with 0 groups creates a copy with only a new UUID and 0 groups.
 * - 3.9: Toggling isActive on the original does NOT affect the duplicate's isActive.
 * - 3.10: Deleting the original does NOT cascade-delete the duplicate.
 *
 * **Validates: Requirements 3.8, 3.9, 3.10**
 */
@RunWith(AndroidJUnit4::class)
class DuplicarPreservationTest {

    private lateinit var db: AppDatabase
    private lateinit var configViewModel: ConfigurationViewModel
    private lateinit var productRepository: ProductRepository

    // Stable IDs for seeded data
    private val menuId     = "menu-preservation-dup"
    private val categoryId = "cat-preservation-dup"
    private val productId  = "product-agua-fresca"

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

        // Seed: menu → category → product (no customization groups — that's the preservation case)
        runBlocking {
            db.menuItemDao().insert(
                MenuItemEntity(id = menuId, emoji = "🥤", name = "Menú Preservation")
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id               = categoryId,
                    name             = "Bebidas",
                    associatedMenuId = menuId
                )
            )
            db.productDao().insert(
                ProductEntity(
                    id          = productId,
                    emoji       = "🥤",
                    name        = "Agua Fresca",
                    description = "Bebida de temporada",
                    basePrice   = 25.0,
                    isActive    = true,
                    categoryId  = categoryId
                )
            )
            // Intentionally NO customization groups seeded — isBugCondition_3 = false
        }

        productRepository = ProductRepository(
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
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 3.8 — Duplicating product with 0 groups produces copy with new UUID and 0 groups ─

    /**
     * For all products P where `isBugCondition_3(P)` is false (0 groups), duplicating P
     * must produce a copy P' that has:
     * - A different product ID (`P'.id ≠ P.id`)
     * - 0 customization groups (same as the original — no data was lost or invented)
     *
     * This is NOT a bug-condition case (the original has 0 groups, so the unfixed code
     * trivially produces a correct result). This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.8**
     */
    @Test
    fun duplicating_product_with_no_groups_creates_copy_with_new_uuid_and_zero_groups() =
        runBlocking {
            val originalProduct = Product(
                id          = productId,
                emoji       = "🥤",
                name        = "Agua Fresca",
                description = "Bebida de temporada",
                basePrice   = 25.0,
                isActive    = true,
                categoryId  = categoryId
            )

            // Confirm the original has 0 groups (¬isBugCondition_3 precondition)
            val originalGroupsBefore = db.customizationGroupDao()
                .getGroupsByProductOnce(productId)
            assertEquals(
                "Precondition: original product must have 0 groups for this preservation test",
                0,
                originalGroupsBefore.size
            )

            // Perform duplication via ConfigurationViewModel
            configViewModel.duplicateProduct(originalProduct)

            // Wait for the duplication coroutine to complete
            Thread.sleep(500)

            // Find the duplicate (different ID, same name)
            val allProducts = db.productDao()
                .getProductsByCategory(categoryId)
                .first()

            val duplicateProduct = allProducts.firstOrNull { it.id != productId }
                ?: throw AssertionError(
                    "No duplicate product found. Expected a second product with a different ID."
                )

            // ── Assertion 1: Duplicate has a NEW UUID (different from original) ─────
            assertNotEquals(
                "Req 3.8: Duplicate product must have a new UUID, different from the original",
                productId,
                duplicateProduct.id
            )

            // ── Assertion 2: Duplicate has 0 groups (same as original) ───────────────
            val duplicateGroups = db.customizationGroupDao()
                .getGroupsByProductOnce(duplicateProduct.id)
            assertEquals(
                "Req 3.8: Duplicate of a 0-group product must also have 0 groups. " +
                "Got ${duplicateGroups.size} groups for duplicate id=${duplicateProduct.id}",
                0,
                duplicateGroups.size
            )

            // ── Assertion 3: Original still has 0 groups (untouched) ─────────────────
            val originalGroupsAfter = db.customizationGroupDao()
                .getGroupsByProductOnce(productId)
            assertEquals(
                "Req 3.8: Original product must still have 0 groups after duplication",
                0,
                originalGroupsAfter.size
            )
        }

    // ── Property 3.9 — Toggling isActive on original does NOT affect duplicate ─

    /**
     * For all products P, calling `toggleProductActive(P)` after duplication must
     * change only the original product's `isActive` flag; the duplicate P' must remain
     * unaffected.
     *
     * On unfixed code: the original and duplicate are independent `ProductEntity` rows
     * with different primary keys. A `toggleProductActive` (which calls
     * `productRepository.insert(product.copy(isActive = !product.isActive))`) only
     * upserts the row for `product.id` — the duplicate's row is untouched.
     * This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.9**
     */
    @Test
    fun toggling_isActive_on_original_does_not_affect_duplicate_isActive() = runBlocking {
        val originalProduct = Product(
            id          = productId,
            emoji       = "🥤",
            name        = "Agua Fresca",
            description = "Bebida de temporada",
            basePrice   = 25.0,
            isActive    = true,
            categoryId  = categoryId
        )

        // Step 1: Duplicate the product
        configViewModel.duplicateProduct(originalProduct)
        Thread.sleep(500)

        // Step 2: Locate the duplicate
        val allProductsAfterDup = db.productDao()
            .getProductsByCategory(categoryId)
            .first()

        val duplicateEntity = allProductsAfterDup.firstOrNull { it.id != productId }
            ?: throw AssertionError(
                "No duplicate product found after duplicateProduct(). " +
                "Expected a product with a different ID."
            )

        // Record the duplicate's isActive before toggle
        val duplicateIsActiveBefore = duplicateEntity.isActive

        // Step 3: Toggle isActive on the ORIGINAL product
        configViewModel.toggleProductActive(originalProduct)
        Thread.sleep(500)

        // Step 4: Re-read both products from DB
        val allProductsAfterToggle = db.productDao()
            .getProductsByCategory(categoryId)
            .first()

        val originalAfterToggle = allProductsAfterToggle.firstOrNull { it.id == productId }
            ?: throw AssertionError("Original product not found after toggle")

        val duplicateAfterToggle = allProductsAfterToggle.firstOrNull { it.id == duplicateEntity.id }
            ?: throw AssertionError("Duplicate product not found after toggle")

        // ── Assertion 1: Original's isActive was flipped ─────────────────────────
        assertNotEquals(
            "Req 3.9: toggleProductActive must flip the original's isActive flag. " +
            "Original before=${originalProduct.isActive}, after=${originalAfterToggle.isActive}",
            originalProduct.isActive,
            originalAfterToggle.isActive
        )

        // ── Assertion 2: Duplicate's isActive is unchanged ───────────────────────
        assertEquals(
            "Req 3.9: Toggling isActive on the original must NOT affect the duplicate's isActive. " +
            "Duplicate isActive before=${duplicateIsActiveBefore}, after=${duplicateAfterToggle.isActive}",
            duplicateIsActiveBefore,
            duplicateAfterToggle.isActive
        )
    }

    // ── Property 3.10 — Deleting original does NOT remove duplicate from DB ──

    /**
     * For all products P, deleting P after duplication must NOT cascade-delete the
     * duplicate P'. P' (and all its children) must remain in the database.
     *
     * On unfixed code: the original and duplicate are independent `ProductEntity` rows.
     * `deleteProduct(originalId)` calls `productRepository.deleteById(originalId)`, which
     * executes `DELETE FROM products WHERE id = :id`. Because the duplicate has a different
     * primary key, it is not touched. Room's FK cascade only removes children of the deleted
     * product (the original's groups/options), not rows belonging to a different product.
     * This test must PASS on unfixed code.
     *
     * **Validates: Requirement 3.10**
     */
    @Test
    fun deleting_original_does_not_remove_duplicate_from_database() = runBlocking {
        val originalProduct = Product(
            id          = productId,
            emoji       = "🥤",
            name        = "Agua Fresca",
            description = "Bebida de temporada",
            basePrice   = 25.0,
            isActive    = true,
            categoryId  = categoryId
        )

        // Step 1: Duplicate the product
        configViewModel.duplicateProduct(originalProduct)
        Thread.sleep(500)

        // Step 2: Locate the duplicate and record its ID
        val allProductsAfterDup = db.productDao()
            .getProductsByCategory(categoryId)
            .first()

        assertEquals(
            "After duplication there should be exactly 2 products in the category",
            2,
            allProductsAfterDup.size
        )

        val duplicateEntity = allProductsAfterDup.firstOrNull { it.id != productId }
            ?: throw AssertionError(
                "No duplicate product found after duplicateProduct(). " +
                "Expected a product with a different ID."
            )
        val duplicateId = duplicateEntity.id

        // Step 3: Delete the ORIGINAL product
        configViewModel.deleteProduct(productId)
        Thread.sleep(500)

        // Step 4: Verify original is gone
        val allProductsAfterDelete = db.productDao()
            .getProductsByCategory(categoryId)
            .first()

        val originalAfterDelete = allProductsAfterDelete.firstOrNull { it.id == productId }
        assertEquals(
            "Req 3.10: The original product must be deleted from the database",
            null,
            originalAfterDelete
        )

        // ── Assertion: Duplicate still exists in the DB ───────────────────────────
        val duplicateAfterDelete = allProductsAfterDelete.firstOrNull { it.id == duplicateId }
        assertNotNull(
            "Req 3.10: Deleting the original must NOT remove the duplicate from the database. " +
            "Duplicate id=$duplicateId was not found after deleting original id=$productId.",
            duplicateAfterDelete
        )

        // ── Extra: Duplicate groups/options also survive (0 in this case, but verifiable) ─
        val duplicateGroupsAfterDelete = db.customizationGroupDao()
            .getGroupsByProductOnce(duplicateId)
        assertEquals(
            "Req 3.10: Duplicate product should still have 0 groups after original deletion " +
            "(no cascade across independent product rows)",
            0,
            duplicateGroupsAfterDelete.size
        )
    }
}
