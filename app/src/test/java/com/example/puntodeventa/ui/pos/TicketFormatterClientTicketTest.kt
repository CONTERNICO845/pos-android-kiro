package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

class TicketFormatterClientTicketTest : FunSpec({

    val sampleItems = listOf(
        TicketLineItem(quantity = 2, productName = "Taco al Pastor", lineTotal = 70.0),
        TicketLineItem(quantity = 1, productName = "Agua Fresca", lineTotal = 25.0)
    )

    test("formatClientTicket contains LOS TACOS header") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        ticket shouldContain "LOS TACOS"
    }

    test("formatClientTicket contains ticket ID, date, name, and status") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        ticket shouldContain "Ticket: ORD-001"
        ticket shouldContain "15/01/2025 14:30:00"
        ticket shouldContain "Nombre: Juan Perez"
        ticket shouldContain "Pagado"
    }

    test("formatClientTicket separator lines are exactly 48 dashes") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        val separatorLine = "-".repeat(48)
        val lines = ticket.lines()
        val separatorLines = lines.filter { it.all { ch -> ch == '-' } && it.isNotEmpty() }
        separatorLines.forEach { line ->
            line.length shouldBe 48
            line shouldBe separatorLine
        }
    }

    test("formatClientTicket item lines are exactly 48 chars wide") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        val lines = ticket.lines()
        // Find item lines (between second separator and third separator)
        val separatorIndices = lines.mapIndexedNotNull { idx, line ->
            if (line == "-".repeat(48)) idx else null
        }
        // Items are between index separatorIndices[1] + 2 (skip table header) and separatorIndices[2]
        val tableHeaderIdx = separatorIndices[1] + 1
        val itemStart = tableHeaderIdx + 1
        val itemEnd = separatorIndices[2]
        for (i in itemStart until itemEnd) {
            lines[i].length shouldBe 48
        }
    }

    test("formatClientTicket table header is exactly 48 chars") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        val lines = ticket.lines()
        val tableHeader = lines.first { it.startsWith("CANT") }
        tableHeader.length shouldBe 48
    }

    test("formatClientTicket contains SUBTOTAL, IVA, and TOTAL") {
        val total = 116.0
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-002",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Maria",
            paymentStatus = "Pagado",
            items = listOf(TicketLineItem(1, "Combo Grande", 116.0)),
            totalAmount = total
        )
        // subtotal = 116 / 1.16 = 100.00
        // iva = 116 - 100 = 16.00
        ticket shouldContain "Subtotal (antes de IVA):"
        ticket shouldContain "\$100.00"
        ticket shouldContain "IVA (16%):"
        ticket shouldContain "\$16.00"
        ticket shouldContain "Total:"
        ticket shouldContain "\$116.00"
    }

    test("formatClientTicket contains footer text") {
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan Perez",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        ticket shouldContain "Gracias por su compra"
        ticket shouldContain "Conserve su ticket"
    }

    test("formatClientTicket truncates long product names to 30 chars") {
        val longName = "Super Taco Especial de Barbacoa con Guacamole Extra"
        val items = listOf(TicketLineItem(1, longName, 150.0))
        val ticket = TicketFormatter.formatClientTicket(
            ticketId = "ORD-003",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Test",
            paymentStatus = "Pagado",
            items = items,
            totalAmount = 150.0
        )
        // The product name in the ticket should be truncated to 30 chars
        ticket shouldContain longName.take(30)
    }

    test("formatClientTicket is deterministic - same inputs produce same output") {
        val ticket1 = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        val ticket2 = TicketFormatter.formatClientTicket(
            ticketId = "ORD-001",
            dateTime = "15/01/2025 14:30:00",
            customerName = "Juan",
            paymentStatus = "Pagado",
            items = sampleItems,
            totalAmount = 95.0
        )
        ticket1 shouldBe ticket2
    }
})
