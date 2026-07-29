package com.example.puntodeventa.ui.pos

/**
 * Represents a selected customization option for a cart item.
 *
 * @property optionId Unique identifier of the customization option
 * @property optionName Display name of the customization option
 * @property extraPrice Additional price for this customization (≥ 0.00)
 */
data class SelectedCustomization(
    val optionId: String,
    val optionName: String,
    val extraPrice: Double
)

/**
 * In-memory representation of a product added to the current order cart.
 * Each addition creates a new line item with a unique [id], even if the same
 * product configuration already exists in the cart.
 *
 * @property id UUID unique per line item
 * @property productId Identifier of the product from the catalog
 * @property productName Display name of the product
 * @property emoji Product emoji (max 8 characters)
 * @property basePrice Base price of the product (≥ 0.00)
 * @property quantity Number of units (1..99)
 * @property selectedCustomizations List of customization options selected for this item
 * @property extraNotes Additional notes from the cashier (0..280 characters)
 * @property totalPrice Calculated as (basePrice + Σ extraPrices) × quantity, rounded to 2 decimal places
 */
data class CartItem(
    val id: String,
    val productId: String,
    val productName: String,
    val emoji: String,
    val basePrice: Double,
    val quantity: Int,
    val selectedCustomizations: List<SelectedCustomization>,
    val extraNotes: String,
    val totalPrice: Double,
    val isDivider: Boolean = false
)
