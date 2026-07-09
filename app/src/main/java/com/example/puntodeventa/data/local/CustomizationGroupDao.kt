package com.example.puntodeventa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomizationGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternal(group: CustomizationGroupEntity)

    @Query("SELECT * FROM customization_groups WHERE productId = :productId")
    fun getGroupsByProduct(productId: String): Flow<List<CustomizationGroupEntity>>

    @Query("SELECT * FROM customization_groups WHERE productId = :productId")
    suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity>

    @Query("DELETE FROM customization_groups WHERE id = :id")
    suspend fun deleteById(id: String)
}
