package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey val id: String,           // UUID, max 36 chars
    val emoji: String,                    // max 8 chars
    val name: String,                     // max 120 chars
    val description: String,              // max 500 chars
    val basePrice: Double,                // ≥ 0.0
    val isActive: Boolean,
    val categoryId: String                // FK → categories.id
)
