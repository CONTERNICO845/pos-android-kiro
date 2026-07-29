package com.example.puntodeventa.ui.configuration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll

/**
 * PBT-04: Price format always starts with "$" and has exactly two decimal places.
 *
 * Validates: AC-06.3, Property 11
 */
class ProductCardFormatPriceTest : StringSpec({

    "PBT-04: formatPrice always starts with dollar sign and has exactly two decimal places" {
        checkAll(Arb.double(min = 0.0, max = 1_000_000.0)) { price ->
            val result = formatPrice(price)
            // Must start with "$"
            val startsWithDollar = result.startsWith("$")
            // Must match pattern: $<digits>.<2 digits>
            val matchesTwoDecimalPlaces = result.matches(Regex("""^\$\d+\.\d{2}$"""))
            startsWithDollar && matchesTwoDecimalPlaces
        }
    }
})
