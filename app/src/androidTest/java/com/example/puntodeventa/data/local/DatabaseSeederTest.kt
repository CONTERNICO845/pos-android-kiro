package com.example.puntodeventa.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumented tests for [DatabaseSeeder].
 *
 * Validates: Requirements 1.1, 2.1
 *
 * Uses an in-memory Room database with foreign key enforcement enabled
 * to verify the seeder populates data correctly and atomically.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var seeder: DatabaseSeeder

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
            .allowMainThreadQueries()
            .addCallback(foreignKeyCallback)
            .build()
        seeder = DatabaseSeeder()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun seedDatabase() = runBlocking {
        seeder.seedIfEmpty(db)
    }

    // ── Smoke test ───────────────────────────────────────────────────────────

    /**
     * Validates: Requirements 1.1, 2.1
     * Verifies that seeding an empty database populates menu_items with at least one row.
     */
    @Test
    fun seedIfEmpty_onEmptyDatabase_populatesMenuItems() {
        seedDatabase()

        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM menu_items")
        val count = cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
        assertTrue("menu_items should have at least one row after seeding", count > 0)
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    /**
     * Validates: Requirements 14.1, 14.2
     * Seeding twice should not duplicate or modify any data.
     */
    @Test
    fun seedIfEmpty_calledTwice_leavesRowCountsUnchanged() {
        // First seed
        seedDatabase()

        // Record counts after first seed
        val menuCountBefore = queryCount("menu_items")
        val categoryCountBefore = queryCount("categories")
        val productCountBefore = queryCount("products")
        val groupCountBefore = queryCount("customization_groups")
        val optionCountBefore = queryCount("customization_options")

        // Second seed
        seedDatabase()

        // Assert counts unchanged
        assertEquals("menu_items count should be unchanged after second seed", menuCountBefore, queryCount("menu_items"))
        assertEquals("categories count should be unchanged after second seed", categoryCountBefore, queryCount("categories"))
        assertEquals("products count should be unchanged after second seed", productCountBefore, queryCount("products"))
        assertEquals("customization_groups count should be unchanged after second seed", groupCountBefore, queryCount("customization_groups"))
        assertEquals("customization_options count should be unchanged after second seed", optionCountBefore, queryCount("customization_options"))
    }

    private fun queryCount(tableName: String): Int {
        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tableName")
        return cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    // ── Foreign Key Integrity ────────────────────────────────────────────────

    /**
     * Validates: Requirements 13.1–13.6
     * Seeds with PRAGMA foreign_keys = ON (already enabled in setUp via foreignKeyCallback).
     * Verifies no FK constraint violation occurs and all relationships are intact.
     */
    @Test
    fun seedIfEmpty_withForeignKeysEnabled_completesWithoutConstraintErrors() {
        // The foreignKeyCallback in setUp already enables PRAGMA foreign_keys = ON.
        // Seeding should complete without throwing SQLiteConstraintException.
        seedDatabase()

        // Verify FK integrity: every category references an existing menu item
        val orphanCategories = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM categories WHERE associatedMenuId NOT IN (SELECT id FROM menu_items)"
        )
        assertEquals("No orphan categories should exist", 0, orphanCategories.use { it.moveToFirst(); it.getInt(0) })

        // Every product references an existing category
        val orphanProducts = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM products WHERE categoryId NOT IN (SELECT id FROM categories)"
        )
        assertEquals("No orphan products should exist", 0, orphanProducts.use { it.moveToFirst(); it.getInt(0) })

        // Every customization group references an existing product
        val orphanGroups = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM customization_groups WHERE productId NOT IN (SELECT id FROM products)"
        )
        assertEquals("No orphan customization groups should exist", 0, orphanGroups.use { it.moveToFirst(); it.getInt(0) })

        // Every customization option references an existing group
        val orphanOptions = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM customization_options WHERE groupId NOT IN (SELECT id FROM customization_groups)"
        )
        assertEquals("No orphan customization options should exist", 0, orphanOptions.use { it.moveToFirst(); it.getInt(0) })
    }

    // ── Helpers for deterministic ID computation ─────────────────────────────

    /** Mirrors DatabaseSeeder.deterministicId for computing expected IDs in tests. */
    private fun deterministicId(namespace: String, name: String): String =
        UUID.nameUUIDFromBytes("$namespace:$name".toByteArray(Charsets.UTF_8)).toString()

    // ── Seed data content tests (Task 4.2) ───────────────────────────────────

    /**
     * Validates: Requirement 3.1
     * Verifies exactly 1 menu item with name="Tacos" and emoji="🌮".
     */
    @Test
    fun seed_insertsExactlyOneMenuItemWithCorrectData() {
        seedDatabase()

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name, emoji FROM menu_items"
        )
        cursor.use {
            assertEquals("Should have exactly 1 menu item", 1, it.count)
            it.moveToFirst()
            val name = it.getString(it.getColumnIndexOrThrow("name"))
            val emoji = it.getString(it.getColumnIndexOrThrow("emoji"))
            assertEquals("Menu item name should be 'Tacos'", "Tacos", name)
            assertEquals("Menu item emoji should be '🌮'", "🌮", emoji)
        }
    }

    /**
     * Validates: Requirements 4.1–4.7
     * Verifies exactly 4 categories with correct names, all referencing the "Tacos" menu.
     */
    @Test
    fun seed_insertsExactlyFourCategoriesWithCorrectNames() {
        seedDatabase()

        val expectedMenuId = deterministicId("menu", "Tacos")
        val expectedNames = setOf("Tacos", "Tortas", "Tacos Dorados", "Refrescos")

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name, associatedMenuId FROM categories WHERE associatedMenuId = '$expectedMenuId'"
        )
        cursor.use {
            assertEquals("Should have exactly 4 categories", 4, it.count)
            val actualNames = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val menuId = it.getString(it.getColumnIndexOrThrow("associatedMenuId"))
                actualNames.add(name)
                assertEquals("Category associatedMenuId should reference Tacos menu", expectedMenuId, menuId)
            }
            assertEquals("Category names should match expected set", expectedNames, actualNames)
        }
    }

    /**
     * Validates: Requirements 5.1–5.7, 6.1–6.7, 7.1–7.5, 8.1–8.5
     * Verifies exactly 12 products with correct names, prices, emojis, and isActive=true.
     */
    @Test
    fun seed_insertsExactlyTwelveProductsWithCorrectData() {
        seedDatabase()

        // Verify total product count
        assertEquals("Should have exactly 12 products", 12, queryCount("products"))

        val catTacos = deterministicId("category", "Tacos")
        val catTortas = deterministicId("category", "Tortas")
        val catTacosDorados = deterministicId("category", "Tacos Dorados")
        val catRefrescos = deterministicId("category", "Refrescos")

        // ── Category "Tacos" (4 products) ──
        verifyProducts(
            categoryId = catTacos,
            expectedCount = 4,
            expectedProducts = mapOf(
                "Taco de Bistec" to 16.0,
                "Taco de Chorizo" to 16.0,
                "Taco de Tripa" to 16.0,
                "Taco de Costilla" to 18.0
            ),
            expectedEmoji = "🌮",
            categoryLabel = "Tacos"
        )

        // ── Category "Tortas" (4 products) ──
        verifyProducts(
            categoryId = catTortas,
            expectedCount = 4,
            expectedProducts = mapOf(
                "Torta de Bistec" to 40.0,
                "Torta de Chorizo" to 40.0,
                "Torta de Tripa" to 50.0,
                "Torta de Costilla" to 50.0
            ),
            expectedEmoji = "🍔",
            categoryLabel = "Tortas"
        )

        // ── Category "Tacos Dorados" (2 products) ──
        verifyProducts(
            categoryId = catTacosDorados,
            expectedCount = 2,
            expectedProducts = mapOf(
                "Taco Individual" to 10.0,
                "Orden de 5" to 50.0
            ),
            expectedEmoji = "🌮",
            categoryLabel = "Tacos Dorados"
        )

        // ── Category "Refrescos" (2 products) ──
        verifyProducts(
            categoryId = catRefrescos,
            expectedCount = 2,
            expectedProducts = mapOf(
                "Refresco Pequeño" to 18.0,
                "Refresco Grande" to 23.0
            ),
            expectedEmoji = "🥤",
            categoryLabel = "Refrescos"
        )
    }

    /** Helper to verify products in a given category. */
    private fun verifyProducts(
        categoryId: String,
        expectedCount: Int,
        expectedProducts: Map<String, Double>,
        expectedEmoji: String,
        categoryLabel: String
    ) {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name, basePrice, emoji, isActive FROM products WHERE categoryId = '$categoryId'"
        )
        cursor.use {
            assertEquals("$categoryLabel should have $expectedCount products", expectedCount, it.count)
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val price = it.getDouble(it.getColumnIndexOrThrow("basePrice"))
                val emoji = it.getString(it.getColumnIndexOrThrow("emoji"))
                val isActive = it.getInt(it.getColumnIndexOrThrow("isActive"))

                assertTrue("Product '$name' should be in expected set for $categoryLabel", expectedProducts.containsKey(name))
                assertEquals("Price for '$name' in $categoryLabel", expectedProducts[name]!!, price, 0.001)
                assertEquals("Emoji for '$name' in $categoryLabel", expectedEmoji, emoji)
                assertEquals("isActive for '$name' in $categoryLabel should be true (1)", 1, isActive)
            }
        }
    }

    /**
     * Validates: Requirements 9.1, 10.1, 11.1
     * Verifies exactly 10 customization groups with groupName="Remover" and
     * selectionType="multiple_checkboxes".
     */
    @Test
    fun seed_insertsExactlyTenCustomizationGroups() {
        seedDatabase()

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT groupName, selectionType FROM customization_groups"
        )
        cursor.use {
            assertEquals("Should have exactly 10 customization groups", 10, it.count)
            while (it.moveToNext()) {
                val groupName = it.getString(it.getColumnIndexOrThrow("groupName"))
                val selectionType = it.getString(it.getColumnIndexOrThrow("selectionType"))
                assertEquals("Group name should be 'Remover'", "Remover", groupName)
                assertEquals("Selection type should be 'multiple_checkboxes'", "multiple_checkboxes", selectionType)
            }
        }
    }

    /**
     * Validates: Requirements 9.2, 10.2, 11.2
     * Verifies exactly 40 customization options with correct names and extraPrice=0.0.
     * - Tacos: 3 options × 4 products = 12
     * - Tortas: 5 options × 4 products = 20
     * - Tacos Dorados: 4 options × 2 products = 8
     */
    @Test
    fun seed_insertsExactlyFortyCustomizationOptions() {
        seedDatabase()

        // Verify total count
        assertEquals("Should have exactly 40 customization options", 40, queryCount("customization_options"))

        // Verify all have extraPrice = 0.0
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT optionName, extraPrice FROM customization_options"
        )
        cursor.use {
            while (it.moveToNext()) {
                val extraPrice = it.getDouble(it.getColumnIndexOrThrow("extraPrice"))
                val optionName = it.getString(it.getColumnIndexOrThrow("optionName"))
                assertEquals("extraPrice for option '$optionName' should be 0.0", 0.0, extraPrice, 0.001)
            }
        }

        // Verify Tacos options (3 per product × 4 products = 12)
        val tacosOptionNames = setOf("Sin cilantro", "Sin cebolla", "Tortilla sin grasa")
        val tacosProductNames = listOf("Taco de Bistec", "Taco de Chorizo", "Taco de Tripa", "Taco de Costilla")
        verifyOptionsForProducts(tacosProductNames, tacosOptionNames, "Tacos")

        // Verify Tortas options (5 per product × 4 products = 20)
        val tortasOptionNames = setOf("Cilantro", "Cebolla", "Crema", "Lechuga", "Jitomate")
        val tortasProductNames = listOf("Torta de Bistec", "Torta de Chorizo", "Torta de Tripa", "Torta de Costilla")
        verifyOptionsForProducts(tortasProductNames, tortasOptionNames, "Tortas")

        // Verify Tacos Dorados options (4 per product × 2 products = 8)
        val tacosDoradosOptionNames = setOf("Lechuga", "Queso", "Jitomate", "Crema")
        val tacosDoradosProductNames = listOf("Taco Individual", "Orden de 5")
        verifyOptionsForProducts(tacosDoradosProductNames, tacosDoradosOptionNames, "Tacos Dorados")
    }

    /** Helper: verify correct options exist for each product via its customization group. */
    private fun verifyOptionsForProducts(
        productNames: List<String>,
        expectedOptionNames: Set<String>,
        categoryLabel: String
    ) {
        for (productName in productNames) {
            val groupId = deterministicId("group", "$productName:Remover")
            val cursor = db.openHelper.readableDatabase.query(
                "SELECT optionName FROM customization_options WHERE groupId = '$groupId'"
            )
            cursor.use {
                assertEquals(
                    "$categoryLabel product '$productName' should have ${expectedOptionNames.size} options",
                    expectedOptionNames.size, it.count
                )
                val actualNames = mutableSetOf<String>()
                while (it.moveToNext()) {
                    actualNames.add(it.getString(it.getColumnIndexOrThrow("optionName")))
                }
                assertEquals(
                    "Option names for '$productName' in $categoryLabel",
                    expectedOptionNames, actualNames
                )
            }
        }
    }

    /**
     * Validates: Requirements 12.1–12.3
     * Verifies zero customization groups and options for Refrescos products.
     */
    @Test
    fun seed_createsZeroCustomizationsForRefrescosProducts() {
        seedDatabase()

        // Get Refrescos product IDs
        val catRefrescos = deterministicId("category", "Refrescos")
        val productsCursor = db.openHelper.readableDatabase.query(
            "SELECT id FROM products WHERE categoryId = '$catRefrescos'"
        )
        val refrescosProductIds = mutableListOf<String>()
        productsCursor.use {
            while (it.moveToNext()) {
                refrescosProductIds.add(it.getString(it.getColumnIndexOrThrow("id")))
            }
        }
        // Sanity check: we should have 2 Refrescos products
        assertEquals("Refrescos should have 2 products", 2, refrescosProductIds.size)

        // Query customization groups for Refrescos products
        for (productId in refrescosProductIds) {
            val groupCursor = db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM customization_groups WHERE productId = '$productId'"
            )
            groupCursor.use {
                it.moveToFirst()
                assertEquals("Refrescos product $productId should have 0 customization groups", 0, it.getInt(0))
            }
        }

        // Also verify no customization options exist via group join for Refrescos
        val optionsCursor = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM customization_options co " +
            "INNER JOIN customization_groups cg ON co.groupId = cg.id " +
            "WHERE cg.productId IN (${refrescosProductIds.joinToString(",") { "'$it'" }})"
        )
        optionsCursor.use {
            it.moveToFirst()
            assertEquals("Refrescos should have 0 customization options", 0, it.getInt(0))
        }
    }
}
