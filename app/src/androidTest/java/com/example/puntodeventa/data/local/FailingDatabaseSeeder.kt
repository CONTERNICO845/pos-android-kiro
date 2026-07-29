package com.example.puntodeventa.data.local

import androidx.room.withTransaction
import java.util.UUID

/**
 * Exception thrown to simulate an insertion failure at a specific step.
 */
class SimulatedInsertionFailure(step: Int) :
    RuntimeException("Simulated insertion failure at step $step")

/**
 * A variant of [DatabaseSeeder] that throws [SimulatedInsertionFailure] at a
 * configurable insertion step. Used by property-based tests to verify transaction
 * rollback behavior.
 *
 * Steps:
 * - 0: Fail during menu_items insertion
 * - 1: Fail during categories insertion
 * - 2: Fail during products insertion
 * - 3: Fail during customization_groups insertion
 * - 4: Fail during customization_options insertion
 */
class FailingDatabaseSeeder(private val failAtStep: Int) {

    private fun deterministicId(namespace: String, name: String): String =
        UUID.nameUUIDFromBytes("$namespace:$name".toByteArray(Charsets.UTF_8)).toString()

    private val menu: MenuItemEntity
    private val categories: List<CategoryEntity>
    private val products: List<ProductEntity>
    private val customizationGroups: List<CustomizationGroupEntity>
    private val customizationOptions: List<CustomizationOptionEntity>

    init {
        val menuId = deterministicId("menu", "Tacos")
        menu = MenuItemEntity(id = menuId, emoji = "🌮", name = "Tacos")

        val categoryNames = listOf("Tacos", "Tortas", "Tacos Dorados", "Refrescos")
        categories = categoryNames.map { name ->
            CategoryEntity(
                id = deterministicId("category", name),
                name = name,
                associatedMenuId = menuId
            )
        }

        val catTacos = categories[0].id
        val catTortas = categories[1].id
        val catTacosDorados = categories[2].id
        val catRefrescos = categories[3].id

        data class ProductSpec(
            val name: String, val basePrice: Double,
            val emoji: String, val categoryId: String
        )

        val productSpecs = listOf(
            ProductSpec("Taco de Bistec", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Chorizo", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Tripa", 16.0, "🌮", catTacos),
            ProductSpec("Taco de Costilla", 18.0, "🌮", catTacos),
            ProductSpec("Torta de Bistec", 40.0, "🍔", catTortas),
            ProductSpec("Torta de Chorizo", 40.0, "🍔", catTortas),
            ProductSpec("Torta de Tripa", 50.0, "🍔", catTortas),
            ProductSpec("Torta de Costilla", 50.0, "🍔", catTortas),
            ProductSpec("Taco Individual", 10.0, "🌮", catTacosDorados),
            ProductSpec("Orden de 5", 50.0, "🌮", catTacosDorados),
            ProductSpec("Refresco Pequeño", 18.0, "🥤", catRefrescos),
            ProductSpec("Refresco Grande", 23.0, "🥤", catRefrescos)
        )

        products = productSpecs.map { spec ->
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

        val nonRefrescosProducts = products.filter { it.categoryId != catRefrescos }
        customizationGroups = nonRefrescosProducts.map { product ->
            CustomizationGroupEntity(
                id = deterministicId("group", "${product.name}:Remover"),
                productId = product.id,
                groupName = "Remover",
                selectionType = SelectionType.MULTIPLE_CHECKBOXES.value
            )
        }

        val tacosOptions = listOf("Sin cilantro", "Sin cebolla", "Tortilla sin grasa")
        val tortasOptions = listOf("Cilantro", "Cebolla", "Crema", "Lechuga", "Jitomate")
        val tacosDoradosOptions = listOf("Lechuga", "Queso", "Jitomate", "Crema")

        customizationOptions = customizationGroups.flatMap { group ->
            val product = products.first { it.id == group.productId }
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
     * Seeds the database but throws [SimulatedInsertionFailure] at the configured step.
     * Room's withTransaction should roll back all prior inserts within the same transaction.
     */
    suspend fun seedIfEmpty(database: AppDatabase) {
        val cursor = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM menu_items")
        val count = cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
        if (count > 0) return

        database.withTransaction {
            // Step 0: menu_items
            if (failAtStep == 0) throw SimulatedInsertionFailure(0)
            database.menuItemDao().insert(menu)

            // Step 1: categories
            if (failAtStep == 1) throw SimulatedInsertionFailure(1)
            for (category in categories) {
                database.categoryDao().insert(category)
            }

            // Step 2: products
            if (failAtStep == 2) throw SimulatedInsertionFailure(2)
            for (product in products) {
                database.productDao().insert(product)
            }

            // Step 3: customization_groups
            if (failAtStep == 3) throw SimulatedInsertionFailure(3)
            for (group in customizationGroups) {
                database.customizationGroupDao().insertInternal(group)
            }

            // Step 4: customization_options
            if (failAtStep == 4) throw SimulatedInsertionFailure(4)
            for (option in customizationOptions) {
                database.customizationOptionDao().insert(option)
            }
        }
    }
}
