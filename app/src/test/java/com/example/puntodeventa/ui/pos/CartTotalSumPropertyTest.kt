package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

// Feature: pos-main-screen, Property 4: Cart Total is Sum of Row Prices

class CartTotalSumPropertyTest : StringSpec({

    /**
     * Property 4: Cart Total is Sum of Row Prices
     *
     * For any list of cart items, calculateCartTotal returns the sum of all
     * CartItem.totalPrice values, rounded to 2 decimal places with HALF_UP.
     *
     * **Validates: Requirements 5.3, 10.5**
     */
    "Property 4 - calculateCartTotal returns the sum of all CartItem totalPrice values" {
        val arbCartItem = Arb.double(0.0..100000.0).map { totalPrice ->
            CartItem(
                id = "id",
                productId = "prod",
                productName = "Product",
                emoji = "🛒",
                basePrice = 10.0,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = BigDecimal.valueOf(totalPrice)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
            )
        }

        checkAll(
            PropTestConfig(iterations = 200),
            Arb.list(arbCartItem, range = 0..20)
        ) { items ->
            val actual = calculateCartTotal(items)

            val expected = items.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(BigDecimal.valueOf(item.totalPrice))
            }.setScale(2, RoundingMode.HALF_UP).toDouble()

            actual shouldBe expected
        }
    }
})
