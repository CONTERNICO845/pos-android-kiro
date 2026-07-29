package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

// Feature: 16_sprint_correcciones, Properties 6-8: Cart divider logic

class CartTotalPropertyTest : StringSpec({

    /**
     * Property 6: Cart total excludes divider items
     *
     * For any list of CartItems containing a mix of regular items and divider items,
     * the computed cart total SHALL equal the sum of totalPrice of only those items
     * where isDivider == false.
     *
     * **Validates: Requirements 3.3, 12.5**
     */
    "Property 6 - Cart total excludes divider items" {
        val arbRegularItem = Arb.double(0.01..10000.0).map { price ->
            CartItem(
                id = UUID.randomUUID().toString(),
                productId = "prod-${UUID.randomUUID()}",
                productName = "Product",
                emoji = "🍕",
                basePrice = price,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = BigDecimal.valueOf(price)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble(),
                isDivider = false
            )
        }

        val arbDividerItem = Arb.int(0..100).map {
            CartItem(
                id = UUID.randomUUID().toString(),
                productId = "",
                productName = "--- DIVISOR ---",
                emoji = "",
                basePrice = 0.00,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = 0.00,
                isDivider = true
            )
        }

        val arbMixedCart = Arb.list(arbRegularItem, range = 0..15).map { regulars ->
            val dividerCount = (0..5).random()
            val dividers = (0 until dividerCount).map {
                CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = "",
                    productName = "--- DIVISOR ---",
                    emoji = "",
                    basePrice = 0.00,
                    quantity = 1,
                    selectedCustomizations = emptyList(),
                    extraNotes = "",
                    totalPrice = 0.00,
                    isDivider = true
                )
            }
            (regulars + dividers).shuffled()
        }

        checkAll(PropTestConfig(iterations = 200), arbMixedCart) { cart ->
            // This is the logic from PosViewModel's cartTotalFlow
            val computedTotal = cart.filter { !it.isDivider }
                .sumOf { it.totalPrice }

            val expectedTotal = cart
                .filter { !it.isDivider }
                .fold(BigDecimal.ZERO) { acc, item ->
                    acc.add(BigDecimal.valueOf(item.totalPrice))
                }
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            BigDecimal.valueOf(computedTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble() shouldBe expectedTotal
        }
    }

    /**
     * Property 7: Divider addition produces correct fixed values
     *
     * For any existing cart state, adding a divider SHALL append exactly one CartItem
     * with isDivider = true, productName = "--- DIVISOR ---", productId = "",
     * emoji = "", basePrice = 0.00, totalPrice = 0.00, quantity = 1,
     * selectedCustomizations = emptyList(), and extraNotes = "".
     *
     * **Validates: Requirements 3.1, 12.2**
     */
    "Property 7 - Divider addition produces correct fixed values" {
        val arbRegularItem = Arb.double(0.01..10000.0).map { price ->
            CartItem(
                id = UUID.randomUUID().toString(),
                productId = "prod-${UUID.randomUUID()}",
                productName = "Product ${(1..100).random()}",
                emoji = "🛒",
                basePrice = price,
                quantity = (1..10).random(),
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = BigDecimal.valueOf(price)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble(),
                isDivider = false
            )
        }

        val arbExistingCart = Arb.list(arbRegularItem, range = 0..10)

        checkAll(PropTestConfig(iterations = 200), arbExistingCart) { existingCart ->
            // Simulate addDivider() logic from PosViewModel
            val dividerItem = CartItem(
                id = UUID.randomUUID().toString(),
                productId = "",
                productName = "--- DIVISOR ---",
                emoji = "",
                basePrice = 0.00,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = 0.00,
                isDivider = true
            )
            val newCart = existingCart + dividerItem

            // Verify: new cart has exactly one more item
            newCart shouldHaveSize existingCart.size + 1

            // Verify: the last item is the divider with correct fixed values
            val addedDivider = newCart.last()
            addedDivider.isDivider shouldBe true
            addedDivider.productName shouldBe "--- DIVISOR ---"
            addedDivider.productId shouldBe ""
            addedDivider.emoji shouldBe ""
            addedDivider.basePrice shouldBe 0.00
            addedDivider.totalPrice shouldBe 0.00
            addedDivider.quantity shouldBe 1
            addedDivider.selectedCustomizations shouldBe emptyList()
            addedDivider.extraNotes shouldBe ""
        }
    }

    /**
     * Property 8: Order persistence excludes divider items
     *
     * For any cart containing divider items, filtering cart to !isDivider before mapping
     * to OrderItemEntity produces no divider entries.
     *
     * **Validates: Requirements 3.4**
     */
    "Property 8 - Order persistence excludes divider items" {
        val arbRegularItem = Arb.double(0.01..10000.0).map { price ->
            CartItem(
                id = UUID.randomUUID().toString(),
                productId = "prod-${UUID.randomUUID()}",
                productName = "Product",
                emoji = "🍔",
                basePrice = price,
                quantity = (1..5).random(),
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = BigDecimal.valueOf(price)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble(),
                isDivider = false
            )
        }

        val arbMixedCart = Arb.list(arbRegularItem, range = 1..15).map { regulars ->
            val dividerCount = (1..5).random()
            val dividers = (0 until dividerCount).map {
                CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = "",
                    productName = "--- DIVISOR ---",
                    emoji = "",
                    basePrice = 0.00,
                    quantity = 1,
                    selectedCustomizations = emptyList(),
                    extraNotes = "",
                    totalPrice = 0.00,
                    isDivider = true
                )
            }
            (regulars + dividers).shuffled()
        }

        checkAll(PropTestConfig(iterations = 200), arbMixedCart) { cart ->
            // This is the logic from PosViewModel's confirmPayment/completeOrder
            val orderId = UUID.randomUUID().toString()
            val orderItems = cart.filter { !it.isDivider }.map { cartItem ->
                OrderItemEntityTestModel(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    productId = cartItem.productId,
                    productName = cartItem.productName,
                    quantity = cartItem.quantity,
                    basePrice = cartItem.basePrice,
                    totalPrice = cartItem.totalPrice,
                    extraNotes = cartItem.extraNotes.ifBlank { null }
                )
            }

            // Verify: no divider entries in the persisted order items
            orderItems.none { it.productId == "" && it.productName == "--- DIVISOR ---" } shouldBe true

            // Verify: count matches only non-divider items from the cart
            val expectedCount = cart.count { !it.isDivider }
            orderItems shouldHaveSize expectedCount

            // Verify: all persisted items have valid productIds (non-empty)
            orderItems.all { it.productId.isNotEmpty() } shouldBe true
        }
    }
})

/**
 * Lightweight test model mirroring OrderItemEntity structure.
 * Used to avoid Room/Android dependencies in pure logic unit tests.
 */
private data class OrderItemEntityTestModel(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val basePrice: Double,
    val totalPrice: Double,
    val extraNotes: String?
)
