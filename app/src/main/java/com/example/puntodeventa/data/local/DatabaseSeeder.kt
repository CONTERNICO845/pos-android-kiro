package com.example.puntodeventa.data.local

import androidx.room.withTransaction
import java.util.UUID

/**
 * Populates the database with default seed data when the menu_items table is empty.
 * All operations run within a single Room transaction.
 */
class DatabaseSeeder {

    private fun deterministicId(namespace: String, name: String): String =
        UUID.nameUUIDFromBytes("$namespace:$name".toByteArray(Charsets.UTF_8)).toString()

    private object SeedData {
        // Initialized lazily via `build()` since we need the outer class's deterministicId
        lateinit var menu: MenuItemEntity
        lateinit var categories: List<CategoryEntity>
        lateinit var products: List<ProductEntity>
        lateinit var customizationGroups: List<CustomizationGroupEntity>
        lateinit var customizationOptions: List<CustomizationOptionEntity>
    }

    init {
        buildSeedData()
    }

    private fun buildSeedData() {
        // ── Menu Item (1 row) ──────────────────────────────────────────────────
        val menuId = deterministicId("menu", "Tacos")
        SeedData.menu = MenuItemEntity(
            id = menuId,
            emoji = "🌮",
            name = "Tacos"
        )

        // ── Categories (4 rows) ────────────────────────────────────────────────
        val categoryNames = listOf("Tacos", "Tortas", "Tacos Dorados", "Refrescos")
        SeedData.categories = categoryNames.map { name ->
            CategoryEntity(
                id = deterministicId("category", name),
                name = name,
                associatedMenuId = menuId
            )
        }

        val catTacos = SeedData.categories[0].id
        val catTortas = SeedData.categories[1].id
        val catTacosDorados = SeedData.categories[2].id
        val catRefrescos = SeedData.categories[3].id

        // ── Products (12 rows) ─────────────────────────────────────────────────
        data class ProductSpec(
            val name: String,
            val basePrice: Double,
            val emoji: String,
            val categoryId: String
        )

        val productSpecs = listOf(
            // Tacos (4)
            ProductSpec("Taco de Bistec", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Chorizo", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Tripa", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Costilla", 18.0, "🌮", catTacos),
            // Tortas (4)
            ProductSpec("Torta de Bistec", 40.0, "🍔", catTortas),
            ProductSpec("Torta de Chorizo", 40.0, "🍔", catTortas),
            ProductSpec("Torta de Tripa", 50.0, "🍔", catTortas),
            ProductSpec("Torta de Costilla", 50.0, "🍔", catTortas),
            // Tacos Dorados (2)
            ProductSpec("Taco Individual", 10.0, "🌮", catTacosDorados),
            ProductSpec("Orden de 5", 50.0, "🌮", catTacosDorados),
            // Refrescos (2)
            ProductSpec("Refresco Pequeño", 18.0, "🥤", catRefrescos),
            ProductSpec("Refresco Grande", 23.0, "🥤", catRefrescos)
        )

        SeedData.products = productSpecs.map { spec ->
            ProductEntity(
                id = deterministicId("product", spec.name),
                emoji = spec.emoji,
                name = spec.name,
                description = "",
                basePrice = spec.basePrice,
                isActive = true,
                categoryId = spec.categoryId
            )
        }

        // ── Customization Groups (10 rows) ─────────────────────────────────────
        // One per non-Refrescos product
        val nonRefrescosProducts = SeedData.products.filter { it.categoryId != catRefrescos }

        SeedData.customizationGroups = nonRefrescosProducts.map { product ->
            CustomizationGroupEntity(
                id = deterministicId("group", "${product.name}:Remover"),
                productId = product.id,
                groupName = "Remover",
                selectionType = SelectionType.MULTIPLE_CHECKBOXES.value
            )
        }

        // ── Customization Options (40 rows) ────────────────────────────────────
        val tacosOptions = listOf("Sin cilantro", "Sin cebolla", "Tortilla sin grasa")
        val tortasOptions = listOf("Cilantro", "Cebolla", "Crema", "Lechuga", "Jitomate")
        val tacosDoradosOptions = listOf("Lechuga", "Queso", "Jitomate", "Crema")

        SeedData.customizationOptions = SeedData.customizationGroups.flatMap { group ->
            val product = SeedData.products.first { it.id == group.productId }
            val optionNames = when (product.categoryId) {
                catTacos -> tacosOptions
                catTortas -> tortasOptions
                catTacosDorados -> tacosDoradosOptions
                else -> emptyList()
            }
            optionNames.map { optionName ->
                CustomizationOptionEntity(
                    id = deterministicId("option", "${product.name}:Remover:$optionName"),
                    groupId = group.id,
                    optionName = optionName,
                    extraPrice = 0.0
                )
            }
        }
    }

    /**
     * Checks if the database is empty and seeds it atomically if so.
     * If menu_items table has rows, returns immediately (no-op).
     * Exceptions propagate naturally — Room rolls back the transaction on error.
     */
    suspend fun seedIfEmpty(database: AppDatabase) {
        val cursor = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM menu_items")
        val count = cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
        if (count > 0) return

        database.withTransaction {
            // 1. MenuItemEntity
            database.menuItemDao().insert(SeedData.menu)

            // 2. CategoryEntity
            for (category in SeedData.categories) {
                database.categoryDao().insert(category)
            }

            // 3. ProductEntity
            for (product in SeedData.products) {
                database.productDao().insert(product)
            }

            // 4. CustomizationGroupEntity
            for (group in SeedData.customizationGroups) {
                database.customizationGroupDao().insertInternal(group)
            }

            // 5. CustomizationOptionEntity
            for (option in SeedData.customizationOptions) {
                database.customizationOptionDao().insert(option)
            }
        }
    }
}
