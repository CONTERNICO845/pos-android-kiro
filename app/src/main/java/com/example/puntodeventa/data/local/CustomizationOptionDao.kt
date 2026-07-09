package com.example.puntodeventa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomizationOptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: CustomizationOptionEntity)

    @Query("SELECT * FROM customization_options WHERE groupId = :groupId")
    fun getOptionsByGroup(groupId: String): Flow<List<CustomizationOptionEntity>>

    @Query("SELECT * FROM customization_options WHERE groupId = :groupId")
    suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity>

    @Query("DELETE FROM customization_options WHERE id = :id")
    suspend fun deleteById(id: String)
}
