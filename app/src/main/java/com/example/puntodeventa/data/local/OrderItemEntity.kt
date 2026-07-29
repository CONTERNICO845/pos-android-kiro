package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,               // UUID
    val orderId: String,                      // FK → orders.id
    val productId: String,
    val productName: String,                  // max 120 chars
    val quantity: Int,                        // min 1
    val basePrice: Double,                    // 0.00..999,999.99
    val totalPrice: Double,                   // 0.00..999,999,999.99
    val extraNotes: String?                   // nullable, max 500 chars
)
