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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CustomizationOptionDao].
 *
 * Validates: Requirements 4.4, 4.5, 4.6, 9.5
 *
 * FK hierarchy (must be inserted in order):
 *   MenuItemEntity → CategoryEntity → ProductEntity → CustomizationGroupEntity → CustomizationOptionEntity
 */
@RunWith(AndroidJUnit4::class)
class CustomizationOptionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var optionDao: CustomizationOptionDao

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

    private val product = ProductEntity(
        id = "prod-1",
        emoji = "🍔",
        name = "Classic Burger",
        description = "A juicy burger",
        basePrice = 9.99,
        isActive = true,
        categoryId = "cat-1"
    )

    private val groupA = CustomizationGroupEntity(
        id = "group-a",
        productId = "prod-1",
        groupName = "Size",
        selectionType = "single_option"
    )

    private val groupB = CustomizationGroupEntity(
        id = "group-b",
        productId = "prod-1",
        groupName = "Toppings",
        selectionType = "multiple_checkboxes"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .addCallback(foreignKeyCallback)
            .build()

        optionDao = db.customizationOptionDao()

        // Insert the full FK ancestor chain so option inserts don't violate constraints
        runBlocking {
            db.menuItemDao().insert(menuItem)
            db.categoryDao().insert(category)
            db.productDao().insert(product)
            db.customizationGroupDao().insertInternal(groupA)
            db.customizationGroupDao().insertInternal(groupB)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Property 5 — Count invariant (N=0 case) ──────────────────────────────
    //
    // When no options have been inserted for a groupId, getOptionsByGroup should
    // emit an empty list.
    //
    // Validates: Requirements 4.4, 4.5

    @Test
    fun getOptionsByGroup_noOptions_emitsEmptyList() = runBlocking {
        val result = optionDao.getOptionsByGroup("group-a").first()

        assertTrue("Expected empty list when no options inserted", result.isEmpty())
    }

    // ── Property 5 — Count invariant (N>0 case) ──────────────────────────────
    //
    // Insert N=5 distinct CustomizationOptionEntity rows under the same groupId.
    // Assert getOptionsByGroup emits a list of exactly size 5.
    //
    // Validates: Requirements 4.4, 4.5

    @Test
    fun getOptionsByGroup_nOptions_emitsListOfSizeN() = runBlocking {
        val options = (1..5).map { i ->
            CustomizationOptionEntity(
                id = "option-$i",
                groupId = "group-a",
                optionName = "Option $i",
                extraPrice = i * 0.5
            )
        }

        options.forEach { optionDao.insert(it) }

        val result = optionDao.getOptionsByGroup("group-a").first()

        assertEquals(5, result.size)
    }

    // ── Property 10 (Option): Query filter isolation ──────────────────────────
    //
    // Insert options under two different groupId values (groupA and groupB).
    // Assert getOptionsByGroup(groupA.id) returns only groupA's options and
    // getOptionsByGroup(groupB.id) returns only groupB's options with no
    // cross-contamination.
    //
    // Validates: Requirements 4.6, 9.5

    @Test
    fun getOptionsByGroup_filterIsolation_returnsOnlyOwnRows() = runBlocking {
        val optionsA = (1..3).map { i ->
            CustomizationOptionEntity(
                id = "opt-a-$i",
                groupId = "group-a",
                optionName = "Size option $i",
                extraPrice = i * 1.0
            )
        }
        val optionsB = (1..2).map { i ->
            CustomizationOptionEntity(
                id = "opt-b-$i",
                groupId = "group-b",
                optionName = "Topping option $i",
                extraPrice = i * 0.25
            )
        }

        optionsA.forEach { optionDao.insert(it) }
        optionsB.forEach { optionDao.insert(it) }

        val rowsForGroupA = optionDao.getOptionsByGroup("group-a").first()
        val rowsForGroupB = optionDao.getOptionsByGroup("group-b").first()

        // group-a should see exactly its 3 options
        assertEquals(3, rowsForGroupA.size)
        val idsForGroupA = rowsForGroupA.map { it.id }.toSet()
        assertEquals(setOf("opt-a-1", "opt-a-2", "opt-a-3"), idsForGroupA)

        // group-b should see exactly its 2 options
        assertEquals(2, rowsForGroupB.size)
        val idsForGroupB = rowsForGroupB.map { it.id }.toSet()
        assertEquals(setOf("opt-b-1", "opt-b-2"), idsForGroupB)

        // Cross-check: no option from group-b appears in group-a results
        assertTrue(rowsForGroupA.none { it.groupId == "group-b" })
        // Cross-check: no option from group-a appears in group-b results
        assertTrue(rowsForGroupB.none { it.groupId == "group-a" })
    }
}
