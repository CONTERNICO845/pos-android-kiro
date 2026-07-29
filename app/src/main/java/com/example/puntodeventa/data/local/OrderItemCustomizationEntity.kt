package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_item_customizations",
    foreignKeys = [
        ForeignKey(
            entity = OrderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderItemId")]
)
data class OrderItemCustomizationEntity(
    @PrimaryKey val id: String,               // UUID
    val orderItemId: String,                  // FK → order_items.id
    val optionName: String,                   // max 120 chars
    val extraPrice: Double                    // min 0.00
)
