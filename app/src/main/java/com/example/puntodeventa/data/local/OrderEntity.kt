package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,               // UUID
    val timestamp: Long,                      // epoch millis
    val totalAmount: Double,                  // 0.00..999,999,999.99
    val status: String,                       // "COMPLETED" | "CANCELLED" | "REFUNDED"
    val customerName: String? = null,         // max 120 chars
    val clientTicketText: String? = null,     // max 10,000 chars
    val internalTicketText: String? = null    // max 10,000 chars
)
