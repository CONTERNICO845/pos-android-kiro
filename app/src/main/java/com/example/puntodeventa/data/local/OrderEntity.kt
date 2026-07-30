package com.example.puntodeventa.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.puntodeventa.data.model.PAYMENT_METHOD_CASH_STORAGE_VALUE
import com.example.puntodeventa.data.model.PaymentMethod

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,               // UUID
    val timestamp: Long,                      // epoch millis
    val totalAmount: Double,                  // 0.00..999,999,999.99
    val status: String,                       // "COMPLETED" | "CANCELLED" | "REFUNDED"
    val customerName: String? = null,         // max 120 chars
    val clientTicketText: String? = null,     // max 10,000 chars
    val internalTicketText: String? = null,   // max 10,000 chars
    // Tender type: "EFECTIVO" | "TARJETA" | "TRANSFERENCIA". Added in schema v5 with a SQL default
    // so orders stored before the upgrade are classified as cash. (Req 14.1, 14.2)
    @ColumnInfo(defaultValue = PAYMENT_METHOD_CASH_STORAGE_VALUE)
    val paymentMethod: String = PaymentMethod.CASH.storageValue
)
