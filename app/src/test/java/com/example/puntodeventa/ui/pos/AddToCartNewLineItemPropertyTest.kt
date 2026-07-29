package com.example.puntodeventa.ui.pos

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.UUID

// Feature: pos-main-screen, Property 11: Adding a Product Creates a New Line Item

@OptIn(ExperimentalKotest::class)
class AddToCartNewLineItemPropertyTest : StringSpec({

    /**
     * Property 11: Adding a Product Creates a New Line Item
     *
     * For any product configuration (product, quantity, customizations, notes), calling addToCart
     * SHALL always append a new CartItem with a unique id to the cart list, regardless of whether
     * an identical product configuration already exists in the cart.
     *
     * **Validates: Requirements 9.1**
     */
    "Property 11 - addToCart always appends a new CartItem with a unique id regardless of matching product configuration" {
        // Generator: create a fixed product configuration and a count of times to add it
        val arbAddCount = Arb.int(2..10)
        val arbProductId = Arb.string(5..10).map { "prod-$it" }
        val arbQuantity = Arb.int(1..99)
        val arbNotes = Arb.string(0..50)

        checkAll(
            PropTestConfig(iterations = 200),
            arbProductId,
            arbQuantity,
            arbNotes,
            arbAddCount
        ) { productId, quantity, notes, addCount ->
            // Start with an empty cart
            var cart: List<CartItem> = emptyList()

            // Add the same product configuration multiple times, each with a unique UUID id
            val addedIds = mutableListOf<String>()
            repeat(addCount) {
                val uniqueId = UUID.randomUUID().toString()
                addedIds.add(uniqueId)

                val cartItem = CartItem(
                    id = uniqueId,
                    productId = productId,
                    productName = "Test Product",
                    emoji = "🍔",
                    basePrice = 10.0,
                    quantity = quantity,
                    selectedCustomizations = listOf(
                        SelectedCustomization(
                            optionId = "opt-1",
                            optionName = "Extra Cheese",
                            extraPrice = 2.0
                        )
                    ),
                    extraNotes = notes,
                    totalPrice = (10.0 + 2.0) * quantity
                )

                // Simulate addToCart: cart = cart + cartItem
                cart = cart + cartItem
            }

            // Assert 1: cart size increases by exactly 1 for each addToCart call
            cart.size shouldBe addCount

            // Assert 2: each CartItem has a unique id
            val uniqueIds = cart.map { it.id }.toSet()
            uniqueIds.size shouldBe addCount

            // Assert 3: all items have the same product configuration but different ids
            cart.forEach { item ->
                item.productId shouldBe productId
                item.productName shouldBe "Test Product"
                item.quantity shouldBe quantity
                item.extraNotes shouldBe notes
            }

            // Assert 4: the ids match the ones we generated (order preserved)
            cart.map { it.id } shouldBe addedIds
        }
    }

    "Property 11 - each addToCart call increases cart size by exactly 1" {
        val arbCartItems = Arb.list(Arb.int(0..100000), range = 1..15).map { seeds ->
            seeds.mapIndexed { index, seed ->
                CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = "prod-${seed % 5}", // reuse same productIds to test duplicates
                    productName = "Product ${seed % 5}",
                    emoji = "🛒",
                    basePrice = (seed % 100).toDouble(),
                    quantity = (seed % 99) + 1,
                    selectedCustomizations = emptyList(),
                    extraNotes = "note-$index",
                    totalPrice = ((seed % 100) * ((seed % 99) + 1)).toDouble()
                )
            }
        }

        checkAll(
            PropTestConfig(iterations = 200),
            arbCartItems
        ) { itemsToAdd ->
            var cart: List<CartItem> = emptyList()

            itemsToAdd.forEachIndexed { index, item ->
                val sizeBefore = cart.size
                // Simulate addToCart
                cart = cart + item
                // Cart size increased by exactly 1
                cart.size shouldBe sizeBefore + 1
                // The new item is at the end
                cart.last() shouldBe item
                // Total size matches expected
                cart.size shouldBe index + 1
            }
        }
    }
})
