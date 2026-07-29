package com.example.puntodeventa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE associatedMenuId = :menuId")
    fun getCategoriesByMenu(menuId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE associatedMenuId = :menuId")
    suspend fun getCategoriesByMenuOnce(menuId: String): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
