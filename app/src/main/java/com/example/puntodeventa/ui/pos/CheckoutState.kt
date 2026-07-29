package com.example.puntodeventa.ui.pos

data class CheckoutState(
    val customerName: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.PAGADO,
    val denominationCounts: Map<Int, Int> = emptyMap(), // denomination value → count
    val cashReceived: Double = 0.0,
    val customAmounts: List<Double> = emptyList(), // tracking custom amount entries
    val printAttempts: Int = 0,
    val isPrinting: Boolean = false
)

enum class PaymentStatus(val displayText: String) {
    PAGADO("Pagado"),
    NO_PAGO("No pagó"),
    PAGA_DESPUES("Paga después")
}
