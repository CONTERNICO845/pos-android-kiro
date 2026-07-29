package com.example.puntodeventa.ui.stats

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll

/**
 * Property-based tests for StatsFormatters.
 *
 * Property 8: Currency formatting
 * Property 9: Integer count formatting
 * Property 11: Product sale row formatting
 * Property 12: Order row customer name display
 */
class FormattersPropertyTest : StringSpec({

    /**
     * Property 8: Currency formatting always produces "$" prefix and exactly 2 decimal places.
     */
    "Property 8 — formatCurrency always starts with $ and has exactly 2 decimal places" {
        forAll(PropTestConfig(iterations = 100), Arb.double(0.0..999_999.99)) { amount ->
            val result = StatsFormatters.formatCurrency(amount)
            result.startsWith("$") &&
                result.substringAfter(".").length == 2
        }
    }

    /**
     * Property 9: Integer count formatting produces no decimal point.
     */
    "Property 9 — formatCount never contains a decimal point" {
        forAll(PropTestConfig(iterations = 100), Arb.int(0..1_000_000)) { count ->
            val result = StatsFormatters.formatCount(count)
            !result.contains(".")
        }
    }

    /**
     * Property 11: formatQuantitySold always ends with " vendidos".
     */
    "Property 11 — formatQuantitySold ends with ' vendidos'" {
        forAll(PropTestConfig(iterations = 100), Arb.int(0..10_000)) { qty ->
            val result = StatsFormatters.formatQuantitySold(qty)
            result.endsWith(" vendidos") && result.startsWith("$qty")
        }
    }

    /**
     * Property 12: displayCustomerName returns "Cliente anónimo" for null/blank,
     * original name otherwise.
     */
    "Property 12 — displayCustomerName returns 'Cliente anónimo' for null" {
        StatsFormatters.displayCustomerName(null) == "Cliente anónimo"
    }

    "Property 12b — displayCustomerName returns 'Cliente anónimo' for blank strings" {
        forAll(PropTestConfig(iterations = 50), Arb.string(0..5)) { s ->
            val input = if (s.isBlank()) s else return@forAll true
            StatsFormatters.displayCustomerName(input) == "Cliente anónimo"
        }
    }

    "Property 12c — displayCustomerName returns original name for non-blank" {
        forAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..30).let { arb ->
                // Filter to non-blank strings
                object : Arb<String>() {
                    override fun edgecase(rs: io.kotest.property.RandomSource) = arb.edgecase(rs)?.takeIf { it.isNotBlank() }
                    override fun sample(rs: io.kotest.property.RandomSource): io.kotest.property.Sample<String> {
                        var s = arb.sample(rs)
                        while (s.value.isBlank()) s = arb.sample(rs)
                        return s
                    }
                }
            }
        ) { name ->
            StatsFormatters.displayCustomerName(name) == name
        }
    }

    /**
     * formatOrderTime produces a "HH:mm" pattern (5 chars, colon at position 2).
     */
    "formatOrderTime produces HH:mm pattern" {
        forAll(PropTestConfig(iterations = 100), Arb.long(1_000_000_000_000L..1_900_000_000_000L)) { ts ->
            val result = StatsFormatters.formatOrderTime(ts)
            result.length == 5 && result[2] == ':'
        }
    }
})
