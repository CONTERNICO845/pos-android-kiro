package com.example.puntodeventa.ui.pos

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.map
import java.util.UUID

/**
 * Kotest property-based testing generators for the printer-audit-and-ticket-format spec.
 *
 * Validates: Requirements 6.1, 6.6
 */

// Characters printable in Cp850 (latin printable subset: ASCII printable + common accented chars)
private val CP850_CHARS: List<Char> = ((' '..'~').toList() +
        listOf('á', 'é', 'í', 'ó', 'ú', 'ñ', 'Ñ', 'ü', 'Ü', '¿', '¡', 'ä', 'ö', 'Á', 'É', 'Í', 'Ó', 'Ú'))

/**
 * Generates a random string of [length] characters from the Cp850-compatible charset.
 */
private fun Arb.Companion.cp850String(lengthRange: IntRange): Arb<String> = arbitrary { rs ->
    val length = Arb.int(lengthRange).bind()
    buildString {
        repeat(length) {
            append(CP850_CHARS[rs.random.nextInt(CP850_CHARS.size)])
        }
    }
}

/**
 * Arb.positiveMoney() — Double in range $0.01–$999,999.99
 */
fun Arb.Companion.positiveMoney(): Arb<Double> =
    Arb.double(0.01..999_999.99)
        .map { Math.round(it * 100.0) / 100.0 }

/**
 * Arb.extraNotesString() — String 0–300 chars with printable latin characters compatible with Cp850
 */
fun Arb.Companion.extraNotesString(): Arb<String> =
    Arb.cp850String(0..300)

/**
 * Arb.cartItem() — generates CartItem with:
 * - quantity: 1–99
 * - totalPrice > 0 (range 0.01–999,999.99)
 * - productName: 1–30 printable ASCII/latin chars
 * - extraNotes: 0–280 chars
 * - 0–5 selectedCustomizations
 */
fun Arb.Companion.cartItem(): Arb<CartItem> = arbitrary { rs ->
    val quantity = Arb.int(1..99).bind()
    val basePrice = Arb.positiveMoney().bind()
    val productName = Arb.cp850String(1..30).bind()
    val extraNotes = Arb.cp850String(0..280).bind()

    val customizationCount = Arb.int(0..5).bind()
    val customizations = (1..customizationCount).map {
        SelectedCustomization(
            optionId = UUID.randomUUID().toString(),
            optionName = Arb.cp850String(1..40).bind(),
            extraPrice = Arb.double(0.0..100.0).map { p -> Math.round(p * 100.0) / 100.0 }.bind()
        )
    }

    val totalPrice = java.math.BigDecimal.valueOf(basePrice)
        .add(customizations.fold(java.math.BigDecimal.ZERO) { acc, c ->
            acc.add(java.math.BigDecimal.valueOf(c.extraPrice))
        })
        .multiply(java.math.BigDecimal.valueOf(quantity.toLong()))
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toDouble()

    CartItem(
        id = UUID.randomUUID().toString(),
        productId = UUID.randomUUID().toString(),
        productName = productName,
        emoji = "🌮",
        basePrice = basePrice,
        quantity = quantity,
        selectedCustomizations = customizations,
        extraNotes = extraNotes,
        totalPrice = totalPrice
    )
}

/**
 * Arb.ticketLineItem() — generates TicketLineItem derived from the cartItem generator
 * (maps CartItem fields to TicketLineItem fields as PosViewModel does)
 */
fun Arb.Companion.ticketLineItem(): Arb<TicketLineItem> = arbitrary {
    val item = Arb.cartItem().bind()
    TicketLineItem(
        quantity = item.quantity,
        productName = item.productName,
        lineTotal = item.totalPrice,
        customizations = item.selectedCustomizations.map { it.optionName },
        extraNotes = item.extraNotes
    )
}

/**
 * Data class representing a payment scenario for property testing.
 */
data class PaymentScenario(
    val cashReceived: Double,
    val totalAmount: Double,
    val paymentStatus: String
)

/**
 * Arb.paymentScenario() — generates valid combinations of (cashReceived, totalAmount, paymentStatus).
 * When paymentStatus is "Pagado", cashReceived >= totalAmount (most of the time).
 * totalAmount is always > 0.
 */
fun Arb.Companion.paymentScenario(): Arb<PaymentScenario> = arbitrary { rs ->
    val totalAmount = Arb.positiveMoney().bind()
    val isPagado = rs.random.nextBoolean()

    if (isPagado) {
        // When "Pagado", cashReceived >= totalAmount (generate extra 0..500 on top)
        val extra = Arb.double(0.0..500.0).map { Math.round(it * 100.0) / 100.0 }.bind()
        val cashReceived = Math.round((totalAmount + extra) * 100.0) / 100.0
        PaymentScenario(
            cashReceived = cashReceived,
            totalAmount = totalAmount,
            paymentStatus = "Pagado"
        )
    } else {
        // Non-paid status: cashReceived is 0
        PaymentScenario(
            cashReceived = 0.0,
            totalAmount = totalAmount,
            paymentStatus = "Pendiente"
        )
    }
}
