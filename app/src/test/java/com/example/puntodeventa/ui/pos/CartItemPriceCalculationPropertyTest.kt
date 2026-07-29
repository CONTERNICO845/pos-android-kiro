package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

// Feature: pos-main-screen, Property 3: Cart Item Price Calculation

class CartItemPriceCalculationPropertyTest : StringSpec({

    /**
     * Property 3: Cart Item Price Calculation
     *
     * For any basePrice ≥ 0, extraPrices list of non-negative Doubles, and quantity in [1, 99],
     * calculateItemTotal returns round((basePrice + sum(extraPrices)) × quantity, 2)
     *
     * **Validates: Requirements 5.2, 9.2**
     */
    "Property 3 - calculateItemTotal returns round((basePrice + sum(extraPrices)) * quantity, 2)" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.double(0.0..10000.0),
            Arb.list(Arb.double(0.0..500.0), range = 0..10),
            Arb.int(1..99)
        ) { basePrice, extraPrices, quantity ->
            val actual = calculateItemTotal(basePrice, extraPrices, quantity)

            val expected = BigDecimal.valueOf(basePrice)
                .add(extraPrices.fold(BigDecimal.ZERO) { acc, price -> acc.add(BigDecimal.valueOf(price)) })
                .multiply(BigDecimal.valueOf(quantity.toLong()))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            actual shouldBe expected
        }
    }
})
