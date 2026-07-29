package com.example.puntodeventa.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests that verify the database seeding is visible through the
 * same repository/DAO layer the POS UI uses. These tests simulate what happens
 * when the app starts for the first time and the user navigates to the POS screen.
 *
 * Each test creates a fresh in-memory database, runs the seeder (same as app startup),
 * then queries through the repositories just like the ViewModels do.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSeederIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var seeder: DatabaseSeeder
    private lateinit var menuRepository: MenuRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var productRepository: ProductRepository

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

        // Wire up repositories exactly as MainActivity does
        menuRepository = MenuRepository(db.menuItemDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        productRepository = ProductRepository(
            productDao = db.productDao(),
            groupDao = db.customizationGroupDao(),
            optionDao = db.customizationOptionDao(),
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 1: Menu "Tacos" is visible in HomeScreen after seeding
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the HomeViewModel flow: after seeding, the MenuRepository should
     * emit a list containing the "Tacos" menu item with its 🌮 emoji.
     * This is what powers `homeUiState.menuItems` and produces `activeMenuId`.
     */
    @Test
    fun afterSeeding_menuRepositoryEmitsTacosMenuItem() = runBlocking {
        // Seed (same as app startup)
        seeder.seedIfEmpty(db)

        // Query via repository (same as HomeViewModel subscribes)
        val menuItems = menuRepository.menuItems.first()

        // The POS should see at least 1 menu item
        assertTrue(
            "MenuRepository should emit at least 1 menu item after seeding",
            menuItems.isNotEmpty()
        )

        // The first (and only) menu item should be "Tacos" with 🌮
        val tacosMenu = menuItems.first()
        assertEquals("Menu item name should be 'Tacos'", "Tacos", tacosMenu.name)
        assertEquals("Menu item emoji should be '🌮'", "🌮", tacosMenu.emoji)
        assertTrue("Menu item id should not be empty", tacosMenu.id.isNotEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 2: POS categories are loaded for the seeded menu
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the PosViewModel flow: using the menuId from the seeded "Tacos"
     * menu, the CategoryRepository should return 4 categories (Tacos, Tortas,
     * Tacos Dorados, Refrescos). These are the tabs the user sees in the POS.
     */
    @Test
    fun afterSeeding_categoryRepositoryReturnsFourCategoriesForTacosMenu() = runBlocking {
        // Seed
        seeder.seedIfEmpty(db)

        // Get the menuId (same as MainActivity derives activeMenuId)
        val menuItems = menuRepository.menuItems.first()
        val activeMenuId = menuItems.first().id

        // Query categories (same as PosViewModel.categoriesFlow)
        val categories = categoryRepository.getCategoriesByMenu(activeMenuId).first()

        assertEquals(
            "POS should display exactly 4 category tabs",
            4, categories.size
        )

        val categoryNames = categories.map { it.name }.toSet()
        assertEquals(
            "Category names should match the expected set",
            setOf("Tacos", "Tortas", "Tacos Dorados", "Refrescos"),
            categoryNames
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 3: POS products are visible when selecting a category
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the full POS flow: seed → get menuId → get categories → select
     * "Tacos" category → verify products are loaded with correct names and prices.
     * This proves the product grid will show items to the POS operator.
     */
    @Test
    fun afterSeeding_productsAreVisibleWhenSelectingTacosCategory() = runBlocking {
        // Seed
        seeder.seedIfEmpty(db)

        // Get menuId
        val menuItems = menuRepository.menuItems.first()
        val activeMenuId = menuItems.first().id

        // Get categories
        val categories = categoryRepository.getCategoriesByMenu(activeMenuId).first()
        val tacosCategory = categories.first { it.name == "Tacos" }

        // Query products for "Tacos" category (same as PosViewModel.productsFlow)
        val products = productRepository.getActiveProductsByCategory(tacosCategory.id).first()

        assertEquals(
            "Tacos category should have 4 products in the POS grid",
            4, products.size
        )

        // Verify all expected products are present with correct prices
        val productMap = products.associate { it.name to it.basePrice }
        assertEquals("Taco de Bistec price", 16.0, productMap["Taco de Bistec"]!!, 0.001)
        assertEquals("Taco de Chorizo price", 16.0, productMap["Taco de Chorizo"]!!, 0.001)
        assertEquals("Taco de Tripa price", 16.0, productMap["Taco de Tripa"]!!, 0.001)
        assertEquals("Taco de Costilla price", 18.0, productMap["Taco de Costilla"]!!, 0.001)

        // Verify all products are active (visible in POS)
        assertTrue(
            "All Tacos products should be active (isActive=true)",
            products.all { it.isActive }
        )

        // Also verify Tortas category has products
        val tortasCategory = categories.first { it.name == "Tortas" }
        val tortasProducts = productRepository.getActiveProductsByCategory(tortasCategory.id).first()
        assertEquals("Tortas category should have 4 products", 4, tortasProducts.size)

        // And Refrescos
        val refrescosCategory = categories.first { it.name == "Refrescos" }
        val refrescosProducts = productRepository.getActiveProductsByCategory(refrescosCategory.id).first()
        assertEquals("Refrescos category should have 2 products", 2, refrescosProducts.size)
    }
}
