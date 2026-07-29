package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
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
 * Preservation property tests for TicketFormatter.
 *
 * These tests lock existing correct behaviors that MUST remain unchanged after the fix.
 * They are expected to PASS on the unfixed code.
 *
 * **Validates: Requirements 3.3, 3.4, 3.5, 3.6, 3.7, 3.10**
 */
class TicketFormatterPreservationTest : FunSpec({

    // -- Generators --

    val arbAmount = Arb.double(0.01..99999.99).map { amount ->
        BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    val arbQuantity = Arb.int(1..100)

    val arbProductName = Arb.string(minSize = 1, maxSize = 30).map { name ->
        // Ensure printable characters only (no newlines or control chars)
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

    fun arbLineItemWithName(name: String, qty: Int) = TicketLineItem(
        quantity = qty,
        productName = name,
        lineTotal = (10..500).random().toDouble()
    )

    // -- Property Tests --

    /**
     * Property 5: Tax Calculation Invariant
     *
     * For any totalAmount in (0.01..99999.99):
     * calculateSubtotal(t) + calculateIva(t) == t (to 2 decimal places)
     *
     * **Validates: Requirements 3.4, 3.5**
     */
    test("Tax Calculation Invariant: calculateSubtotal + calculateIva == total") {
        checkAll(PropTestConfig(iterations = 100), arbAmount) { total ->
            val subtotal = TicketFormatter.calculateSubtotal(total)
            val iva = TicketFormatter.calculateIva(total)

            val sum = BigDecimal(subtotal)
                .add(BigDecimal(iva))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            val expectedTotal = BigDecimal(total)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            sum shouldBeExactly expectedTotal
        }
    }

    /**
     * Property 6: Currency Format
     *
     * For any amount in (0.01..99999.99):
     * formatCurrency(amount) matches the pattern \$\d+\.\d{2}
     *
     * **Validates: Requirements 3.5**
     */
    test("Currency Format: formatCurrency matches dollar pattern") {
        checkAll(PropTestConfig(iterations = 100), arbAmount) { amount ->
            val formatted = TicketFormatter.formatCurrency(amount)
            formatted shouldMatch Regex("""\$\d+\.\d{2}""")
        }
    }

    /**
     * Property 7: Nombre Prefix Preserved
     *
     * For any customer name and items: "Nombre: {name}" appears in both
     * formatClientTicket() and formatInternalTicket()
     *
     * **Validates: Requirements 3.3**
     */
    test("Nombre Prefix Preserved: both tickets contain Nombre line") {
        checkAll(
            PropTestConfig(iterations = 100),
            arbCustomerName,
            Arb.list(arbLineItem, 1..5)
        ) { customerName, items ->
            val totalAmount = items.sumOf { it.lineTotal }
            val roundedTotal = BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()

            val clientTicket = TicketFormatter.formatClientTicket(
                ticketId = "TEST-001",
                dateTime = "01/06/2025 14:00:00",
                customerName = customerName,
                paymentStatus = "Pagado",
                items = items,
                totalAmount = if (roundedTotal > 0) roundedTotal else 1.00
            )

            val internalTicket = TicketFormatter.formatInternalTicket(
                ticketId = "TEST-001",
                dateTime = "01/06/2025 14:00:00",
                customerName = customerName,
                paymentStatus = "Pagado",
                items = items
            )

            clientTicket shouldContain "Nombre: $customerName"
            internalTicket shouldContain "Nombre: $customerName"
        }
    }

    /**
     * Property 8: 48-char Separator Width
     *
     * For any items: all separator lines (lines composed entirely of dashes) are exactly 48 chars.
     *
     * **Validates: Requirements 3.10**
     */
    test("48-char Separator Width: all separator lines are 48 chars") {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(arbLineItem, 1..10)
        ) { items ->
            val totalAmount = items.sumOf { it.lineTotal }
            val roundedTotal = BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()

            val clientTicket = TicketFormatter.formatClientTicket(
                ticketId = "TEST-002",
                dateTime = "01/06/2025 14:00:00",
                customerName = "TestCliente",
                paymentStatus = "Pagado",
                items = items,
                totalAmount = if (roundedTotal > 0) roundedTotal else 1.00
            )

            val internalTicket = TicketFormatter.formatInternalTicket(
                ticketId = "TEST-002",
                dateTime = "01/06/2025 14:00:00",
                customerName = "TestCliente",
                paymentStatus = "Pagado",
                items = items
            )

            // Check client ticket separators
            clientTicket.lines()
                .filter { line -> line.isNotEmpty() && line.all { it == '-' } }
                .forEach { separatorLine ->
                    separatorLine.length shouldBe 48
                }

            // Check internal ticket separators
            internalTicket.lines()
                .filter { line -> line.isNotEmpty() && line.all { it == '-' } }
                .forEach { separatorLine ->
                    separatorLine.length shouldBe 48
                }
        }
    }

    /**
     * Property 9: Internal Ticket No Dollar Signs in Items
     *
     * For any items: formatInternalTicket() item lines (after the header section)
     * contain no "$" character.
     *
     * **Validates: Requirements 3.7**
     */
    test("Internal Ticket No Dollar Signs in Items: item lines have no dollar sign") {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(arbLineItem, 1..10)
        ) { items ->
            val internalTicket = TicketFormatter.formatInternalTicket(
                ticketId = "TEST-003",
                dateTime = "01/06/2025 14:00:00",
                customerName = "TestCliente",
                paymentStatus = "Pagado",
                items = items
            )

            val lines = internalTicket.lines()
            // Find the item section: lines between the column header and separator/total
            val headerLineIdx = lines.indexOfFirst { it.contains("CANT") && it.contains("DESCRIPCION") }
            val totalLineIdx = lines.indexOfFirst { it.contains("Total:") && it.contains("Artículos") }

            if (headerLineIdx >= 0 && totalLineIdx > headerLineIdx) {
                // Item lines are between header+1 and totalLine (exclusive of separators)
                for (i in (headerLineIdx + 1) until totalLineIdx) {
                    val line = lines[i]
                    // Skip separator lines
                    if (line.isNotEmpty() && line.all { it == '-' }) continue
                    line shouldNotContain "$"
                }
            }
        }
    }

    /**
     * Property 10: Article Count = Sum of Quantities
     *
     * For any items: the internal ticket contains "Total: {N} Artículos"
     * where N = items.sumOf { it.quantity }
     *
     * **Validates: Requirements 3.6**
     */
    test("Article Count equals Sum of Quantities") {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(arbLineItem, 1..10)
        ) { items ->
            val internalTicket = TicketFormatter.formatInternalTicket(
                ticketId = "TEST-004",
                dateTime = "01/06/2025 14:00:00",
                customerName = "TestCliente",
                paymentStatus = "Pagado",
                items = items
            )

            val expectedCount = items.sumOf { it.quantity }
            internalTicket shouldContain "Total: $expectedCount Artículos"
        }
    }
})
