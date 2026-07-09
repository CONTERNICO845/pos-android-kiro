package com.example.puntodeventa.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [ProductDao].
 *
 * Covers:
 * - Property 3: Count invariant — `getProductsByCategory`
 * - Property 6: DAO insert/retrieve field preservation
 * - Property 7 (Product): Idempotent upsert
 * - Property 11: Active product filter
 *
 * **Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7, 9.3, 9.6**
 *
 * FK hierarchy: MenuItemEntity → CategoryEntity → ProductEntity
 * Foreign keys are enabled via a RoomDatabase.Callback (Room 2.7.1 compatible).
 */
@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var menuItemDao: MenuItemDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var productDao: ProductDao

    /** Enables SQLite FK enforcement on every connection (Room 2.7.1 compatible approach). */
    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val testMenuItem = MenuItemEntity(
        id = "menu-1",
        emoji = "🍔",
        name = "Burgers"
    )

    private val testCategory = CategoryEntity(
        id = "cat-1",
        name = "Classic Burgers",
        associatedMenuId = "menu-1"
    )

    private fun makeProduct(
        id: String,
        name: String = "Product $id",
        categoryId: String = "cat-1",
        isActive: Boolean = true,
        emoji: String = "🍔",
        description: String = "A description",
        basePrice: Double = 9.99
    ) = ProductEntity(
        id = id,
        emoji = emoji,
        name = name,
        description = description,
        basePrice = basePrice,
        isActive = isActive,
        categoryId = categoryId
    )

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(foreignKeyCallback)
            .allowMainThreadQueries()
            .build()
        menuItemDao = db.menuItemDao()
        categoryDao = db.categoryDao()
        productDao = db.productDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Insert the required FK ancestors so ProductEntity rows can be inserted. */
    private suspend fun insertAncestors() {
        menuItemDao.insert(testMenuItem)
        categoryDao.insert(testCategory)
    }

    // ── Property 3: Count invariant — getProductsByCategory ──────────────────
    // **Validates: Requirements 2.5, 2.6, 9.3**

    @Test
    fun property3_countInvariant_emptyReturnsEmptyList() = runBlocking {
        insertAncestors()
        val result = productDao.getProductsByCategory("cat-1").first()
        assertTrue("Expected empty list when no products inserted", result.isEmpty())
    }

    @Test
    fun property3_countInvariant_singleProduct() = runBlocking {
        insertAncestors()
        productDao.insert(makeProduct("p-1"))
        val result = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Expected 1 product", 1, result.size)
    }

    @Test
    fun property3_countInvariant_multipleProducts() = runBlocking {
        insertAncestors()
        val n = 5
        repeat(n) { i -> productDao.insert(makeProduct("p-$i")) }
        val result = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Expected $n products", n, result.size)
    }

    @Test
    fun property3_countInvariant_doesNotIncludeOtherCategories() = runBlocking {
        // Insert a second category under same menu
        insertAncestors()
        categoryDao.insert(CategoryEntity(id = "cat-2", name = "Specials", associatedMenuId = "menu-1"))

        productDao.insert(makeProduct("p-1", categoryId = "cat-1"))
        productDao.insert(makeProduct("p-2", categoryId = "cat-2"))
        productDao.insert(makeProduct("p-3", categoryId = "cat-2"))

        val cat1Result = productDao.getProductsByCategory("cat-1").first()
        val cat2Result = productDao.getProductsByCategory("cat-2").first()

        assertEquals("cat-1 should have 1 product", 1, cat1Result.size)
        assertEquals("cat-2 should have 2 products", 2, cat2Result.size)
    }

    // ── Property 6: DAO insert/retrieve field preservation ───────────────────
    // **Validates: Requirements 2.3, 2.6, 9.6**

    @Test
    fun property6_fieldPreservation_allSevenFieldsEqual() = runBlocking {
        insertAncestors()
        val original = ProductEntity(
            id = "p-field-test",
            emoji = "🌮",
            name = "Taco Supreme",
            description = "A really good taco with all the fixings",
            basePrice = 12.50,
            isActive = true,
            categoryId = "cat-1"
        )
        productDao.insert(original)

        val retrieved = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Should retrieve exactly 1 product", 1, retrieved.size)
        val row = retrieved.first()

        assertEquals("id must match", original.id, row.id)
        assertEquals("emoji must match", original.emoji, row.emoji)
        assertEquals("name must match", original.name, row.name)
        assertEquals("description must match", original.description, row.description)
        assertEquals("basePrice must match", original.basePrice, row.basePrice, 0.0)
        assertEquals("isActive must match", original.isActive, row.isActive)
        assertEquals("categoryId must match", original.categoryId, row.categoryId)
    }

    @Test
    fun property6_fieldPreservation_inactiveProduct() = runBlocking {
        insertAncestors()
        val original = ProductEntity(
            id = "p-inactive",
            emoji = "🥤",
            name = "Soda",
            description = "Carbonated beverage",
            basePrice = 2.00,
            isActive = false,
            categoryId = "cat-1"
        )
        productDao.insert(original)

        val retrieved = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Should retrieve 1 product regardless of isActive", 1, retrieved.size)
        assertEquals("isActive=false must be preserved", false, retrieved.first().isActive)
    }

    @Test
    fun property6_fieldPreservation_zeroPriceProduct() = runBlocking {
        insertAncestors()
        val original = makeProduct("p-free", basePrice = 0.0)
        productDao.insert(original)

        val retrieved = productDao.getProductsByCategory("cat-1").first().first()
        assertEquals("basePrice=0.0 must be preserved", 0.0, retrieved.basePrice, 0.0)
    }

    // ── Property 7 (Product): Idempotent upsert ───────────────────────────────
    // **Validates: Requirements 2.3, 9.6**

    @Test
    fun property7_idempotentUpsert_secondInsertWins() = runBlocking {
        insertAncestors()
        val original = makeProduct("p-upsert", name = "Original Name")
        val updated = original.copy(name = "Updated Name")

        productDao.insert(original)
        productDao.insert(updated)

        val retrieved = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Should have exactly 1 row after upsert", 1, retrieved.size)
        assertEquals("Second insert name should win", "Updated Name", retrieved.first().name)
    }

    @Test
    fun property7_idempotentUpsert_updatedPriceWins() = runBlocking {
        insertAncestors()
        val v1 = makeProduct("p-price", basePrice = 5.00)
        val v2 = v1.copy(basePrice = 10.00)

        productDao.insert(v1)
        productDao.insert(v2)

        val retrieved = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Single row after upsert", 1, retrieved.size)
        assertEquals("Updated price must win", 10.00, retrieved.first().basePrice, 0.0)
    }

    @Test
    fun property7_idempotentUpsert_sameSameNoExtra() = runBlocking {
        insertAncestors()
        val product = makeProduct("p-same")

        productDao.insert(product)
        productDao.insert(product) // exact same entity twice

        val retrieved = productDao.getProductsByCategory("cat-1").first()
        assertEquals("Inserting same entity twice should still be 1 row", 1, retrieved.size)
    }

    // ── Property 11: Active product filter ───────────────────────────────────
    // **Validates: Requirements 2.7**

    @Test
    fun property11_activeFilter_returnsOnlyActiveRows() = runBlocking {
        insertAncestors()
        val active1 = makeProduct("p-active-1", isActive = true)
        val active2 = makeProduct("p-active-2", isActive = true)
        val inactive1 = makeProduct("p-inactive-1", isActive = false)
        val inactive2 = makeProduct("p-inactive-2", isActive = false)

        productDao.insert(active1)
        productDao.insert(active2)
        productDao.insert(inactive1)
        productDao.insert(inactive2)

        val result = productDao.getActiveProductsByCategory("cat-1").first()
        assertEquals("Should return only 2 active products", 2, result.size)
        assertTrue("All returned rows must be active", result.all { it.isActive })
    }

    @Test
    fun property11_activeFilter_emptyWhenAllInactive() = runBlocking {
        insertAncestors()
        productDao.insert(makeProduct("p-off-1", isActive = false))
        productDao.insert(makeProduct("p-off-2", isActive = false))

        val result = productDao.getActiveProductsByCategory("cat-1").first()
        assertTrue("Should return empty list when all products are inactive", result.isEmpty())
    }

    @Test
    fun property11_activeFilter_emptyWhenNoProductsExist() = runBlocking {
        insertAncestors()
        val result = productDao.getActiveProductsByCategory("cat-1").first()
        assertTrue("Should return empty list when no products exist", result.isEmpty())
    }

    @Test
    fun property11_activeFilter_activeCountMatchesInserted() = runBlocking {
        insertAncestors()
        val activeCount = 7
        val inactiveCount = 3
        repeat(activeCount) { i -> productDao.insert(makeProduct("p-a-$i", isActive = true)) }
        repeat(inactiveCount) { i -> productDao.insert(makeProduct("p-i-$i", isActive = false)) }

        val result = productDao.getActiveProductsByCategory("cat-1").first()
        assertEquals("Should return exactly $activeCount active products", activeCount, result.size)
    }
}
