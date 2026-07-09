package com.example.puntodeventa.data.model

data class Product(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val isActive: Boolean,
    val categoryId: String
)
