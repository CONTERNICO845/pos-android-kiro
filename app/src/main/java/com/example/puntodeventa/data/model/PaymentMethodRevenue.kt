package com.example.puntodeventa.data.model

/**
 * Room query projection: revenue and order count for one tender type in a time range. (Req 2.9)
 *
 * [paymentMethod] carries the raw storage token; resolve it with [PaymentMethod.fromStorage] before
 * displaying it so legacy/unknown tokens are folded into "Efectivo".
 */
data class PaymentMethodRevenue(
    val paymentMethod: String,
    val totalRevenue: Double,
    val orderCount: Int
)
