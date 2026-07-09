package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customization_options",
    foreignKeys = [
        ForeignKey(
            entity = CustomizationGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class CustomizationOptionEntity(
    @PrimaryKey val id: String,
    val groupId: String,        // FK → customization_groups.id
    val optionName: String,     // max 120 chars
    val extraPrice: Double      // ≥ 0.0; 0.0 = no surcharge
)
