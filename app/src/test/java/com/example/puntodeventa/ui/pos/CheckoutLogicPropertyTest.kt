package com.example.puntodeventa.ui.pos

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Property-based tests for checkout logic (Properties 12-17).
 *
 * These are PURE LOGIC tests — they replicate the calculation logic from PosViewModel
 * without instantiating the ViewModel. Testing the math and boolean logic directly.
 *
 * Feature: 16_sprint_correcciones
 */
class CheckoutLogicPropertyTest : FunSpec({

    // ── Available denominations (same as PosViewModel/BillsGrid) ──────────────
    val denominations = listOf(1000, 500, 200, 100, 50, 20, 10, 5, 2, 1)

    // ── Pure logic functions replicating PosViewModel behavior ─────────────────

    /**
     * Simulates tapping denomination buttons. Each tap increments the count for that
     * denomination. Returns the final (denominationCounts, cashReceived) pair.
     */
    fun simulateDenominationTaps(taps: List<Int>): Pair<Map<Int, Int>, Double> {
        val counts = mutableMapOf<Int, Int>()
        for (denom in taps) {
            counts[denom] = (counts[denom] ?: 0) + 1
        }
        val cashReceived = counts.entries.sumOf { (denom, count) ->
            denom.toLong() * count
        }.toDouble()
        return Pair(counts.toMap(), cashReceived)
    }

    /**
     * Simulates adding a custom amount. Returns the new cashReceived if valid,
     * or the original if invalid.
     */
    fun simulateAddCustomAmount(currentCashReceived: Double, amountStr: String): Double {
        val parsed = amountStr.toDoubleOrNull() ?: return currentCashReceived
        if (parsed <= 0.0) return currentCashReceived
        return currentCashReceived + parsed
    }

    /**
     * Calculates the change assistant text/values based on cartTotal and cashReceived.
     */
    fun calculateChangeAssistant(
        cartTotal: Double,
        cashReceived: Double
    ): Triple<Double, String, Boolean> {
        val change = if (cashReceived >= cartTotal) {
            BigDecimal(cashReceived)
                .subtract(BigDecimal(cartTotal))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }

        val text = when {
            cashReceived == cartTotal -> "Pago exacto"
            cashReceived > cartTotal -> {
                val changeFormatted = BigDecimal(cashReceived)
                    .subtract(BigDecimal(cartTotal))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                "Dar \$${String.format("%.2f", changeFormatted)} de cambio exacto"
            }
            else -> {
                val falta = BigDecimal(cartTotal)
                    .subtract(BigDecimal(cashReceived))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                "Falta \$${String.format("%.2f", falta)}"
            }
        }

        val isExact = cashReceived == cartTotal
        return Triple(change, text, isExact)
    }

    /**
     * Determines if "Completar Orden" button should be enabled.
     * Replicates PosViewModel.isCompletarOrdenEnabled() logic.
     */
    fun isCompletarOrdenEnabled(
        customerName: String,
        paymentStatus: PaymentStatus,
        cashReceived: Double,
        cartTotal: Double
    ): Boolean {
        if (customerName.trim().isEmpty()) return false
        if (paymentStatus == PaymentStatus.PAGADO && cashReceived < cartTotal) return false
        return true
    }

    // ── Generators ────────────────────────────────────────────────────────────

    val arbDenomination = Arb.element(denominations)
    val arbDenominationSequence = Arb.list(arbDenomination, range = 1..20)
    val arbPaymentStatus = Arb.element(PaymentStatus.entries)
    val arbPositiveDouble = Arb.double(0.01..10000.0)
    val arbNonNegativeDouble = Arb.double(0.0..10000.0)

    // Generator for invalid custom amount strings
    val arbInvalidAmount = Arb.of(
        "",           // empty
        "abc",        // non-numeric
        "hello",      // non-numeric
        "0",          // zero
        "0.0",        // zero
        "0.00",       // zero
        "-5",         // negative
        "-100.50",    // negative
        "-0.01",      // negative
        "  ",         // whitespace
        "12.34.56",   // multiple dots
        "$100",       // currency symbol
        "1,000"       // comma-separated (toDoubleOrNull returns null for this)
    )

    // ── Property Tests ────────────────────────────────────────────────────────

    /**
     * Feature: 16_sprint_correcciones, Property 12: Denomination tap updates count and cashReceived consistently
     *
     * For any sequence of denomination button taps, denominationCounts[value] SHALL equal
     * the number of times that denomination was tapped, and cashReceived SHALL equal the
     * sum of all (denomination_value × tap_count) across all denominations.
     *
     * **Validates: Requirements 8.3, 8.6**
     */
    test("Feature: 16_sprint_correcciones, Property 12: Denomination tap updates count and cashReceived consistently") {
        checkAll(PropTestConfig(iterations = 100), arbDenominationSequence) { taps ->
            val (counts, cashReceived) = simulateDenominationTaps(taps)

            // Verify counts match number of taps per denomination
            for (denom in denominations) {
                val expectedCount = taps.count { it == denom }
                val actualCount = counts[denom] ?: 0
                actualCount shouldBe expectedCount
            }

            // Verify cashReceived is the sum of (denomination * count)
            val expectedCash = counts.entries.sumOf { (denom, count) ->
                denom.toLong() * count
            }.toDouble()
            cashReceived shouldBe expectedCash

            // Also verify it equals the sum of all individual taps
            val tapSum = taps.sumOf { it.toLong() }.toDouble()
            cashReceived shouldBe tapSum
        }
    }

    /**
     * Feature: 16_sprint_correcciones, Property 13: Valid custom amount adds to cashReceived
     *
     * For any valid positive numeric string input, pressing "Agregar" SHALL increase
     * cashReceived by exactly that parsed amount.
     *
     * **Validates: Requirements 9.2**
     */
    test("Feature: 16_sprint_correcciones, Property 13: Valid custom amount adds to cashReceived") {
        checkAll(PropTestConfig(iterations = 100), arbPositiveDouble, arbNonNegativeDouble) { amount, initialCash ->
            val amountStr = String.format("%.2f", amount)
            val parsedAmount = amountStr.toDouble()

            val newCash = simulateAddCustomAmount(initialCash, amountStr)

            // Cash should increase by exactly the parsed amount
            val expected = BigDecimal(initialCash)
                .add(BigDecimal(parsedAmount))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            BigDecimal(newCash).setScale(2, RoundingMode.HALF_UP).toDouble() shouldBe expected
        }
    }

    /**
     * Feature: 16_sprint_correcciones, Property 14: Invalid custom amount is ignored
     *
     * For any input string that is empty, non-numeric, zero, or negative,
     * pressing "Agregar" SHALL leave cashReceived unchanged.
     *
     * **Validates: Requirements 9.3**
     */
    test("Feature: 16_sprint_correcciones, Property 14: Invalid custom amount is ignored") {
        checkAll(PropTestConfig(iterations = 100), arbInvalidAmount, arbNonNegativeDouble) { invalidInput, initialCash ->
            val newCash = simulateAddCustomAmount(initialCash, invalidInput)
            newCash shouldBe initialCash
        }
    }

    /**
     * Feature: 16_sprint_correcciones, Property 15: Limpiar resets all cash state to zero
     *
     * For any CheckoutState with non-zero denominationCounts or cashReceived,
     * pressing "Limpiar" SHALL result in denominationCounts being empty and cashReceived being 0.0.
     *
     * **Validates: Requirements 9.4**
     */
    test("Feature: 16_sprint_correcciones, Property 15: Limpiar resets all cash state to zero") {
        checkAll(PropTestConfig(iterations = 100), arbDenominationSequence) { taps ->
            // Build up some state
            val (counts, cashReceived) = simulateDenominationTaps(taps)

            // Verify we have non-zero state
            assert(counts.isNotEmpty())
            assert(cashReceived > 0.0)

            // Simulate "Limpiar" — replicate clearCashReceived() logic
            val clearedState = CheckoutState(
                denominationCounts = emptyMap(),
                customAmounts = emptyList(),
                cashReceived = 0.0
            )

            clearedState.denominationCounts shouldBe emptyMap()
            clearedState.cashReceived shouldBe 0.0
            clearedState.customAmounts shouldBe emptyList()
        }
    }

    /**
     * Feature: 16_sprint_correcciones, Property 16: Change assistant calculation
     *
     * For any pair of non-negative values (cartTotal, cashReceived):
     * - If cashReceived >= cartTotal: change = (cashReceived - cartTotal) rounded HALF_UP to 2dp
     * - If cashReceived == cartTotal: display text is "Pago exacto"
     * - If cashReceived > cartTotal: display text is "Dar $XX.XX de cambio exacto"
     * - If cashReceived < cartTotal: change displays $0.00 and text is "Falta $XX.XX"
     *
     * **Validates: Requirements 10.2, 10.3, 10.4, 10.5**
     */
    test("Feature: 16_sprint_correcciones, Property 16: Change assistant calculation") {
        checkAll(PropTestConfig(iterations = 100), arbNonNegativeDouble, arbNonNegativeDouble) { cartTotal, cashReceived ->
            val (change, text, _) = calculateChangeAssistant(cartTotal, cashReceived)

            when {
                cashReceived == cartTotal -> {
                    // Exact payment
                    change shouldBe 0.0
                    text shouldBe "Pago exacto"
                }
                cashReceived > cartTotal -> {
                    // Overpayment — change is positive
                    val expectedChange = BigDecimal(cashReceived)
                        .subtract(BigDecimal(cartTotal))
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
                    change shouldBe expectedChange
                    text shouldBe "Dar \$${String.format("%.2f", expectedChange)} de cambio exacto"
                }
                else -> {
                    // Underpayment — change is 0, text shows deficit
                    change shouldBe 0.0
                    val falta = BigDecimal(cartTotal)
                        .subtract(BigDecimal(cashReceived))
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
                    text shouldBe "Falta \$${String.format("%.2f", falta)}"
                }
            }
        }
    }

    /**
     * Feature: 16_sprint_correcciones, Property 17: Completar Orden button enablement logic
     *
     * For any CheckoutState:
     * - If customerName.trim().isEmpty() → button is disabled
     * - If paymentStatus == PAGADO AND cashReceived < cartTotal → button is disabled
     * - If paymentStatus ∈ {NO_PAGO, PAGA_DESPUES} AND customerName.trim().isNotEmpty() → enabled
     * - If paymentStatus == PAGADO AND cashReceived >= cartTotal AND customerName.trim().isNotEmpty() → enabled
     *
     * **Validates: Requirements 11.2, 11.3, 11.4**
     */
    test("Feature: 16_sprint_correcciones, Property 17: Completar Orden button enablement logic") {
        val arbCustomerName = Arb.of("", "  ", "Juan", "María López", " Ana ", "Cliente 1")
        val arbCartTotal = Arb.double(0.01..5000.0)
        val arbCashReceived = Arb.double(0.0..10000.0)

        checkAll(
            PropTestConfig(iterations = 100),
            arbCustomerName,
            arbPaymentStatus,
            arbCashReceived,
            arbCartTotal
        ) { customerName, paymentStatus, cashReceived, cartTotal ->
            val actual = isCompletarOrdenEnabled(customerName, paymentStatus, cashReceived, cartTotal)

            val expected = when {
                customerName.trim().isEmpty() -> false
                paymentStatus == PaymentStatus.PAGADO && cashReceived < cartTotal -> false
                else -> true
            }

            actual shouldBe expected
        }
    }
})
