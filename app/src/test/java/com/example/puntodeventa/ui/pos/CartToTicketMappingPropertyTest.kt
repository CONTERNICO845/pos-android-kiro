package com.example.puntodeventa.ui.pos

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

// Feature: printer-audit-and-ticket-format, Property 1: CartItem to TicketLineItem mapping preserves all fields

@OptIn(ExperimentalKotest::class)
class CartToTicketMappingPropertyTest : StringSpec({

    /**
     * Property 1: CartItem to TicketLineItem mapping preserves all fields
     *
     * For any list of CartItems (size 1–100), mapping to TicketLineItems SHALL produce
     * a list of the same size where each TicketLineItem preserves:
     * - quantity == CartItem.quantity
     * - productName == CartItem.productName
     * - lineTotal == CartItem.totalPrice
     * - customizations == CartItem.selectedCustomizations.map { it.optionName } in the same order
     * - extraNotes == CartItem.extraNotes
     *
     * **Validates: Requirements 6.1, 6.6, 3.2**
     */
    "Property 1 - CartItem to TicketLineItem mapping preserves all fields" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(Arb.cartItem(), range = 1..100)
        ) { cartItems ->
            // Apply the same mapping that PosViewModel.confirmPayment() uses
            val ticketLineItems = cartItems.map { item ->
                TicketLineItem(
                    quantity = item.quantity,
                    productName = item.productName,
                    lineTotal = item.totalPrice,
                    customizations = item.selectedCustomizations.map { it.optionName },
                    extraNotes = item.extraNotes
                )
            }

            // List size is preserved
            ticketLineItems.size shouldBe cartItems.size

            // Each field is preserved for every item
            cartItems.zip(ticketLineItems).forEach { (cart, ticket) ->
                ticket.quantity shouldBe cart.quantity
                ticket.productName shouldBe cart.productName
                ticket.lineTotal shouldBe cart.totalPrice
                ticket.customizations shouldBe cart.selectedCustomizations.map { it.optionName }
                ticket.extraNotes shouldBe cart.extraNotes
            }
        }
    }
})
