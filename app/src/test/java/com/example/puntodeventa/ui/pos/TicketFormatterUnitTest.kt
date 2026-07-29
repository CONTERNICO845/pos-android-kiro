package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Unit tests for TicketFormatter edge cases.
 *
 * Validates: Requirements 2.6, 3.3, 3.4, 3.5, 4.1, 4.2, 5.1, 6.2
 */
class TicketFormatterUnitTest : StringSpec({

    /**
     * Test 1: Verify the ticket output contains the exact financial labels.
     * Validates: Requirements 4.1, 4.2
     */
    "ticket contains exact financial labels" {
        val items = listOf(
            TicketLineItem(quantity = 1, productName = "Taco", lineTotal = 100.0)
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 100.0,
            cashReceived = 200.0,
            change = 100.0
        )

        output shouldContain "Subtotal (antes de IVA):"
        output shouldContain "IVA (16%):"
        output shouldContain "Total:"
        output shouldContain "Pago (Efectivo MXN):"
        output shouldContain "Cambio:"
    }

    /**
     * Test 2: When cashReceived < totalAmount with status PAGADO, change shows "$0.00".
     * Validates: Requirements 2.6
     */
    "cashReceived less than totalAmount with PAGADO shows change as dollar 0.00" {
        val items = listOf(
            TicketLineItem(quantity = 1, productName = "Taco", lineTotal = 100.0)
        )

        // cashReceived = 50 < totalAmount = 100, but change is coerced to 0.0 by caller
        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 100.0,
            cashReceived = 50.0,
            change = 0.0
        )

        output shouldContain "Cambio:"
        // The Cambio line should contain $0.00
        val lines = output.split("\n")
        val cambioLine = lines.first { it.contains("Cambio:") }
        cambioLine shouldContain "\$0.00"
    }

    /**
     * Test 3: Ticket with 0 customizations and empty extraNotes produces no extra lines.
     * Validates: Requirements 3.4, 5.1
     */
    "ticket with 0 customizations and empty extraNotes has no extra lines" {
        val items = listOf(
            TicketLineItem(
                quantity = 1,
                productName = "Taco al Pastor",
                lineTotal = 45.0,
                customizations = emptyList(),
                extraNotes = ""
            )
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 45.0,
            cashReceived = 0.0,
            change = 0.0
        )

        // No customization lines (prefixed with "      - ")
        output shouldNotContain "      - "
        // No note lines (prefixed with "* Nota:")
        output shouldNotContain "* Nota:"
    }

    /**
     * Test 4: extraNotes with exactly 34 chars fits on one line (no continuation).
     * Validates: Requirements 3.3, 3.5
     */
    "extraNotes with exactly 34 chars does not wrap" {
        // Exactly 34 characters
        val notes34 = "A".repeat(34)

        val items = listOf(
            TicketLineItem(
                quantity = 1,
                productName = "Producto",
                lineTotal = 50.0,
                customizations = emptyList(),
                extraNotes = notes34
            )
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        val lines = output.split("\n")
        val noteLineIndex = lines.indexOfFirst { it.contains("* Nota:") }
        (noteLineIndex >= 0) shouldBe true

        // The note line should contain all 34 chars
        val noteLine = lines[noteLineIndex]
        noteLine shouldContain notes34

        // No continuation line (next line should NOT start with 13 spaces indent pattern)
        val continuationIndent = " ".repeat(13)
        if (noteLineIndex + 1 < lines.size) {
            val nextLine = lines[noteLineIndex + 1]
            // Next line should not be a continuation (either it's a separator or something else)
            val isContinuation = nextLine.startsWith(continuationIndent) &&
                !nextLine.startsWith("      * Nota:") &&
                !nextLine.startsWith("      - ") &&
                nextLine.trim().isNotEmpty()
            isContinuation shouldBe false
        }
    }

    /**
     * Test 5: extraNotes with 35 chars produces exactly 1 continuation line.
     * Validates: Requirements 3.3, 3.5
     */
    "extraNotes with 35 chars produces exactly 1 continuation line" {
        // 35 characters: first line gets 34, continuation gets 1
        val notes35 = "B".repeat(35)

        val items = listOf(
            TicketLineItem(
                quantity = 1,
                productName = "Producto",
                lineTotal = 50.0,
                customizations = emptyList(),
                extraNotes = notes35
            )
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        val lines = output.split("\n")
        val noteLineIndex = lines.indexOfFirst { it.contains("* Nota:") }
        (noteLineIndex >= 0) shouldBe true

        // Count continuation lines (13-space indent that aren't note or customization lines)
        val continuationIndent = " ".repeat(13)
        var continuationCount = 0
        for (i in (noteLineIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.startsWith(continuationIndent) &&
                !line.startsWith("      * Nota:") &&
                !line.startsWith("      - ") &&
                line.trim().isNotEmpty()
            ) {
                continuationCount++
            } else {
                break
            }
        }

        continuationCount shouldBe 1
    }

    /**
     * Test 6: productName of exactly 30 chars is not truncated.
     * Validates: Requirements 6.2
     */
    "productName of exactly 30 chars is not truncated" {
        val name30 = "C".repeat(30)

        val items = listOf(
            TicketLineItem(quantity = 1, productName = name30, lineTotal = 50.0)
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        output shouldContain name30
    }

    /**
     * Test 7: productName of 31 chars is truncated to 30.
     * Validates: Requirements 6.2
     */
    "productName of 31 chars is truncated to 30" {
        val name31 = "D".repeat(31)
        val expectedTruncated = "D".repeat(30)

        val items = listOf(
            TicketLineItem(quantity = 1, productName = name31, lineTotal = 50.0)
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        // The full 31-char name should NOT appear
        output shouldNotContain name31
        // The truncated 30-char name should appear
        output shouldContain expectedTruncated
    }

    /**
     * Test 8: optionName of 40 chars is not truncated.
     * Validates: Requirements 5.1
     */
    "optionName of 40 chars is not truncated" {
        val option40 = "E".repeat(40)

        val items = listOf(
            TicketLineItem(
                quantity = 1,
                productName = "Producto",
                lineTotal = 50.0,
                customizations = listOf(option40),
                extraNotes = ""
            )
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        // "      - " prefix + full 40-char option should appear
        output shouldContain "      - $option40"
    }

    /**
     * Test 9: optionName of 41 chars is truncated to 40.
     * Validates: Requirements 5.1
     */
    "optionName of 41 chars is truncated to 40" {
        val option41 = "F".repeat(41)
        val expectedTruncated = "F".repeat(40)

        val items = listOf(
            TicketLineItem(
                quantity = 1,
                productName = "Producto",
                lineTotal = 50.0,
                customizations = listOf(option41),
                extraNotes = ""
            )
        )

        val output = TicketFormatter.formatClientTicket(
            ticketId = "001",
            dateTime = "01/01/2025 12:00:00",
            customerName = "Cliente",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 50.0,
            cashReceived = 0.0,
            change = 0.0
        )

        // The full 41-char option should NOT appear after prefix
        output shouldNotContain "      - $option41"
        // The truncated 40-char option should appear after prefix
        output shouldContain "      - $expectedTruncated"
    }
})
