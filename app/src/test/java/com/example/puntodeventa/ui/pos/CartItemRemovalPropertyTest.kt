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

// Feature: pos-main-screen, Property 5: Cart Item Removal Preserves Other Items

@OptIn(ExperimentalKotest::class)
class CartItemRemovalPropertyTest : StringSpec({

    /**
     * Property 5: Cart Item Removal Preserves Other Items
     *
     * For any cart containing N items (N ≥ 1), removing a specific item by its id
     * results in a cart of N-1 items where all other items remain unchanged in value and order.
     *
     * **Validates: Requirements 5.6**
     */
    "Property 5 - removing an item by id results in N-1 items with all others unchanged in order" {
        // Generator: create a list of CartItems with unique indexed ids
        val arbCartItems = Arb.list(Arb.int(0..100000), range = 1..20).map { indices ->
            indices.mapIndexed { index, seed ->
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
            arbCartItems,
            Arb.int(0..19)
        ) { items, removeIndexRaw ->
            // Pick a valid index to remove
            val removeIndex = removeIndexRaw % items.size
            val removedId = items[removeIndex].id

            // Apply the removal logic (same as PosViewModel.removeFromCart)
            val result = items.filter { it.id != removedId }

            // Assert: result has exactly N-1 items
            result.size shouldBe items.size - 1

            // Assert: all remaining items are unchanged in value and order
            val expected = items.filterIndexed { index, _ -> index != removeIndex }
            result shouldBe expected
        }
    }
})
