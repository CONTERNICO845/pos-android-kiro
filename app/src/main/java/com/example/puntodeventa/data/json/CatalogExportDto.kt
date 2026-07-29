package com.example.puntodeventa.data.json

import kotlinx.serialization.Serializable

/**
 * Root DTO for the catalog JSON export/import.
 * Contains schema version, export timestamp, and the full catalog data.
 */
@Serializable
data class CatalogExport(
    val version: Int,
    val exportedAt: String,
    val catalog: CatalogData
)

@Serializable
data class CatalogData(
    val menuItems: List<MenuItemDto>
)

@Serializable
data class MenuItemDto(
    val id: String,
    val emoji: String,
    val name: String,
    val categories: List<CategoryDto>
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val products: List<ProductDto>
)

@Serializable
data class ProductDto(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val isActive: Boolean,
    val customizationGroups: List<CustomizationGroupDto>
)

@Serializable
data class CustomizationGroupDto(
    val id: String,
    val groupName: String,
    val selectionType: String,
    val options: List<CustomizationOptionDto>
)

@Serializable
data class CustomizationOptionDto(
    val id: String,
    val optionName: String,
    val extraPrice: Double
)
