package com.example.puntodeventa.ui.pos

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Data class representing a single line item on a ticket.
 */
data class TicketLineItem(
    val quantity: Int,
    val productName: String,
    val lineTotal: Double, // only used for client ticket
    val customizations: List<String> = emptyList(), // display names of selected customization options
    val extraNotes: String = "", // notas adicionales del cajero
    val isDivider: Boolean = false
)

/**
 * Holds the internal ticket text split into three segments so that
 * the printer can apply Double Height exclusively to the items section.
 */
data class InternalTicketSegments(
    val header: String,  // From title through column header + separator
    val items: String,   // Product rows (including customization sub-lines, notes, and divider dashes)
    val footer: String   // "Total: N Artículos" + footer lines
)

/**
 * Pure utility object for ticket formatting, currency display, and tax calculations.
 * No side effects — all functions are deterministic.
 */
object TicketFormatter {

    /**
     * Formats a monetary amount as "$X.XX" with HALF_UP rounding.
     * No thousands separator, leading $ sign, exactly 2 decimal places.
     */
    fun formatCurrency(amount: Double): String {
        val rounded = BigDecimal(amount)
            .setScale(2, RoundingMode.HALF_UP)
        return "$${rounded.toPlainString()}"
    }

    /**
     * Calculates subtotal from total by dividing by 1.16, rounded HALF_UP to 2 decimal places.
     */
    fun calculateSubtotal(total: Double): Double {
        return BigDecimal(total)
            .divide(BigDecimal("1.16"), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates IVA such that SUBTOTAL + IVA = TOTAL exactly.
     * Computed as total - calculateSubtotal(total) to guarantee the invariant.
     */
    fun calculateIva(total: Double): Double {
        val subtotal = calculateSubtotal(total)
        return BigDecimal(total)
            .setScale(2, RoundingMode.HALF_UP)
            .subtract(BigDecimal(subtotal).setScale(2, RoundingMode.HALF_UP))
            .toDouble()
    }

    private const val TICKET_WIDTH = 48
    private val SEPARATOR = "-".repeat(TICKET_WIDTH)

    /**
     * Formats the client-facing ticket with full itemized pricing, tax breakdown, and totals.
     * Pure function: same inputs always produce the same output.
     */
    fun formatClientTicket(
        ticketId: String,
        dateTime: String,
        customerName: String,
        paymentStatus: String,
        items: List<TicketLineItem>,
        totalAmount: Double,
        cashReceived: Double = 0.0,
        change: Double = 0.0
    ): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("LOS TACOS".centerIn(TICKET_WIDTH))
        sb.appendLine("Ticket: $ticketId")
        sb.appendLine(dateTime)
        sb.appendLine("Nombre: $customerName")
        sb.appendLine(paymentStatus)
        sb.appendLine(SEPARATOR)

        // Items table header: CANT(5) + DESCRIPCION(30) + IMPORTE(13) = 48
        sb.appendLine("CANT ".padEnd(5) + "DESCRIPCION".padEnd(30) + "IMPORTE".padStart(13))

        // Items
        for (item in items) {
            if (item.isDivider) {
                sb.appendLine("-".repeat(48))
                continue
            }
            val qty = item.quantity.toString().padEnd(5)
            val name = item.productName.take(30).padEnd(30)
            val amount = formatCurrency(item.lineTotal).padStart(13)
            sb.appendLine("$qty$name$amount")
            for (customization in item.customizations) {
                sb.appendLine("      - ${customization.take(40)}")
            }
            val notesBlock = formatExtraNotes(item.extraNotes)
            if (notesBlock.isNotEmpty()) {
                sb.append(notesBlock)
            }
        }

        sb.appendLine(SEPARATOR)
        sb.append(formatFinancialSection(totalAmount, cashReceived, change))
        sb.appendLine(SEPARATOR)

        // Footer
        sb.appendLine("Gracias por su compra".centerIn(TICKET_WIDTH))
        sb.appendLine("Conserve su ticket".centerIn(TICKET_WIDTH))

        return sb.toString().trimEnd('\n')
    }

    /**
     * Formats the internal ticket (kitchen copy) with item quantities and names,
     * but NO prices. Includes a total article count line.
     * Pure function: same inputs always produce the same output.
     */
    fun formatInternalTicket(
        ticketId: String,
        dateTime: String,
        customerName: String,
        paymentStatus: String,
        items: List<TicketLineItem>
    ): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("LOS TACOS".centerIn(TICKET_WIDTH))
        sb.appendLine("Ticket: $ticketId")
        sb.appendLine(dateTime)
        sb.appendLine("Nombre: $customerName")
        sb.appendLine(paymentStatus)
        sb.appendLine(SEPARATOR)

        // Items table header: CANT(5) + DESCRIPCION(30) + IMPORTE(13) = 48
        sb.appendLine("CANT ".padEnd(5) + "DESCRIPCION".padEnd(30) + "IMPORTE".padStart(13))
        sb.appendLine(SEPARATOR)

        // Item lines: quantity + product name + trailing spaces (no price)
        for (item in items) {
            if (item.isDivider) {
                sb.appendLine("-".repeat(48))
                continue
            }
            val qty = item.quantity.toString().padEnd(5)
            val name = item.productName.take(30).padEnd(30)
            val emptyPrice = "".padStart(13)
            sb.appendLine("$qty$name$emptyPrice")
            for (customization in item.customizations) {
                sb.appendLine("      - ${customization.take(40)}")
            }
            val notesBlock = formatExtraNotes(item.extraNotes)
            if (notesBlock.isNotEmpty()) {
                sb.append(notesBlock)
            }
        }

        // Total article count (directly after last item, no separator) — excludes dividers
        val totalCount = items.filter { !it.isDivider }.sumOf { it.quantity }
        sb.appendLine("Total: $totalCount Artículos")

        // Footer
        sb.appendLine("Gracias por su compra".centerIn(TICKET_WIDTH))
        sb.appendLine("Conserve su ticket".centerIn(TICKET_WIDTH))

        return sb.toString().trimEnd('\n')
    }

    /**
     * Formats the internal ticket (kitchen copy) segmented into three parts:
     * header, items, and footer. This allows the printer to apply ESC/POS
     * commands (e.g., Double Height) exclusively to the items section.
     * Pure function: same inputs always produce the same output.
     */
    fun formatInternalTicketSegmented(
        ticketId: String,
        dateTime: String,
        customerName: String,
        paymentStatus: String,
        items: List<TicketLineItem>
    ): InternalTicketSegments {
        val headerSb = StringBuilder()

        // Header: title through column header + separator
        headerSb.appendLine("LOS TACOS".centerIn(TICKET_WIDTH))
        headerSb.appendLine("Ticket: $ticketId")
        headerSb.appendLine(dateTime)
        headerSb.appendLine("Nombre: $customerName")
        headerSb.appendLine(paymentStatus)
        headerSb.appendLine(SEPARATOR)
        headerSb.appendLine("CANT ".padEnd(5) + "DESCRIPCION".padEnd(30) + "IMPORTE".padStart(13))
        headerSb.appendLine(SEPARATOR)

        val itemsSb = StringBuilder()

        // Items: product rows including customization sub-lines, notes, and divider dashes
        for (item in items) {
            if (item.isDivider) {
                itemsSb.appendLine("-".repeat(48))
            } else {
                val qty = item.quantity.toString().padEnd(5)
                val name = item.productName.take(30).padEnd(30)
                val emptyPrice = "".padStart(13)
                itemsSb.appendLine("$qty$name$emptyPrice")
                for (customization in item.customizations) {
                    itemsSb.appendLine("      - ${customization.take(40)}")
                }
                val notesBlock = formatExtraNotes(item.extraNotes)
                if (notesBlock.isNotEmpty()) {
                    itemsSb.append(notesBlock)
                }
            }
        }

        val footerSb = StringBuilder()

        // Footer: article count (excludes dividers) + footer lines
        val totalCount = items.filter { !it.isDivider }.sumOf { it.quantity }
        footerSb.appendLine("Total: $totalCount Artículos")
        footerSb.appendLine("Gracias por su compra".centerIn(TICKET_WIDTH))
        footerSb.appendLine("Conserve su ticket".centerIn(TICKET_WIDTH))

        return InternalTicketSegments(
            header = headerSb.toString().trimEnd('\n'),
            items = itemsSb.toString().trimEnd('\n'),
            footer = footerSb.toString().trimEnd('\n')
        )
    }

    /**
     * Formats extra notes with word-wrapping for ticket printing.
     * First line prefix: "      * Nota: " (14 chars), leaving 34 chars for content.
     * Continuation lines: 13 spaces indent, leaving 35 chars for content.
     * Maximum 8 continuation lines (9 total lines including first).
     * Returns empty string if notes is empty or only whitespace.
     */
    private fun formatExtraNotes(notes: String): String {
        val trimmed = notes.trim()
        if (trimmed.isEmpty()) return ""

        val firstLinePrefix = "      * Nota: " // 14 chars
        val continuationPrefix = " ".repeat(13) // 13 chars
        val firstLineMaxChars = 34
        val continuationMaxChars = 35
        val maxContinuationLines = 8

        val sb = StringBuilder()
        var remaining = trimmed

        // First line
        val firstContent = remaining.take(firstLineMaxChars)
        remaining = remaining.drop(firstLineMaxChars)
        sb.append(firstLinePrefix)
        sb.append(firstContent)
        sb.append("\n")

        // Continuation lines (max 8)
        var continuationCount = 0
        while (remaining.isNotEmpty() && continuationCount < maxContinuationLines) {
            val lineContent = remaining.take(continuationMaxChars)
            remaining = remaining.drop(continuationMaxChars)
            sb.append(continuationPrefix)
            sb.append(lineContent)
            sb.append("\n")
            continuationCount++
        }

        return sb.toString()
    }

    /**
     * Renders the financial section of the client ticket:
     * Subtotal (antes de IVA), IVA (16%), Total, and optionally Pago/Cambio.
     * Each line is exactly 48 characters: label left-aligned + "$X.XX" right-aligned.
     * Does NOT include separator lines — those are added by the caller.
     */
    private fun formatFinancialSection(
        totalAmount: Double,
        cashReceived: Double,
        change: Double
    ): String {
        val sb = StringBuilder()

        // Calculate subtotal and IVA using BigDecimal for precision
        val subtotal = BigDecimal(totalAmount)
            .divide(BigDecimal("1.16"), 2, RoundingMode.HALF_UP)
        val iva = BigDecimal(totalAmount)
            .setScale(2, RoundingMode.HALF_UP)
            .subtract(subtotal)

        // Helper to format a single 48-char line: label left + amount right
        fun formatLine(label: String, amount: BigDecimal): String {
            val formattedAmount = "$${amount.toPlainString()}"
            val padding = TICKET_WIDTH - label.length - formattedAmount.length
            return label + " ".repeat(padding.coerceAtLeast(0)) + formattedAmount + "\n"
        }

        sb.append(formatLine("Subtotal (antes de IVA):", subtotal))
        sb.append(formatLine("IVA (16%):", iva))
        sb.append(formatLine("Total:", BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP)))

        if (cashReceived > 0) {
            val cashBD = BigDecimal(cashReceived).setScale(2, RoundingMode.HALF_UP)
            val changeBD = BigDecimal(change).setScale(2, RoundingMode.HALF_UP)
            sb.append(formatLine("Pago (Efectivo MXN):", cashBD))
            sb.append(formatLine("Cambio:", changeBD))
        }

        return sb.toString()
    }

    /**
     * Centers a string within the given width by padding with spaces.
     */
    private fun String.centerIn(width: Int): String {
        if (this.length >= width) return this.take(width)
        val totalPadding = width - this.length
        val leftPad = totalPadding / 2
        return " ".repeat(leftPad) + this
    }
}
