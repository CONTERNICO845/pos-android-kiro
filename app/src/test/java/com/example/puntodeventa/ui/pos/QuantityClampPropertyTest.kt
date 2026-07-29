package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.math.max
import kotlin.math.min

// Feature: pos-main-screen, Property 10: Quantity Clamped Within [1, 99]

class QuantityClampPropertyTest : StringSpec({

    /**
     * Property 10: Quantity Clamped Within [1, 99]
     *
     * For any current quantity in [1, 99]:
     * - clampQuantity(current, +1) == min(current + 1, 99)
     * - clampQuantity(current, -1) == max(current - 1, 1)
     * - For any delta, the result is always in [1, 99]
     *
     * **Validates: Requirements 8.2, 8.3, 8.4, 8.5**
     */
    "Property 10 - clampQuantity(current, +1) equals min(current + 1, 99)" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.int(1..99)
        ) { current ->
            val actual = clampQuantity(current, +1)
            val expected = min(current + 1, 99)
            actual shouldBe expected
        }
    }

    "Property 10 - clampQuantity(current, -1) equals max(current - 1, 1)" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.int(1..99)
        ) { current ->
            val actual = clampQuantity(current, -1)
            val expected = max(current - 1, 1)
            actual shouldBe expected
        }
    }

    "Property 10 - clampQuantity(current, delta) is always in [1, 99] for any delta" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.int(1..99),
            Arb.int(-200..200)
        ) { current, delta ->
            val actual = clampQuantity(current, delta)
            actual shouldBeInRange 1..99
        }
    }
})
