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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CategoryDao].
 *
 * Validates: Requirements 1.3, 1.4, 1.5, 1.6, 9.4
 *
 * Properties tested:
 *   - Property 4:  Count invariant — getCategoriesByMenu
 *   - Property 7:  Idempotent upsert (REPLACE strategy)
 *   - Property 10: Query filter isolation
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var menuItemDao: MenuItemDao

    /** Enables SQLite FK enforcement for the in-memory test database. */
    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(foreignKeyCallback)
            .build()
        categoryDao = db.categoryDao()
        menuItemDao = db.menuItemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private suspend fun insertParentMenu(menuId: String) {
        menuItemDao.insert(MenuItemEntity(id = menuId, emoji = "🍔", name = "Menu $menuId"))
    }

    // ── Property 4: Count invariant — getCategoriesByMenu ────────────────────

    /**
     * **Property 4 — Validates: Requirements 9.4, 1.5**
     * Inserting N distinct CategoryEntity rows under the same associatedMenuId
     * causes getCategoriesByMenu to emit a list of exactly N items.
     */
    @Test
    fun property4_countInvariant_emitsExactlyNItems() = runBlocking {
        val menuId = "menu-count-test"
        insertParentMenu(menuId)

        val n = 5
        repeat(n) { i ->
            categoryDao.insert(
                CategoryEntity(
                    id = "cat-$i",
                    name = "Category $i",
                    associatedMenuId = menuId
                )
            )
        }

        val result = categoryDao.getCategoriesByMenu(menuId).first()
        assertEquals(n, result.size)
    }

    /**
     * **Property 4 edge case — Validates: Requirements 9.4, 1.5**
     * When N = 0 (no rows inserted), getCategoriesByMenu emits an empty list.
     */
    @Test
    fun property4_countInvariant_zeroItems_emitsEmptyList() = runBlocking {
        val menuId = "menu-empty"
        insertParentMenu(menuId)

        val result = categoryDao.getCategoriesByMenu(menuId).first()
        assertEquals(0, result.size)
    }

    // ── Property 7: Idempotent upsert ────────────────────────────────────────

    /**
     * **Property 7 — Validates: Requirements 1.3**
     * Inserting the same CategoryEntity id twice with different names results in
     * exactly one row, and that row has the name from the second insert.
     */
    @Test
    fun property7_idempotentUpsert_secondInsertWins() = runBlocking {
        val menuId = "menu-upsert"
        insertParentMenu(menuId)

        val id = "cat-upsert"
        val firstName = "First Name"
        val secondName = "Second Name"

        categoryDao.insert(CategoryEntity(id = id, name = firstName, associatedMenuId = menuId))
        categoryDao.insert(CategoryEntity(id = id, name = secondName, associatedMenuId = menuId))

        val result = categoryDao.getCategoriesByMenu(menuId).first()

        assertEquals("Should have exactly one row after duplicate insert", 1, result.size)
        assertEquals("Row should reflect the second insert's name", secondName, result[0].name)
        assertEquals("Row id should be preserved", id, result[0].id)
    }

    // ── Property 10: Query filter isolation ──────────────────────────────────

    /**
     * **Property 10 — Validates: Requirements 1.6, 9.4**
     * Categories inserted under menuIdA are not returned by getCategoriesByMenu(menuIdB)
     * and vice versa.
     */
    @Test
    fun property10_queryFilterIsolation_eachMenuSeesOnlyItsOwnRows() = runBlocking {
        val menuIdA = "menu-A"
        val menuIdB = "menu-B"
        insertParentMenu(menuIdA)
        insertParentMenu(menuIdB)

        // Insert 3 categories under A, 2 under B
        val categoriesA = listOf(
            CategoryEntity(id = "cat-a1", name = "Cat A1", associatedMenuId = menuIdA),
            CategoryEntity(id = "cat-a2", name = "Cat A2", associatedMenuId = menuIdA),
            CategoryEntity(id = "cat-a3", name = "Cat A3", associatedMenuId = menuIdA)
        )
        val categoriesB = listOf(
            CategoryEntity(id = "cat-b1", name = "Cat B1", associatedMenuId = menuIdB),
            CategoryEntity(id = "cat-b2", name = "Cat B2", associatedMenuId = menuIdB)
        )

        (categoriesA + categoriesB).forEach { categoryDao.insert(it) }

        val resultA = categoryDao.getCategoriesByMenu(menuIdA).first()
        val resultB = categoryDao.getCategoriesByMenu(menuIdB).first()

        // Count checks
        assertEquals("Menu A should see exactly 3 categories", 3, resultA.size)
        assertEquals("Menu B should see exactly 2 categories", 2, resultB.size)

        // Isolation checks — no cross-contamination
        val idsA = resultA.map { it.id }.toSet()
        val idsB = resultB.map { it.id }.toSet()
        assert(idsA.none { it in idsB }) { "Menu A results contain ids from Menu B: ${idsA.intersect(idsB)}" }
        assert(idsB.none { it in idsA }) { "Menu B results contain ids from Menu A: ${idsB.intersect(idsA)}" }

        // All A results reference menuIdA
        resultA.forEach { cat ->
            assertEquals("Category ${cat.id} should belong to menu A", menuIdA, cat.associatedMenuId)
        }
        // All B results reference menuIdB
        resultB.forEach { cat ->
            assertEquals("Category ${cat.id} should belong to menu B", menuIdB, cat.associatedMenuId)
        }
    }
}
