package com.example.puntodeventa.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying SQLite CASCADE DELETE behaviour across the full
 * 5-level entity hierarchy:
 *
 *   MenuItemEntity (menu_items)
 *     └── CategoryEntity (categories)          FK: associatedMenuId → menu_items.id  CASCADE
 *           └── ProductEntity (products)       FK: categoryId → categories.id        CASCADE
 *                 └── CustomizationGroupEntity (customization_groups)
 *                       FK: productId → products.id                                  CASCADE
 *                           └── CustomizationOptionEntity (customization_options)
 *                                 FK: groupId → customization_groups.id              CASCADE
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 6.5
 */
@RunWith(AndroidJUnit4::class)
class CascadeDeletionTest {

    private lateinit var db: AppDatabase

    // Enable foreign-key constraints for in-memory test database (same pattern as
    // CustomizationGroupDaoTest.kt)
    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Shared hierarchy fixtures ─────────────────────────────────────────────

    private val menuItem = MenuItemEntity(id = "menu-1", emoji = "🍔", name = "Main Menu")

    private val category = CategoryEntity(
        id = "cat-1",
        name = "Burgers",
        associatedMenuId = "menu-1"
    )

    private val product = ProductEntity(
        id = "prod-1",
        emoji = "🍔",
        name = "Classic Burger",
        description = "A juicy burger",
        basePrice = 9.99,
        isActive = true,
        categoryId = "cat-1"
    )

    private val group = CustomizationGroupEntity(
        id = "group-1",
        productId = "prod-1",
        groupName = "Size",
        selectionType = "single_option"
    )

    private val option = CustomizationOptionEntity(
        id = "option-1",
        groupId = "group-1",
        optionName = "Large",
        extraPrice = 1.50
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .addCallback(foreignKeyCallback)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Insert the complete 5-level hierarchy into the database. */
    private suspend fun insertFullHierarchy() {
        db.menuItemDao().insert(menuItem)
        db.categoryDao().insert(category)
        db.productDao().insert(product)
        db.customizationGroupDao().insertInternal(group)
        db.customizationOptionDao().insert(option)
    }

    // ── Test: delete MenuItemEntity cascades through all four descendants ─────
    //
    // Validates: Requirement 8.1
    //
    // WHEN a MenuItemEntity is deleted, THE AppDatabase SHALL delete all
    // CategoryEntity rows, which triggers further cascades through products,
    // customization_groups, and customization_options.

    @Test
    fun deleteMenuItem_cascadesAllDescendants() = runBlocking {
        insertFullHierarchy()

        db.menuItemDao().deleteById("menu-1")

        val categories = db.categoryDao().getCategoriesByMenu("menu-1").first()
        val products = db.productDao().getProductsByCategory("cat-1").first()
        val groups = db.customizationGroupDao().getGroupsByProduct("prod-1").first()
        val options = db.customizationOptionDao().getOptionsByGroup("group-1").first()

        assertTrue("categories should be empty after MenuItemEntity deletion", categories.isEmpty())
        assertTrue("products should be empty after MenuItemEntity deletion", products.isEmpty())
        assertTrue("customization_groups should be empty after MenuItemEntity deletion", groups.isEmpty())
        assertTrue("customization_options should be empty after MenuItemEntity deletion", options.isEmpty())
    }

    // ── Test: delete CategoryEntity cascades to products, groups, and options ─
    //
    // Validates: Requirement 8.3
    //
    // WHEN a CategoryEntity is deleted, THE AppDatabase SHALL delete all
    // ProductEntity rows, which triggers further cascades through
    // customization_groups and customization_options.

    @Test
    fun deleteCategory_cascadesProductsGroupsAndOptions() = runBlocking {
        insertFullHierarchy()

        db.categoryDao().deleteById("cat-1")

        val products = db.productDao().getProductsByCategory("cat-1").first()
        val groups = db.customizationGroupDao().getGroupsByProduct("prod-1").first()
        val options = db.customizationOptionDao().getOptionsByGroup("group-1").first()

        assertTrue("products should be empty after CategoryEntity deletion", products.isEmpty())
        assertTrue("customization_groups should be empty after CategoryEntity deletion", groups.isEmpty())
        assertTrue("customization_options should be empty after CategoryEntity deletion", options.isEmpty())
    }

    // ── Test: delete ProductEntity cascades to groups and options ─────────────
    //
    // Validates: Requirement 8.4
    //
    // WHEN a ProductEntity is deleted, THE AppDatabase SHALL delete all
    // CustomizationGroupEntity rows, which triggers cascade deletion of all
    // CustomizationOptionEntity rows.

    @Test
    fun deleteProduct_cascadesGroupsAndOptions() = runBlocking {
        insertFullHierarchy()

        db.productDao().deleteById("prod-1")

        val groups = db.customizationGroupDao().getGroupsByProduct("prod-1").first()
        val options = db.customizationOptionDao().getOptionsByGroup("group-1").first()

        assertTrue("customization_groups should be empty after ProductEntity deletion", groups.isEmpty())
        assertTrue("customization_options should be empty after ProductEntity deletion", options.isEmpty())
    }

    // ── Test: delete CustomizationGroupEntity cascades to options ─────────────
    //
    // Validates: Requirement 8.5
    //
    // WHEN a CustomizationGroupEntity is deleted, THE AppDatabase SHALL delete
    // all CustomizationOptionEntity rows whose groupId references that group.

    @Test
    fun deleteCustomizationGroup_cascadesOptions() = runBlocking {
        insertFullHierarchy()

        db.customizationGroupDao().deleteById("group-1")

        val options = db.customizationOptionDao().getOptionsByGroup("group-1").first()

        assertTrue("customization_options should be empty after CustomizationGroupEntity deletion", options.isEmpty())
    }

    // ── Test: FK violation on ProductEntity insert with unknown categoryId ────
    //
    // Validates: Requirement 6.5
    //
    // IF insert(product) is called where product.categoryId references no
    // existing CategoryEntity, THEN THE AppDatabase SHALL throw a foreign key
    // constraint exception and SHALL NOT persist the row.

    @Test
    fun insertProduct_withUnknownCategoryId_throwsConstraintException() = runBlocking {
        // Do NOT insert any CategoryEntity — the FK parent doesn't exist
        db.menuItemDao().insert(menuItem)

        val orphanProduct = ProductEntity(
            id = "prod-orphan",
            emoji = "🍕",
            name = "Orphan Pizza",
            description = "No category parent",
            basePrice = 12.00,
            isActive = true,
            categoryId = "non-existent-category"
        )

        var exceptionThrown = false
        try {
            db.productDao().insert(orphanProduct)
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            exceptionThrown = true
        }

        assertTrue("Expected SQLiteConstraintException for FK violation, but none was thrown", exceptionThrown)

        // Confirm the row was not persisted
        val rows = db.productDao().getProductsByCategory("non-existent-category").first()
        assertTrue("Orphan product should not have been persisted", rows.isEmpty())
    }
}
