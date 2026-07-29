package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates the total price of a cart item.
 *
 * Formula: round((basePrice + sum(extraPrices)) × quantity, 2)
 * Uses BigDecimal with HALF_UP rounding to avoid floating-point drift.
 *
 * @param basePrice the base price of the product (≥ 0)
 * @param extraPrices list of extra prices from selected customizations (each ≥ 0)
 * @param quantity the number of units (1..99)
 * @return the total price rounded to 2 decimal places
 */
internal fun calculateItemTotal(basePrice: Double, extraPrices: List<Double>, quantity: Int): Double {
    val unitPrice = BigDecimal.valueOf(basePrice)
        .add(extraPrices.fold(BigDecimal.ZERO) { acc, price -> acc.add(BigDecimal.valueOf(price)) })
    return unitPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}

/**
 * Calculates the total price of all items in the cart.
 *
 * @param items list of cart items
 * @return the sum of all CartItem.totalPrice values
 */
internal fun calculateCartTotal(items: List<CartItem>): Double {
    return items.fold(BigDecimal.ZERO) { acc, item ->
        acc.add(BigDecimal.valueOf(item.totalPrice))
    }.setScale(2, RoundingMode.HALF_UP).toDouble()
}

/**
 * Filters and sorts products by category.
 *
 * - If [selectedCategoryId] is null, returns all active products sorted by name (case-insensitive).
 * - If [selectedCategoryId] is not null, returns only active products matching that category,
 *   sorted by name (case-insensitive).
 *
 * @param products the full list of products
 * @param selectedCategoryId the selected category id, or null for all categories
 * @return filtered and sorted list of active products
 */
internal fun sortAndFilterProducts(products: List<Product>, selectedCategoryId: String?): List<Product> {
    return products
        .filter { it.isActive && (selectedCategoryId == null || it.categoryId == selectedCategoryId) }
        .sortedBy { it.name.lowercase() }
}

/**
 * Builds the tab order for categories.
 *
 * Returns a list starting with `null` (representing the "TODO" tab showing all products),
 * followed by categories sorted alphabetically by name (case-insensitive).
 *
 * @param categories the list of categories from the repository
 * @return ordered list where null is first (TODO tab), then sorted categories
 */
internal fun buildTabOrder(categories: List<Category>): List<Category?> {
    return listOf(null) + categories.sortedBy { it.name.lowercase() }
}

/**
 * Clamps the result of (current + delta) to the range [1, 99].
 *
 * @param current the current quantity value
 * @param delta the change to apply (positive or negative)
 * @return the new quantity clamped to [1, 99]
 */
internal fun clampQuantity(current: Int, delta: Int): Int {
    return (current + delta).coerceIn(1, 99)
}

/**
 * Applies a glow effect when the element is enabled, or dims it when disabled.
 *
 * - Enabled: full opacity (alpha 1.0) with a 6dp shadow using a pill shape.
 * - Disabled: reduced opacity (alpha 0.38) with no shadow.
 *
 * Uses color values from Color.kt (ButtonConfirm) — no hardcoded color literals.
 *
 * @param enabled whether the glow effect should be active
 * @return the modified Modifier chain
 */
fun Modifier.glowWhenEnabled(enabled: Boolean): Modifier = this
    .alpha(if (enabled) 1f else 0.38f)
    .then(
        if (enabled) Modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
        else Modifier
    )
