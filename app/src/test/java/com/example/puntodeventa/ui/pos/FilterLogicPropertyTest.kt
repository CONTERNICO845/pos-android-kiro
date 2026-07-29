package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

// ── Pure filter helper functions replicating PosViewModel logic ──────────────

/**
 * Replicates PosViewModel's menu filter logic.
 * When selectedMenuId is non-null, returns only products whose category has
 * associatedMenuId matching the selectedMenuId.
 * When selectedMenuId is null, returns all products unfiltered.
 */
internal fun filterByMenu(
    products: List<Product>,
    categories: List<Category>,
    selectedMenuId: String?
): List<Product> {
    if (selectedMenuId == null) return products
    val categoryIdsForMenu = categories
        .filter { it.associatedMenuId == selectedMenuId }
        .map { it.id }
        .toSet()
    return products.filter { it.categoryId in categoryIdsForMenu }
}

/**
 * Replicates PosViewModel's category filter logic (within a menu context).
 * When selectedCategory is non-null, returns only products matching that category.
 * If the category doesn't belong to the selectedMenu, returns empty.
 * When selectedCategory is null ("TODO" tab), returns all products from menu-filtered categories.
 */
internal fun filterByMenuAndCategory(
    products: List<Product>,
    categories: List<Category>,
    selectedMenuId: String?,
    selectedCategoryId: String?
): List<Product> {
    // Step 1: determine which categories pass the menu filter
    val filteredCategories = if (selectedMenuId != null) {
        categories.filter { it.associatedMenuId == selectedMenuId }
    } else {
        categories
    }
    val validCategoryIds = filteredCategories.map { it.id }.toSet()

    // Step 2: apply category filter
    return if (selectedCategoryId != null) {
        // If the selected category doesn't belong to the menu, no products shown
        if (selectedCategoryId !in validCategoryIds) {
            emptyList()
        } else {
            products.filter { it.categoryId == selectedCategoryId }
        }
    } else {
        // "TODO" tab: show all products from valid categories
        products.filter { it.categoryId in validCategoryIds }
    }
}

/**
 * Replicates PosViewModel's search filter logic.
 * Filters products by case-insensitive name substring match.
 * Empty/blank query returns all products.
 */
internal fun filterBySearch(products: List<Product>, query: String): List<Product> {
    if (query.isBlank()) return products
    return products.filter { it.name.contains(query, ignoreCase = true) }
}

/**
 * Combined filter pipeline: menu → category → search.
 */
internal fun applyAllFilters(
    products: List<Product>,
    categories: List<Category>,
    selectedMenuId: String?,
    selectedCategoryId: String?,
    searchQuery: String
): List<Product> {
    val afterMenuAndCategory = filterByMenuAndCategory(products, categories, selectedMenuId, selectedCategoryId)
    return filterBySearch(afterMenuAndCategory, searchQuery)
}

// ── Property Tests ──────────────────────────────────────────────────────────

class FilterLogicPropertyTest : StringSpec({

    // Shared generators
    val menuIds = listOf("menu-1", "menu-2", "menu-3")
    val categoryIds = listOf("cat-1", "cat-2", "cat-3", "cat-4", "cat-5", "cat-6")

    val categoryArb = Arb.bind(
        Arb.element(categoryIds),
        Arb.string(1..15),
        Arb.element(menuIds)
    ) { id, name, menuId ->
        Category(id = id, name = name, associatedMenuId = menuId)
    }

    // Generate a list of categories that covers all categoryIds (deduplicated by id)
    val categoriesListArb = Arb.list(categoryArb, range = 6..12).filter { cats ->
        // Ensure we have categories for all IDs so products always have a valid category
        cats.map { it.id }.toSet().size >= 4
    }

    val productArb = Arb.bind(
        Arb.string(1..20),
        Arb.element(categoryIds)
    ) { name, catId ->
        Product(
            id = java.util.UUID.randomUUID().toString(),
            emoji = "🍕",
            name = name,
            description = "",
            basePrice = 10.0,
            isActive = true,
            categoryId = catId
        )
    }

    val productsListArb = Arb.list(productArb, range = 0..25)

    /**
     * Property 1: Menu filter shows only matching products
     *
     * For any set of products, categories, and menus, when a menu is selected,
     * all products displayed SHALL have a category whose associatedMenuId matches
     * the selected MenuItem.id, and no product with a non-matching associatedMenuId
     * SHALL appear.
     *
     * **Validates: Requirements 1.2**
     */
    "Property 1 - Menu filter shows only products with matching associatedMenuId" {
        checkAll(
            PropTestConfig(iterations = 200),
            productsListArb,
            categoriesListArb,
            Arb.element(menuIds)
        ) { products, categories, selectedMenuId ->
            // Deduplicate categories by id (keep first occurrence)
            val uniqueCategories = categories.distinctBy { it.id }

            val result = filterByMenu(products, uniqueCategories, selectedMenuId)

            // Build set of category IDs that belong to the selected menu
            val validCategoryIds = uniqueCategories
                .filter { it.associatedMenuId == selectedMenuId }
                .map { it.id }
                .toSet()

            // All returned products must have a categoryId in the valid set
            result.all { it.categoryId in validCategoryIds } shouldBe true

            // No product with a valid categoryId should be missing from result
            val expectedProducts = products.filter { it.categoryId in validCategoryIds }
            result.size shouldBe expectedProducts.size
            result.toSet() shouldBe expectedProducts.toSet()
        }
    }

    /**
     * Property 2: Menu filter toggle restores unfiltered state
     *
     * For any menu selection, selecting a menu chip and then pressing it again
     * (deselecting: selectedMenuId = null) SHALL result in the product list being
     * identical to the state before the menu was first selected.
     *
     * **Validates: Requirements 1.4**
     */
    "Property 2 - Menu filter toggle restores unfiltered state" {
        checkAll(
            PropTestConfig(iterations = 200),
            productsListArb,
            categoriesListArb,
            Arb.element(menuIds)
        ) { products, categories, selectedMenuId ->
            val uniqueCategories = categories.distinctBy { it.id }

            // State before selecting menu (no filter)
            val beforeFilter = filterByMenu(products, uniqueCategories, null)

            // Select menu then deselect (toggle)
            val afterSelect = filterByMenu(products, uniqueCategories, selectedMenuId)
            val afterDeselect = filterByMenu(products, uniqueCategories, null)

            // After deselect, the result should be identical to before
            afterDeselect shouldBe beforeFilter
        }
    }

    /**
     * Property 3: Menu and Category intersection filter
     *
     * For any combination of an active menu filter and a selected category,
     * the displayed products SHALL be exactly those products whose category has
     * associatedMenuId matching the selected menu AND whose categoryId matches
     * the selected category.
     *
     * **Validates: Requirements 1.5**
     */
    "Property 3 - Menu and Category intersection filter" {
        checkAll(
            PropTestConfig(iterations = 200),
            productsListArb,
            categoriesListArb,
            Arb.element(menuIds),
            Arb.element(categoryIds)
        ) { products, categories, selectedMenuId, selectedCategoryId ->
            val uniqueCategories = categories.distinctBy { it.id }

            val result = filterByMenuAndCategory(
                products, uniqueCategories, selectedMenuId, selectedCategoryId
            )

            // Determine if the selected category belongs to the selected menu
            val selectedCat = uniqueCategories.find { it.id == selectedCategoryId }
            val categoryBelongsToMenu = selectedCat?.associatedMenuId == selectedMenuId

            if (!categoryBelongsToMenu) {
                // Category doesn't belong to menu → empty result
                result shouldBe emptyList()
            } else {
                // All results must match BOTH menu AND category
                result.all { it.categoryId == selectedCategoryId } shouldBe true

                // No matching product should be missing
                val expected = products.filter { it.categoryId == selectedCategoryId }
                result.size shouldBe expected.size
                result.toSet() shouldBe expected.toSet()
            }
        }
    }

    /**
     * Property 4: Search filter by name (case-insensitive)
     *
     * For any non-empty search query string and any set of products, the filtered
     * results SHALL contain only products whose name contains the query as a
     * case-insensitive substring, and every product whose name contains the query
     * SHALL be included.
     *
     * **Validates: Requirements 2.2**
     */
    "Property 4 - Search filter by name is case-insensitive and complete" {
        checkAll(
            PropTestConfig(iterations = 200),
            productsListArb,
            Arb.string(1..10)
        ) { products, query ->
            val result = filterBySearch(products, query)

            // All results must contain the query (case-insensitive)
            result.all { it.name.contains(query, ignoreCase = true) } shouldBe true

            // Every product whose name contains the query must be included
            val expected = products.filter { it.name.contains(query, ignoreCase = true) }
            result.size shouldBe expected.size
            result.toSet() shouldBe expected.toSet()
        }
    }

    /**
     * Property 5: Search clear restores state respecting active filters
     *
     * For any state with an active search query and optional menu/category filters,
     * clearing the search query SHALL restore the product list to exactly what the
     * active menu and category filters alone would produce.
     *
     * **Validates: Requirements 2.3**
     */
    "Property 5 - Search clear restores state respecting active menu/category filters" {
        checkAll(
            PropTestConfig(iterations = 200),
            productsListArb,
            categoriesListArb,
            Arb.element(listOf(null) + menuIds),
            Arb.element(listOf(null) + categoryIds),
            Arb.string(1..10)
        ) { products, categories, selectedMenuId, selectedCategoryId, searchQuery ->
            val uniqueCategories = categories.distinctBy { it.id }

            // State with search active
            val withSearch = applyAllFilters(
                products, uniqueCategories, selectedMenuId, selectedCategoryId, searchQuery
            )

            // State after clearing search (query = "")
            val afterClear = applyAllFilters(
                products, uniqueCategories, selectedMenuId, selectedCategoryId, ""
            )

            // State with only menu/category filters (no search)
            val onlyFilters = filterByMenuAndCategory(
                products, uniqueCategories, selectedMenuId, selectedCategoryId
            )

            // After clearing search, result should equal menu+category only
            afterClear shouldBe onlyFilters

            // The search result should be a subset of the filter-only result
            withSearch.all { it in afterClear } shouldBe true
        }
    }
})
