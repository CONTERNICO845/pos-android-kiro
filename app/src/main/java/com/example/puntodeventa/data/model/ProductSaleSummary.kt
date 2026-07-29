package com.example.puntodeventa.data.model

/**
 * Room query-result projection for aggregated product sales.
 * Field names match column aliases in DAO queries.
 */
data class ProductSaleSummary(
    val productName: String,
    val totalQuantity: Int,
    val totalRevenue: Double
)
