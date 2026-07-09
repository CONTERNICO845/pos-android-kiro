package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customization_groups",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class CustomizationGroupEntity(
    @PrimaryKey val id: String,
    val productId: String,      // FK → products.id
    val groupName: String,
    val selectionType: String   // "multiple_checkboxes" | "single_option"
)
