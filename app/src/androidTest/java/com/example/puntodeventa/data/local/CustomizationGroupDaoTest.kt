package com.example.puntodeventa.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CustomizationGroupDao].
 *
 * Validates: Requirements 3.4, 3.5, 3.6
 *
 * FK hierarchy (must be inserted in order):
 *   MenuItemEntity → CategoryEntity → ProductEntity → CustomizationGroupEntity
 */
@RunWith(AndroidJUnit4::class)
class CustomizationGroupDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var groupDao: CustomizationGroupDao

    // Enable foreign-key constraints for in-memory test database
    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Shared ancestor rows ─────────────────────────────────────────────────

    private val menuItem = MenuItemEntity(id = "menu-1", emoji = "🍔", name = "Burgers")

    private val category = CategoryEntity(
        id = "cat-1",
        name = "Mains",
        associatedMenuId = "menu-1"
    )

    private val product1 = ProductEntity(
        id = "prod-1",
        emoji = "🍔",
        name = "Classic Burger",
        description = "A juicy burger",
        basePrice = 9.99,
        isActive = true,
        categoryId = "cat-1"
    )

    private val product2 = ProductEntity(
        id = "prod-2",
        emoji = "🌮",
        name = "Taco",
        description = "A crunchy taco",
        basePrice = 5.99,
        isActive = true,
        categoryId = "cat-1"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .addCallback(foreignKeyCallback)
            .build()

        groupDao = db.customizationGroupDao()

        // Insert the full FK ancestor chain so group inserts don't violate constraints
        runBlocking {
            db.menuItemDao().insert(menuItem)
            db.categoryDao().insert(category)
            db.productDao().insert(product1)
            db.productDao().insert(product2)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 7 (Group): Idempotent upsert via insertInternal ────────────
    //
    // Insert the same CustomizationGroupEntity (same id) twice with different
    // groupName values. Assert exactly one row survives and it retains the
    // second groupName.
    //
    // Validates: Requirements 3.4, 3.5

    @Test
    fun insertInternal_idempotentUpsert_secondInsertWins() = runBlocking {
        val group = CustomizationGroupEntity(
            id = "group-1",
            productId = "prod-1",
            groupName = "First Name",
            selectionType = "single_option"
        )
        val updatedGroup = group.copy(groupName = "Second Name")

        groupDao.insertInternal(group)
        groupDao.insertInternal(updatedGroup)

        val rows = groupDao.getGroupsByProduct("prod-1").first()

        // Only one row should exist for this id
        assertEquals(1, rows.size)
        // The second insert's groupName must have replaced the first
        assertEquals("Second Name", rows[0].groupName)
    }

    // ── Property 10 (Group): Query filter isolation for getGroupsByProduct ───
    //
    // Insert groups under two different productId values.
    // Assert each query returns ONLY its own rows and does not leak rows from
    // the other product.
    //
    // Validates: Requirements 3.6

    @Test
    fun getGroupsByProduct_filterIsolation_returnsOnlyOwnRows() = runBlocking {
        val groupA1 = CustomizationGroupEntity(
            id = "group-a1",
            productId = "prod-1",
            groupName = "Size",
            selectionType = "single_option"
        )
        val groupA2 = CustomizationGroupEntity(
            id = "group-a2",
            productId = "prod-1",
            groupName = "Sauce",
            selectionType = "multiple_checkboxes"
        )
        val groupB1 = CustomizationGroupEntity(
            id = "group-b1",
            productId = "prod-2",
            groupName = "Filling",
            selectionType = "multiple_checkboxes"
        )

        groupDao.insertInternal(groupA1)
        groupDao.insertInternal(groupA2)
        groupDao.insertInternal(groupB1)

        val rowsForProd1 = groupDao.getGroupsByProduct("prod-1").first()
        val rowsForProd2 = groupDao.getGroupsByProduct("prod-2").first()

        // prod-1 should see exactly its two groups
        assertEquals(2, rowsForProd1.size)
        val idsForProd1 = rowsForProd1.map { it.id }.toSet()
        assertEquals(setOf("group-a1", "group-a2"), idsForProd1)

        // prod-2 should see only its own group
        assertEquals(1, rowsForProd2.size)
        assertEquals("group-b1", rowsForProd2[0].id)

        // Cross-check: no group from prod-2 appears in prod-1 results
        assert(rowsForProd1.none { it.productId == "prod-2" })
        // Cross-check: no group from prod-1 appears in prod-2 results
        assert(rowsForProd2.none { it.productId == "prod-1" })
    }
}
