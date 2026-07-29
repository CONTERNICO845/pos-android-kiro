package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

/**
 * Property-based tests for TicketFormatter divider handling and line width.
 *
 * Feature: 16_sprint_correcciones
 * Properties 9, 10, 11
 */

/**
 * Generates a TicketLineItem that is a divider (isDivider = true).
 */
private fun Arb.Companion.dividerLineItem(): Arb<TicketLineItem> = arbitrary {
    TicketLineItem(
        quantity = 0,
        productName = "--- DIVISOR ---",
        lineTotal = 0.0,
        customizations = emptyList(),
        extraNotes = "",
        isDivider = true
    )
}

/**
 * Generates a regular (non-divider) TicketLineItem using the existing cartItem-based generator pattern.
 */
private fun Arb.Companion.regularLineItem(): Arb<TicketLineItem> = arbitrary {
    val quantity = Arb.int(1..99).bind()
    val productName = buildString {
        val len = Arb.int(1..30).bind()
        val chars = (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf(' ')).toList()
        repeat(len) { append(chars[it % chars.size]) }
    }
    val lineTotal = Arb.int(1..999999).bind() / 100.0
    TicketLineItem(
        quantity = quantity,
        productName = productName.take(30),
        lineTotal = lineTotal,
        customizations = emptyList(),
        extraNotes = "",
        isDivider = false
    )
}

/**
 * Generates a mixed list of TicketLineItems (regular and divider) with at least 1 regular and 1 divider.
 * Dividers are placed at random positions to test ordering preservation.
 */
private fun Arb.Companion.mixedItemList(): Arb<List<TicketLineItem>> = arbitrary { rs ->
    val size = Arb.int(2..15).bind()
    val items = mutableListOf<TicketLineItem>()
    var hasDivider = false
    var hasRegular = false

    for (i in 0 until size) {
        val makeDivider = Arb.boolean().bind()
        if (makeDivider) {
            items.add(Arb.dividerLineItem().bind())
            hasDivider = true
        } else {
            items.add(Arb.regularLineItem().bind())
            hasRegular = true
        }
    }

    // Ensure at least one of each type
    if (!hasDivider) {
        val pos = rs.random.nextInt(items.size + 1)
        items.add(pos, Arb.dividerLineItem().bind())
    }
    if (!hasRegular) {
        val pos = rs.random.nextInt(items.size + 1)
        items.add(pos, Arb.regularLineItem().bind())
    }

    items.toList()
}

class TicketFormatterDividerPropertyTest : StringSpec({

    /**
     * Property 9: TicketFormatter renders dividers as 48-dash lines and excludes from totals
     *
     * For any list of TicketLineItems containing items with isDivider = true, the formatted
     * ticket output SHALL contain a line of exactly 48 dash characters at the position of each
     * divider item, SHALL NOT include quantity/name/price/customizations/notes for that item,
     * and SHALL exclude divider items from article count and financial total calculations.
     *
     * **Validates: Requirements 4.1, 4.2, 4.3, 12.6**
     */
    "Property 9 - TicketFormatter renders dividers as 48-dash lines and excludes from totals" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.mixedItemList()
        ) { items ->
            val totalAmount = items.filter { !it.isDivider }.sumOf { it.lineTotal }
            val dividerCount = items.count { it.isDivider }
            val expectedArticleCount = items.filter { !it.isDivider }.sumOf { it.quantity }

            // Test on client ticket
            val clientOutput = TicketFormatter.formatClientTicket(
                ticketId = "T001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Test",
                paymentStatus = "Pagado",
                items = items,
                totalAmount = totalAmount,
                cashReceived = 0.0,
                change = 0.0
            )

            val clientLines = clientOutput.split("\n")
            val dashLine = "-".repeat(48)

            // Count divider lines in the items section (excluding the separator lines before/after items)
            // The separator lines are also 48 dashes but appear in header/footer context.
            // Divider lines appear in the items section — between the column header and the separator after items.
            val columnHeaderIdx = clientLines.indexOfFirst { it.contains("CANT") && it.contains("DESCRIPCION") && it.contains("IMPORTE") }
            val itemSectionLines = clientLines.drop(columnHeaderIdx + 1)

            // In the client ticket, the first separator after items marks end of item section.
            // Count dash lines within the item section.
            var dashLinesInItemSection = 0
            for (line in itemSectionLines) {
                if (line == dashLine) {
                    dashLinesInItemSection++
                    // The first dash line that is NOT a divider is the section separator.
                    // After all dividers are accounted for, the next dash line is the separator.
                    if (dashLinesInItemSection > dividerCount) break
                }
            }
            // There should be at least dividerCount dash lines in the item section
            dashLinesInItemSection shouldBeGreaterThanOrEqual dividerCount

            // Test on internal ticket — verify article count excludes dividers
            val internalOutput = TicketFormatter.formatInternalTicket(
                ticketId = "T001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Test",
                paymentStatus = "Pagado",
                items = items
            )

            val internalLines = internalOutput.split("\n")
            val articleCountLine = internalLines.find { it.startsWith("Total:") && it.contains("Artículos") }
            articleCountLine shouldBe "Total: $expectedArticleCount Artículos"

            // Verify divider items are NOT rendered with product info (no quantity/name/price for dividers)
            // Each divider should produce only a dash line, not a formatted product row
            for (item in items.filter { it.isDivider }) {
                // The sentinel name should NOT appear as a formatted product row
                val formattedDividerAsProduct = item.quantity.toString().padEnd(5) + item.productName.take(30).padEnd(30)
                clientOutput.contains(formattedDividerAsProduct) shouldBe false
            }
        }
    }

    /**
     * Property 10: Divider position preserved in ticket output
     *
     * For any ordered list of TicketLineItems (regular and divider), the relative order of
     * all items in the formatted output SHALL match the relative order of the input list.
     *
     * **Validates: Requirements 4.4**
     */
    "Property 10 - Divider position preserved in ticket output" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.mixedItemList()
        ) { items ->
            val totalAmount = items.filter { !it.isDivider }.sumOf { it.lineTotal }
            val dashLine = "-".repeat(48)

            // Test on client ticket
            val clientOutput = TicketFormatter.formatClientTicket(
                ticketId = "T001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Test",
                paymentStatus = "Pagado",
                items = items,
                totalAmount = totalAmount,
                cashReceived = 0.0,
                change = 0.0
            )

            val clientLines = clientOutput.split("\n")

            // Find where the items section starts (after column header line)
            val columnHeaderIdx = clientLines.indexOfFirst { it.contains("CANT") && it.contains("DESCRIPCION") && it.contains("IMPORTE") }

            // Extract markers from the formatted output in order:
            // - For dividers: a line of exactly 48 dashes
            // - For regular items: a line starting with quantity (padded to 5 chars)
            // We track order by walking through the output lines after the column header.
            val outputOrder = mutableListOf<Boolean>() // true = divider, false = regular item
            var dividersSeen = 0
            val totalDividers = items.count { it.isDivider }

            for (i in (columnHeaderIdx + 1) until clientLines.size) {
                val line = clientLines[i]
                if (line == dashLine) {
                    if (dividersSeen < totalDividers) {
                        outputOrder.add(true)
                        dividersSeen++
                    } else {
                        // This is the section separator, stop
                        break
                    }
                } else if (line.length >= 5 && line.substring(0, 5).trimEnd().toIntOrNull() != null) {
                    // This is a product line (starts with quantity number)
                    outputOrder.add(false)
                }
            }

            // Build expected order from input
            val expectedOrder = items.map { it.isDivider }

            // The output order should match the input order
            outputOrder shouldBe expectedOrder
        }
    }

    /**
     * Property 11: Ticket line width is exactly 48 characters
     *
     * For any product item with any valid productName (1-30 chars) and any valid quantity (1-99)
     * and any valid lineTotal, the formatted line SHALL be exactly 48 characters wide
     * (CANT 5 + DESCRIPCION 30 + IMPORTE 13).
     *
     * **Validates: Requirements 5.3**
     */
    "Property 11 - Ticket line width is exactly 48 characters" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.regularLineItem()
        ) { item ->
            // Test client ticket - product lines should be 48 chars
            val clientOutput = TicketFormatter.formatClientTicket(
                ticketId = "T001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Test",
                paymentStatus = "Pagado",
                items = listOf(item),
                totalAmount = item.lineTotal,
                cashReceived = 0.0,
                change = 0.0
            )

            val clientLines = clientOutput.split("\n")
            val columnHeaderIdx = clientLines.indexOfFirst { it.contains("CANT") && it.contains("DESCRIPCION") && it.contains("IMPORTE") }

            // The product line is the first non-separator line after the column header
            val productLine = clientLines[columnHeaderIdx + 1]
            productLine.length shouldBe 48

            // Test internal ticket - product lines should also be 48 chars
            val internalOutput = TicketFormatter.formatInternalTicket(
                ticketId = "T001",
                dateTime = "01/01/2025 12:00:00",
                customerName = "Test",
                paymentStatus = "Pagado",
                items = listOf(item)
            )

            val internalLines = internalOutput.split("\n")
            val internalColumnHeaderIdx = internalLines.indexOfFirst { it.contains("CANT") && it.contains("DESCRIPCION") && it.contains("IMPORTE") }
            // Skip the separator line after column header in internal ticket
            val internalProductLine = internalLines[internalColumnHeaderIdx + 2]
            internalProductLine.length shouldBe 48
        }
    }
})
