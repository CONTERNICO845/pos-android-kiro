package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.model.Product
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

// Feature: pos-main-screen, Property 2: Product Filtering and Sorting by Category

class ProductFilteringSortingPropertyTest : StringSpec({

    /**
     * Property 2: Product Filtering and Sorting by Category
     *
     * For any set of products and any selected category (including the "TODO" all-products case),
     * the resulting product list SHALL contain only products where isActive == true and
     * (if a specific category is selected) categoryId matches the selected category,
     * sorted by name ascending (case-insensitive).
     *
     * **Validates: Requirements 3.4, 3.5, 4.2, 4.3, 10.4**
     */
    "Property 2 - sortAndFilterProducts returns only active products matching category, sorted by name ascending (case-insensitive)" {
        val categoryPool = listOf("cat-1", "cat-2", "cat-3")

        val productArb = Arb.bind(
            Arb.string(1..30),
            Arb.boolean(),
            Arb.element(categoryPool)
        ) { name, isActive, categoryId ->
            Product(
                id = java.util.UUID.randomUUID().toString(),
                emoji = "🍕",
                name = name,
                description = "desc",
                basePrice = 10.0,
                isActive = isActive,
                categoryId = categoryId
            )
        }

        checkAll(
            PropTestConfig(iterations = 200),
            Arb.list(productArb, range = 0..30),
            Arb.element(listOf(null, "cat-1", "cat-2", "cat-3"))
        ) { products, selectedCategoryId ->
            val result = sortAndFilterProducts(products, selectedCategoryId)

            // 1. Result only contains active products
            result.all { it.isActive } shouldBe true

            // 2. If selectedCategoryId is not null, all results have matching categoryId
            if (selectedCategoryId != null) {
                result.all { it.categoryId == selectedCategoryId } shouldBe true
            }

            // 3. If selectedCategoryId is null, all active products from all categories are included
            if (selectedCategoryId == null) {
                val expectedActiveProducts = products.filter { it.isActive }
                result.size shouldBe expectedActiveProducts.size
            }

            // 4. Result is sorted by name ascending (case-insensitive)
            val names = result.map { it.name.lowercase() }
            names shouldBe names.sorted()

            // 5. No products are lost or duplicated (result size matches expected filter count)
            val expectedCount = products.count { product ->
                product.isActive && (selectedCategoryId == null || product.categoryId == selectedCategoryId)
            }
            result.size shouldBe expectedCount
        }
    }
})
