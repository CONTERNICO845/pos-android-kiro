package com.example.puntodeventa.data.model

/**
 * Storage token of [PaymentMethod.CASH], declared at file level as a `const`.
 *
 * Room's `@ColumnInfo(defaultValue = …)` and the v4→v5 SQL migration both need a compile-time
 * constant, which an enum property cannot provide (the companion object is not initialized while the
 * entries are being constructed).
 */
const val PAYMENT_METHOD_CASH_STORAGE_VALUE = "EFECTIVO"

/**
 * Tender type captured at checkout and persisted on every order. (Req 14.1)
 *
 * [storageValue] is the stable token written to `orders.paymentMethod` — uppercase ASCII so it never
 * depends on a UI label. [displayName] is what the checkout, the dashboard and the CSV report show.
 */
enum class PaymentMethod(val storageValue: String, val displayName: String) {
    CASH(PAYMENT_METHOD_CASH_STORAGE_VALUE, "Efectivo"),
    CARD("TARJETA", "Tarjeta"),
    TRANSFER("TRANSFERENCIA", "Transferencia");

    companion object {

        /**
         * Resolves a persisted token. Unknown or legacy values fall back to [CASH] so their revenue
         * is still counted in the payment breakdown instead of being silently dropped. (Req 14.9)
         */
        fun fromStorage(value: String?): PaymentMethod =
            entries.firstOrNull { it.storageValue == value } ?: CASH
    }
}
