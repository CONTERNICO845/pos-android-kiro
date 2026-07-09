package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.CustomizationGroupDao
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionDao
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.ProductDao
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.local.SelectionType
import com.example.puntodeventa.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val groupDao: CustomizationGroupDao,
    private val optionDao: CustomizationOptionDao
) {

    fun getProductsByCategory(categoryId: String): Flow<List<Product>> =
        productDao.getProductsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getActiveProductsByCategory(categoryId: String): Flow<List<Product>> =
        productDao.getActiveProductsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun insert(product: Product) {
        productDao.insert(product.toEntity())
    }

    suspend fun deleteById(id: String) {
        productDao.deleteById(id)
    }

    // ── Validation-guarded group insert ──────────────────────────────────────

    suspend fun insertGroup(group: CustomizationGroupEntity) {
        SelectionType.fromValue(group.selectionType)
            ?: throw IllegalArgumentException(
                "Invalid selectionType '${group.selectionType}'. " +
                "Must be one of: ${SelectionType.entries.map { it.value }}"
            )
        groupDao.insertInternal(group)
    }

    // ── Validation-guarded option insert ─────────────────────────────────────

    suspend fun insertOption(option: CustomizationOptionEntity) {
        require(option.extraPrice >= 0.0) {
            "extraPrice must be >= 0.0, but was ${option.extraPrice}"
        }
        optionDao.insert(option)
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun ProductEntity.toDomain(): Product =
        Product(
            id          = id,
            emoji       = emoji,
            name        = name,
            description = description,
            basePrice   = basePrice,
            isActive    = isActive,
            categoryId  = categoryId
        )

    private fun Product.toEntity(): ProductEntity =
        ProductEntity(
            id          = id,
            emoji       = emoji,
            name        = name,
            description = description,
            basePrice   = basePrice,
            isActive    = isActive,
            categoryId  = categoryId
        )
}
