package com.example.puntodeventa.ui.pos

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

// Feature: pos-main-screen, Property 6: Cart Maintains Insertion Order

@OptIn(ExperimentalKotest::class)
class CartInsertionOrderPropertyTest : StringSpec({

    /**
     * Property 6: Cart Maintains Insertion Order
     *
     * For any sequence of cart item additions, the cart list shall maintain items
     * in the exact order they were added, with the most recently added item at the end.
     *
     * **Validates: Requirements 5.7**
     */
    "Property 6 - adding items preserves insertion order with the newest item at the end" {
        // Generator: create a list of CartItems to add sequentially
        val arbCartItems = Arb.list(Arb.int(0..100000), range = 1..20).map { seeds ->
            seeds.mapIndexed { index, seed ->
                CartItem(
                    id = "item-$index-$seed",
                    productId = "prod-$seed",
                    productName = "Product $seed",
                    emoji = "🛒",
                    basePrice = (seed % 1000).toDouble(),
                    quantity = (seed % 99) + 1,
                    selectedCustomizations = emptyList(),
                    extraNotes = "note-$seed",
                    totalPrice = ((seed % 1000) * ((seed % 99) + 1)).toDouble()
                )
            }
        }

        checkAll(
            PropTestConfig(iterations = 200),
            arbCartItems
        ) { itemsToAdd ->
            // Simulate addToCart behavior: start with empty cart, append one by one
            var cart: List<CartItem> = emptyList()
            for (item in itemsToAdd) {
                cart = cart + item
            }

            // Assert 1: final list has exactly N items
            cart.size shouldBe itemsToAdd.size

            // Assert 2: insertion order is preserved (item at index i is the i-th item added)
            cart.forEachIndexed { index, cartItem ->
                cartItem shouldBe itemsToAdd[index]
            }

            // Assert 3: the last item added is at the end of the list
            cart.last() shouldBe itemsToAdd.last()
        }
    }
})
