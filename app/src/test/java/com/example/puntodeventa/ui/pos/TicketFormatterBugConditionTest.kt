package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Bug condition exploration test for ticket formatting defects.
 *
 * These tests encode the EXPECTED (correct) behavior and are run against the CURRENT (broken) code.
 * They are EXPECTED TO FAIL — failure confirms the bugs exist.
 *
 * **Validates: Requirements 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10**
 */
class TicketFormatterBugConditionTest : FunSpec({

    // -- Generators --

    val arbProductName = Arb.string(minSize = 1, maxSize = 30).map { name ->
        name.filter { it in ' '..'~' || it in 'À'..'ÿ' }.ifEmpty { "Producto" }
    }

    val arbCustomerName = Arb.string(minSize = 1, maxSize = 40).map { name ->
        name.filter { it in ' '..'~' || it in 'À'..'ÿ' }.ifEmpty { "Cliente" }
    }

    val arbLineItem = Arb.double(0.01..9999.99).map { price ->
        val roundedPrice = BigDecimal(price).setScale(2, RoundingMode.HALF_UP).toDouble()
        TicketLineItem(
            quantity = (1..20).random(),
            productName = listOf("Taco", "Burrito", "Quesadilla", "Agua", "Refresco").random(),
            lineTotal = roundedPrice
        )
    }

    // -- Bug Condition Exploration Tests --

    /**
     * Property 1: Client ticket has no "Fecha:" or "Estado:" prefixes
     *
     * The client ticket should output date-time and payment status directly
     * without "Fecha:" or "Estado:" prefixes.
     *
     * **Validates: Requirements 1.3, 1.4**
     */
    test("Feature: 13_ticket_printing_fixes, Property 1: Client ticket has no Fecha: or Estado: prefixes") {
        checkAll(
            PropTestConfig(iterations = 100),
            arbCustomerName,
            Arb.list(arbLineItem, 1..5)
        ) { customerName, items ->
            val totalAmount = items.sumOf { it.lineTotal }
            val roundedTotal = BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/06/2025 14:30:00",
                customerName = customerName,
                paymentStatus = "Pagado",
                items = items,
                totalAmount = if (roundedTotal > 0) roundedTotal else 1.00
            )
            output shouldNotContain "Fecha:"
            output shouldNotContain "Estado:"
        }
    }

    /**
     * Property 1: Client ticket has no leading/trailing separators
     *
     * The client ticket should NOT start with a separator line before "LOS TACOS"
     * and should NOT end with a separator line after the footer.
     *
     * **Validates: Requirements 1.5, 1.6**
     */
    test("Feature: 13_ticket_printing_fixes, Property 1: Client ticket has no leading/trailing separators") {
        checkAll(
            PropTestConfig(iterations = 100),
            arbCustomerName,
            Arb.list(arbLineItem, 1..5)
        ) { customerName, items ->
            val totalAmount = items.sumOf { it.lineTotal }
            val roundedTotal = BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/06/2025 14:30:00",
                customerName = customerName,
                paymentStatus = "Pagado",
                items = items,
                totalAmount = if (roundedTotal > 0) roundedTotal else 1.00
            )
            val lines = output.lines()
            // First non-blank line should NOT be a separator (all dashes)
            val firstLine = lines.first { it.isNotBlank() }
            (firstLine.all { it == '-' }) shouldBe false
            // Last non-blank line should NOT be a separator
            val lastLine = lines.last { it.isNotBlank() }
            (lastLine.all { it == '-' }) shouldBe false
        }
    }

    /**
     * Property 1: Internal ticket column header contains IMPORTE
     *
     * The internal ticket column header should contain "IMPORTE" to match
     * the full header format: "CANT  DESCRIPCION                         IMPORTE"
     *
     * **Validates: Requirements 1.7**
     */
    test("Feature: 13_ticket_printing_fixes, Property 1: Internal ticket column header contains IMPORTE") {
        checkAll(
            PropTestConfig(iterations = 100),
            arbCustomerName,
            Arb.list(arbLineItem, 1..5)
        ) { customerName, items ->
            val output = TicketFormatter.formatInternalTicket(
                ticketId = "001",
                dateTime = "01/06/2025 14:30:00",
                customerName = customerName,
                paymentStatus = "Pagado",
                items = items
            )
            output shouldContain "IMPORTE"
        }
    }
})
