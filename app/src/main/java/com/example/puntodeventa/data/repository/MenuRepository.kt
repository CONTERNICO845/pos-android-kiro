package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.MenuItemDao
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.model.MenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MenuRepository(private val dao: MenuItemDao) {

    val menuItems: Flow<List<MenuItem>> =
        dao.getAllMenuItems().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun insert(item: MenuItem) {
        dao.insert(item.toEntity())
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun MenuItemEntity.toDomain(): MenuItem =
        MenuItem(id = id, emoji = emoji, name = name)

    private fun MenuItem.toEntity(): MenuItemEntity =
        MenuItemEntity(id = id, emoji = emoji, name = name)
}
