package com.example.puntodeventa.data.model

/**
 * Room query projection: one order reduced to (when, how much). (Req 2.10)
 *
 * Feeds the sales trend chart. Bucketing into hours/days/months happens in
 * `SalesTrendCalculator` (pure Kotlin + java.time) rather than in SQL, because SQLite's `strftime`
 * would need manual timezone-offset arithmetic and could not be covered by JVM tests.
 */
data class OrderTotalPoint(
    val timestamp: Long,
    val amount: Double
)
