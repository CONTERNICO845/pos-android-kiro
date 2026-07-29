package com.example.puntodeventa.ui.pos

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

// Feature: printer-audit-and-ticket-format, Property 2: Subtotal + IVA = Total

@OptIn(ExperimentalKotest::class)
class TicketFormatterPropertyTest : StringSpec({

    /**
     * Property 2: Subtotal + IVA = Total arithmetic invariant
     *
     * For any positive Double `totalAmount`, `calculateSubtotal(totalAmount) + calculateIva(totalAmount)`
     * SHALL equal `totalAmount` rounded to 2 decimal places using BigDecimal HALF_UP.
     *
     * The invariant: Subtotal = Total / 1.16 rounded HALF_UP to 2 decimal places,
     * IVA = Total - Subtotal
     *
     * **Validates: Requirements 4.9**
     */
    "Property 2 - Subtotal + IVA = Total arithmetic invariant" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.positiveMoney()
        ) { totalAmount ->
            val subtotal = TicketFormatter.calculateSubtotal(totalAmount)
            val iva = TicketFormatter.calculateIva(totalAmount)

            val sum = BigDecimal(subtotal)
                .setScale(2, RoundingMode.HALF_UP)
                .add(BigDecimal(iva).setScale(2, RoundingMode.HALF_UP))

            val expectedTotal = BigDecimal(totalAmount)
                .setScale(2, RoundingMode.HALF_UP)

            sum shouldBe expectedTotal
        }
    }

    /**
     * Property 3: Payment section rendered when cash payment present
     *
     * For any valid ticket inputs where cashReceived > 0 and paymentStatus is "Pagado",
     * the output of formatClientTicket SHALL contain a line matching "Pago (Efectivo MXN):"
     * with the formatted cashReceived amount, and a line matching "Cambio:" with the change
     * amount equal to max(0, cashReceived - totalAmount) formatted to 2 decimal places.
     *
     * **Validates: Requirements 2.3, 2.4, 4.6, 4.7**
     */
    "Property 3 - Payment section rendered when cash payment present" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.paymentScenario().filter { it.cashReceived > 0.0 && it.paymentStatus == "Pagado" }
        ) { scenario ->
            val change = BigDecimal(scenario.cashReceived)
                .subtract(BigDecimal(scenario.totalAmount))
                .setScale(2, RoundingMode.HALF_UP)
                .coerceAtLeast(BigDecimal.ZERO)
                .toDouble()

            val items = listOf(
                TicketLineItem(
                    quantity = 1,
                    productName = "Producto Test",
                    lineTotal = scenario.totalAmount
                )
            )

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = scenario.paymentStatus,
                items = items,
                totalAmount = scenario.totalAmount,
                cashReceived = scenario.cashReceived,
                change = change
            )

            // Verify "Pago (Efectivo MXN):" line contains the formatted cashReceived
            val expectedCash = TicketFormatter.formatCurrency(scenario.cashReceived)
            output shouldContain "Pago (Efectivo MXN):"
            output shouldContain expectedCash

            // Verify "Cambio:" line contains the correct change amount
            val expectedChange = TicketFormatter.formatCurrency(change)
            output shouldContain "Cambio:"
            output shouldContain expectedChange
        }
    }

    /**
     * Property 4: Payment section omitted when cash payment absent
     *
     * For any valid ticket inputs where cashReceived == 0.0 OR paymentStatus is not "Pagado",
     * the output of formatClientTicket SHALL NOT contain the strings "Pago (Efectivo MXN):"
     * or "Cambio:".
     *
     * **Validates: Requirements 2.5, 4.8**
     */
    "Property 4 - Payment section omitted when cash payment absent" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.paymentScenario().filter { it.cashReceived == 0.0 || it.paymentStatus != "Pagado" }
        ) { scenario ->
            val items = listOf(
                TicketLineItem(
                    quantity = 1,
                    productName = "Producto Test",
                    lineTotal = scenario.totalAmount
                )
            )

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = scenario.paymentStatus,
                items = items,
                totalAmount = scenario.totalAmount,
                cashReceived = scenario.cashReceived,
                change = 0.0
            )

            output shouldNotContain "Pago (Efectivo MXN):"
            output shouldNotContain "Cambio:"
        }
    }

    /**
     * Property 5: Financial section lines are exactly 48 characters and in correct order
     *
     * For any valid ticket inputs, the financial lines (Subtotal, IVA, Total, and optionally
     * Pago/Cambio) in the output of formatClientTicket SHALL each be exactly 48 characters wide,
     * and SHALL appear in the order: Subtotal → IVA → Total → Pago → Cambio
     * (last two only when cashReceived > 0).
     *
     * **Validates: Requirements 4.1, 4.2**
     */
    "Property 5 - Financial section lines are exactly 48 characters and in correct order" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.paymentScenario()
        ) { scenario ->
            val change = if (scenario.cashReceived > 0.0 && scenario.paymentStatus == "Pagado") {
                BigDecimal(scenario.cashReceived)
                    .subtract(BigDecimal(scenario.totalAmount))
                    .setScale(2, RoundingMode.HALF_UP)
                    .coerceAtLeast(BigDecimal.ZERO)
                    .toDouble()
            } else 0.0

            val items = listOf(
                TicketLineItem(
                    quantity = 1,
                    productName = "Producto Test",
                    lineTotal = scenario.totalAmount
                )
            )

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = scenario.paymentStatus,
                items = items,
                totalAmount = scenario.totalAmount,
                cashReceived = scenario.cashReceived,
                change = change
            )

            val lines = output.split("\n")

            // Financial labels to search for (in required order)
            val financialLabels = mutableListOf(
                "Subtotal (antes de IVA):",
                "IVA (16%):",
                "Total:"
            )
            if (scenario.cashReceived > 0.0 && scenario.paymentStatus == "Pagado") {
                financialLabels.add("Pago (Efectivo MXN):")
                financialLabels.add("Cambio:")
            }

            // Find lines containing financial labels
            val financialLines = lines.filter { line ->
                financialLabels.any { label -> line.contains(label) }
            }

            // There should be exactly as many financial lines as labels
            financialLines.size shouldBe financialLabels.size

            // Each financial line must be exactly 48 characters
            financialLines.forEach { line ->
                line.length shouldBe 48
            }

            // Verify correct order: find indices in the output
            val indices = financialLabels.map { label ->
                lines.indexOfFirst { it.contains(label) }
            }

            // All indices must be found (>= 0)
            indices.forEach { idx ->
                (idx >= 0) shouldBe true
            }

            // Indices must be strictly increasing (correct order)
            for (i in 0 until indices.size - 1) {
                (indices[i] < indices[i + 1]) shouldBe true
            }
        }
    }

    /**
     * Property 6: ExtraNotes rendering with correct prefix and wrapping
     *
     * For any TicketLineItem with a non-empty `extraNotes` (after trimming), the formatted output
     * SHALL contain a line starting with "      * Nota: " followed by at most 34 characters of note
     * content. If the note exceeds 34 characters, continuation lines SHALL be indented with 13 spaces
     * and contain at most 35 characters each, with a maximum of 8 continuation lines.
     *
     * **Validates: Requirements 3.3, 3.5, 5.2, 5.3, 5.4**
     */
    "Property 6 - ExtraNotes rendering with correct prefix and wrapping" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.extraNotesString().filter { it.trim().isNotEmpty() }
        ) { extraNotes ->
            val item = TicketLineItem(
                quantity = 1,
                productName = "Producto",
                lineTotal = 50.0,
                customizations = emptyList(),
                extraNotes = extraNotes
            )

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = listOf(item),
                totalAmount = 50.0,
                cashReceived = 0.0,
                change = 0.0
            )

            val lines = output.split("\n")

            // Find the first "* Nota:" line
            val noteLineIndex = lines.indexOfFirst { it.contains("* Nota:") }
            (noteLineIndex >= 0) shouldBe true

            val noteLine = lines[noteLineIndex]

            // First note line must start with the correct prefix
            noteLine.startsWith("      * Nota: ") shouldBe true

            // First note line: prefix (14 chars) + at most 34 chars content = at most 48 chars
            noteLine.length shouldBeLessThanOrEqual 48

            // Check continuation lines: lines after the note line that start with 13 spaces
            val continuationIndent = " ".repeat(13)
            val continuationLines = mutableListOf<String>()
            for (i in (noteLineIndex + 1) until lines.size) {
                val line = lines[i]
                if (line.startsWith(continuationIndent) && !line.startsWith("      * Nota: ") && !line.startsWith("      - ")) {
                    continuationLines.add(line)
                } else {
                    break
                }
            }

            // Each continuation line: 13 indent + at most 35 chars content = at most 48 chars
            continuationLines.forEach { contLine ->
                contLine.length shouldBeLessThanOrEqual 48
            }

            // Maximum 8 continuation lines
            continuationLines.size shouldBeLessThanOrEqual 8
        }
    }

    /**
     * Property 7: Empty extraNotes produces no note line
     *
     * For any TicketLineItem with an empty `extraNotes` (length == 0 or only whitespace),
     * the formatted output SHALL NOT contain "* Nota:".
     *
     * **Validates: Requirements 3.4, 5.5**
     */
    "Property 7 - Empty extraNotes produces no note line" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.of("", " ", "   ", "\t", "\n", "  \t  ", "\n\t\n")
        ) { emptyNotes ->
            val item = TicketLineItem(
                quantity = 1,
                productName = "Producto Test",
                lineTotal = 50.0,
                customizations = emptyList(),
                extraNotes = emptyNotes
            )

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = listOf(item),
                totalAmount = 50.0,
                cashReceived = 0.0,
                change = 0.0
            )

            output shouldNotContain "* Nota:"
        }
    }

    /**
     * Property 8: Customizations rendered with dash prefix before notes
     *
     * For any TicketLineItem with a non-empty customizations list, each customization SHALL
     * appear on its own line prefixed with "      - " with optionName truncated to 40 characters.
     * When the item also has a non-empty extraNotes, all customization lines SHALL appear
     * before the "* Nota:" line in the output.
     *
     * **Validates: Requirements 5.1, 5.2, 6.4**
     */
    "Property 8 - Customizations rendered with dash prefix before notes" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.ticketLineItem().filter {
                it.customizations.isNotEmpty() && it.extraNotes.trim().isNotEmpty()
            }
        ) { item ->
            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = listOf(item),
                totalAmount = item.lineTotal,
                cashReceived = 0.0,
                change = 0.0
            )

            val lines = output.split("\n")

            // Each customization should appear on its own line with "      - " prefix, truncated to 40 chars
            for (customization in item.customizations) {
                val expectedText = "      - ${customization.take(40)}"
                val matchingLine = lines.find { it == expectedText }
                (matchingLine != null) shouldBe true
            }

            // All customization lines ("      - ") must appear BEFORE the "* Nota:" line
            val customizationIndices = lines.mapIndexedNotNull { index, line ->
                if (line.startsWith("      - ")) index else null
            }
            val noteLineIndex = lines.indexOfFirst { it.contains("* Nota:") }

            // Note line must exist (since extraNotes is non-empty after trim)
            (noteLineIndex >= 0) shouldBe true

            // Every customization line must have an index less than the note line index
            for (custIdx in customizationIndices) {
                (custIdx < noteLineIndex) shouldBe true
            }
        }
    }

    /**
     * Property 9: Every product name appears in formatted output
     *
     * For any list of TicketLineItems passed to formatClientTicket, every item's productName
     * (truncated to 30 characters) SHALL appear in the returned string.
     *
     * **Validates: Requirements 6.2**
     */
    "Property 9 - Every product name appears in formatted output" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(Arb.ticketLineItem(), range = 1..10)
        ) { items ->
            val totalAmount = items.sumOf { it.lineTotal }

            val output = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = items,
                totalAmount = totalAmount,
                cashReceived = 0.0,
                change = 0.0
            )

            // Every product name (truncated to 30 chars) must appear in the output
            for (item in items) {
                val truncatedName = item.productName.take(30)
                output shouldContain truncatedName
            }
        }
    }

    /**
     * Property 10: ExtraNotes appears consistently on both ticket types
     *
     * For any TicketLineItem with non-empty `extraNotes`, both `formatClientTicket` and
     * `formatInternalTicket` SHALL contain the extraNotes content rendered with the
     * "* Nota:" prefix format.
     *
     * **Validates: Requirements 3.6, 5.6**
     */
    "Property 10 - ExtraNotes appears consistently on both ticket types" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.ticketLineItem().filter { it.extraNotes.trim().isNotEmpty() }
        ) { item ->
            val items = listOf(item)

            val clientOutput = TicketFormatter.formatClientTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = items,
                totalAmount = item.lineTotal,
                cashReceived = 0.0,
                change = 0.0
            )

            val internalOutput = TicketFormatter.formatInternalTicket(
                ticketId = "001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Cliente",
                paymentStatus = "Pagado",
                items = items
            )

            // Both outputs must contain the "* Nota:" prefix
            clientOutput shouldContain "* Nota:"
            internalOutput shouldContain "* Nota:"

            // Both outputs must contain the extraNotes content (first 34 chars at minimum)
            val expectedContent = item.extraNotes.trim().take(34)
            clientOutput shouldContain expectedContent
            internalOutput shouldContain expectedContent
        }
    }
})
