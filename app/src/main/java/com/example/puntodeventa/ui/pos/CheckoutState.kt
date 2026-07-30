package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.model.PaymentMethod

data class CheckoutState(
    val customerName: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.PAGADO,
    // Tender type persisted with the order and aggregated by the statistics dashboard. (Req 14.3)
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
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
