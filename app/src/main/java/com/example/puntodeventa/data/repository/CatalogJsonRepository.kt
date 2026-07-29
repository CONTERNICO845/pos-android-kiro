package com.example.puntodeventa.data.repository

import androidx.room.withTransaction
import com.example.puntodeventa.data.json.CatalogData
import com.example.puntodeventa.data.json.CatalogExport
import com.example.puntodeventa.data.json.CategoryDto
import com.example.puntodeventa.data.json.CustomizationGroupDto
import com.example.puntodeventa.data.json.CustomizationOptionDto
import com.example.puntodeventa.data.json.MenuItemDto
import com.example.puntodeventa.data.json.ProductDto
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CategoryDao
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.CustomizationGroupDao
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionDao
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.MenuItemDao
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.local.ProductDao
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.local.SelectionType
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Repository that handles catalog serialization to/from JSON and
 * transactional import (replace-all strategy).
 */
class CatalogJsonRepository(
    private val database: AppDatabase,
    private val menuItemDao: MenuItemDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val groupDao: CustomizationGroupDao,
    private val optionDao: CustomizationOptionDao
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    /**
     * Reads the entire catalog hierarchy from the database and serializes it
     * to a pretty-printed JSON string.
     */
    suspend fun exportCatalogToJson(): String {
        val menuItems = menuItemDao.getAllMenuItemsOnce()

        val menuItemDtos = menuItems.map { menu ->
            val categories = categoryDao.getCategoriesByMenuOnce(menu.id)

            val categoryDtos = categories.map { category ->
                val products = productDao.getProductsByCategoryOnce(category.id)

                val productDtos = products.map { product ->
                    val groups = groupDao.getGroupsByProductOnce(product.id)

                    val groupDtos = groups.map { group ->
                        val options = optionDao.getOptionsByGroupOnce(group.id)

                        CustomizationGroupDto(
                            id = group.id,
                            groupName = group.groupName,
                            selectionType = group.selectionType,
                            options = options.map { option ->
                                CustomizationOptionDto(
                                    id = option.id,
                                    optionName = option.optionName,
                                    extraPrice = option.extraPrice
                                )
                            }
                        )
                    }

                    ProductDto(
                        id = product.id,
                        emoji = product.emoji,
                        name = product.name,
                        description = product.description,
                        basePrice = product.basePrice,
                        isActive = product.isActive,
                        customizationGroups = groupDtos
                    )
                }

                CategoryDto(
                    id = category.id,
                    name = category.name,
                    products = productDtos
                )
            }

            MenuItemDto(
                id = menu.id,
                emoji = menu.emoji,
                name = menu.name,
                categories = categoryDtos
            )
        }

        val export = CatalogExport(
            version = 1,
            exportedAt = Instant.now().toString(),
            catalog = CatalogData(menuItems = menuItemDtos)
        )

        return json.encodeToString(CatalogExport.serializer(), export)
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Parses, validates, and imports a JSON string as the new catalog.
     * Uses a replace-all strategy inside a single Room transaction.
     *
     * @return [Result.success] with the total number of products imported,
     *         or [Result.failure] with a descriptive error message.
     */
    suspend fun importCatalogFromJson(jsonString: String): Result<Int> {
        // 1. Parse
        val export = try {
            json.decodeFromString(CatalogExport.serializer(), jsonString)
        } catch (e: Exception) {
            return Result.failure(
                IllegalArgumentException("El archivo no es un JSON válido: ${e.message}")
            )
        }

        // 2. Validate
        val validation = validate(export)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        // 3. Count products for the success message
        val productCount = export.catalog.menuItems
            .flatMap { it.categories }
            .flatMap { it.products }
            .size

        // 4. Execute replace-all transaction
        try {
            database.withTransaction {
                // Delete in child-to-parent order to respect FK constraints
                optionDao.deleteAll()
                groupDao.deleteAll()
                productDao.deleteAll()
                categoryDao.deleteAll()
                menuItemDao.deleteAll()

                // Insert in parent-to-child order
                for (menuDto in export.catalog.menuItems) {
                    menuItemDao.insert(
                        MenuItemEntity(
                            id = menuDto.id,
                            emoji = menuDto.emoji,
                            name = menuDto.name
                        )
                    )

                    for (catDto in menuDto.categories) {
                        categoryDao.insert(
                            CategoryEntity(
                                id = catDto.id,
                                name = catDto.name,
                                associatedMenuId = menuDto.id
                            )
                        )

                        for (prodDto in catDto.products) {
                            productDao.insert(
                                ProductEntity(
                                    id = prodDto.id,
                                    emoji = prodDto.emoji,
                                    name = prodDto.name,
                                    description = prodDto.description,
                                    basePrice = prodDto.basePrice,
                                    isActive = prodDto.isActive,
                                    categoryId = catDto.id
                                )
                            )

                            for (groupDto in prodDto.customizationGroups) {
                                groupDao.insertInternal(
                                    CustomizationGroupEntity(
                                        id = groupDto.id,
                                        productId = prodDto.id,
                                        groupName = groupDto.groupName,
                                        selectionType = groupDto.selectionType
                                    )
                                )

                                for (optDto in groupDto.options) {
                                    optionDao.insert(
                                        CustomizationOptionEntity(
                                            id = optDto.id,
                                            groupId = groupDto.id,
                                            optionName = optDto.optionName,
                                            extraPrice = optDto.extraPrice
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return Result.failure(
                RuntimeException("Error al importar: ${e.message}")
            )
        }

        return Result.success(productCount)
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate(export: CatalogExport): Result<Unit> {
        // Version check
        if (export.version != 1) {
            return Result.failure(
                IllegalArgumentException("Versión de esquema no soportada (${export.version})")
            )
        }

        val allIds = mutableSetOf<String>()

        for (menu in export.catalog.menuItems) {
            if (menu.id.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("MenuItem tiene un ID vacío")
                )
            }
            if (!allIds.add(menu.id)) {
                return Result.failure(
                    IllegalArgumentException("ID duplicado: ${menu.id}")
                )
            }

            for (cat in menu.categories) {
                if (cat.id.isBlank()) {
                    return Result.failure(
                        IllegalArgumentException("Category tiene un ID vacío")
                    )
                }
                if (!allIds.add(cat.id)) {
                    return Result.failure(
                        IllegalArgumentException("ID duplicado: ${cat.id}")
                    )
                }

                for (prod in cat.products) {
                    if (prod.id.isBlank()) {
                        return Result.failure(
                            IllegalArgumentException("Product tiene un ID vacío")
                        )
                    }
                    if (!allIds.add(prod.id)) {
                        return Result.failure(
                            IllegalArgumentException("ID duplicado: ${prod.id}")
                        )
                    }
                    if (prod.basePrice < 0.0) {
                        return Result.failure(
                            IllegalArgumentException("basePrice negativo en producto '${prod.name}': ${prod.basePrice}")
                        )
                    }

                    for (group in prod.customizationGroups) {
                        if (group.id.isBlank()) {
                            return Result.failure(
                                IllegalArgumentException("CustomizationGroup tiene un ID vacío")
                            )
                        }
                        if (!allIds.add(group.id)) {
                            return Result.failure(
                                IllegalArgumentException("ID duplicado: ${group.id}")
                            )
                        }
                        if (SelectionType.fromValue(group.selectionType) == null) {
                            return Result.failure(
                                IllegalArgumentException(
                                    "selectionType inválido '${group.selectionType}' en grupo '${group.groupName}'. " +
                                    "Debe ser: ${SelectionType.entries.map { it.value }}"
                                )
                            )
                        }

                        for (opt in group.options) {
                            if (opt.id.isBlank()) {
                                return Result.failure(
                                    IllegalArgumentException("CustomizationOption tiene un ID vacío")
                                )
                            }
                            if (!allIds.add(opt.id)) {
                                return Result.failure(
                                    IllegalArgumentException("ID duplicado: ${opt.id}")
                                )
                            }
                            if (opt.extraPrice < 0.0) {
                                return Result.failure(
                                    IllegalArgumentException("extraPrice negativo en opción '${opt.optionName}': ${opt.extraPrice}")
                                )
                            }
                        }
                    }
                }
            }
        }

        return Result.success(Unit)
    }
}
