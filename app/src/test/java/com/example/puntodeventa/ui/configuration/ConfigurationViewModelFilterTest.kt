package com.example.puntodeventa.ui.configuration

import com.example.puntodeventa.data.model.Product
import io.kotest.core.spec.style.PropSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.forAll

// ── Arbitraries ───────────────────────────────────────────────────────────────

/** Generates a single Product with arbitrary field values. */
private val arbProduct: Arb<Product> = arbitrary {
    Product(
        id          = Arb.uuid().bind().toString(),
        emoji       = Arb.string(0..4).bind(),
        name        = Arb.string(0..80).bind(),
        description = Arb.string(0..200).bind(),
        basePrice   = Arb.double(0.0, 1_000_000.0).bind(),
        isActive    = Arb.boolean().bind(),
        categoryId  = Arb.uuid().bind().toString()
    )
}

/** Generates a list of 0–20 Products. */
private val arbProductList: Arb<List<Product>> = Arb.list(arbProduct, 0..20)

/**
 * Generates arbitrary search queries:
 * mix of short, normal, and over-100-char strings to exercise the clamp boundary.
 */
private val arbSearchQuery: Arb<String> = Arb.string(0..150)

// ── Reference implementation (used only in PBT-01) ───────────────────────────

private fun referenceFilter(products: List<Product>, query: String): List<Product> {
    val trimmed = query.trim().take(100)
    return if (trimmed.isBlank()) products
    else products.filter { it.name.contains(trimmed, ignoreCase = true) }
}

// ── Property-Based Tests ──────────────────────────────────────────────────────

/**
 * Property-based tests for the internal pure functions [applyFilter] and [clampQuery]
 * in ConfigurationViewModel.kt.
 */
class ConfigurationViewModelFilterTest : PropSpec({

    /**
     * PBT-01: Filter subset property
     *
     * `applyFilter` output matches the reference implementation for all
     * product lists and search queries.
     *
     * **Validates: AC-04.2, Property 5**
     */
    property("PBT-01: applyFilter output matches reference implementation") {
        forAll(arbProductList, arbSearchQuery) { products, query ->
            applyFilter(products, query) == referenceFilter(products, query)
        }
    }

    /**
     * PBT-02: Filter idempotency
     *
     * Applying the same filter twice yields the same result as applying it once.
     * i.e. applyFilter(applyFilter(products, q), q) == applyFilter(products, q)
     *
     * **Validates: Property 5**
     */
    property("PBT-02: applyFilter is idempotent — applying the same filter twice yields the same result") {
        forAll(arbProductList, arbSearchQuery) { products, query ->
            val once  = applyFilter(products, query)
            val twice = applyFilter(once, query)
            once == twice
        }
    }

    /**
     * PBT-03: Empty query returns full list
     *
     * `applyFilter(products, "")` returns the original product list unchanged.
     *
     * **Validates: AC-04.3**
     */
    property("PBT-03: empty query returns the full product list") {
        forAll(arbProductList) { products ->
            applyFilter(products, "") == products
        }
    }

    /**
     * PBT-07: Search query clamped at 100 characters
     *
     * `clampQuery(query).length <= 100` for all string inputs.
     *
     * **Validates: AC-04.2**
     */
    property("PBT-07: clampQuery always returns a string of at most 100 characters") {
        forAll(arbSearchQuery) { query ->
            clampQuery(query).length <= 100
        }
    }

    /**
     * PBT-08: filteredProducts is always a subset of products
     *
     * Every element returned by `applyFilter` is present in the original list.
     *
     * **Validates: filteredProducts ⊆ products invariant**
     */
    property("PBT-08: every item in applyFilter result is present in the original product list") {
        forAll(arbProductList, arbSearchQuery) { products, query ->
            val filtered = applyFilter(products, query)
            filtered.all { it in products }
        }
    }
})
