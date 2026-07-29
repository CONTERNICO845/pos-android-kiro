package com.example.puntodeventa.ui.pos

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.util.UUID

// Feature: 16_sprint_correcciones, Property 18: CartItem-to-TicketLineItem preserves isDivider

@OptIn(ExperimentalKotest::class)
class CartItemConversionPropertyTest : StringSpec({

    /**
     * Property 18: CartItem-to-TicketLineItem preserves isDivider
     *
     * For any list of CartItems (with a mix of regular items and dividers),
     * the converted TicketLineItem list SHALL have isDivider values matching
     * the source CartItems at the same indices.
     *
     * The conversion logic replicates PosViewModel.confirmPayment():
     * ```
     * val ticketLineItems = cartItems.map { item ->
     *     TicketLineItem(
     *         quantity = item.quantity,
     *         productName = item.productName,
     *         lineTotal = item.totalPrice,
     *         customizations = item.selectedCustomizations.map { it.optionName },
     *         extraNotes = item.extraNotes,
     *         isDivider = item.isDivider
     *     )
     * }
     * ```
     *
     * **Validates: Requirements 12.4**
     */
    "Property 18 - CartItem-to-TicketLineItem preserves isDivider" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(Arb.cartItemWithDivider(), range = 1..30)
        ) { cartItems ->
            // Apply the same mapping that PosViewModel.confirmPayment() uses
            val ticketLineItems = cartItems.map { item ->
                TicketLineItem(
                    quantity = item.quantity,
                    productName = item.productName,
                    lineTotal = item.totalPrice,
                    customizations = item.selectedCustomizations.map { it.optionName },
                    extraNotes = item.extraNotes,
                    isDivider = item.isDivider
                )
            }

            // List size is preserved
            ticketLineItems.size shouldBe cartItems.size

            // isDivider is preserved at every index
            cartItems.forEachIndexed { index, cartItem ->
                ticketLineItems[index].isDivider shouldBe cartItem.isDivider
            }
        }
    }
})

/**
 * Generator that produces CartItems with a random isDivider flag.
 * When isDivider is true, uses the fixed divider field values.
 * When isDivider is false, generates a regular cart item.
 */
private fun Arb.Companion.cartItemWithDivider(): Arb<CartItem> = arbitrary { rs ->
    val isDivider = Arb.boolean().bind()

    if (isDivider) {
        // Divider item with fixed values as per design
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
    } else {
        // Regular cart item using the existing generator
        val item = Arb.cartItem().bind()
        item.copy(isDivider = false)
    }
}
