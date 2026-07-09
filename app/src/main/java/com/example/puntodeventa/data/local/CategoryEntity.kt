package com.example.puntodeventa.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["associatedMenuId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("associatedMenuId")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val associatedMenuId: String
)
