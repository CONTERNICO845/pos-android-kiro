package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.CategoryDao
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {

    fun getCategoriesByMenu(menuId: String): Flow<List<Category>> =
        dao.getCategoriesByMenu(menuId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun insert(category: Category) {
        dao.insert(category.toEntity())
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun CategoryEntity.toDomain(): Category =
        Category(id = id, name = name, associatedMenuId = associatedMenuId)

    private fun Category.toEntity(): CategoryEntity =
        CategoryEntity(id = id, name = name, associatedMenuId = associatedMenuId)
}
