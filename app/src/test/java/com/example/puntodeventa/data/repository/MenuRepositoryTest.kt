package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.MenuItemDao
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.model.MenuItem
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for MenuRepository entity/domain mapping.
 *
 * Property 4: Entity-to-domain mapping round-trip
 * Property 5: Domain-to-entity mapping round-trip
 */
class MenuRepositoryTest : StringSpec({

    /**
     * Property 4: entity-to-domain mapping — collecting menuItems returns
     * MenuItem fields identical to the underlying MenuItemEntity fields.
     */
    "Property 4 — entity-to-domain mapping preserves all fields" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..30),
            Arb.string(1..5),
            Arb.string(1..30)
        ) { id, emoji, name ->
            val entity = MenuItemEntity(id = id, emoji = emoji, name = name)
            val dao = object : MenuItemDao {
                override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(listOf(entity))
                override suspend fun getAllMenuItemsOnce(): List<MenuItemEntity> = listOf(entity)
                override suspend fun insert(item: MenuItemEntity) {}
                override suspend fun deleteById(id: String) {}
                override suspend fun deleteAll() {}
            }

            val repo = MenuRepository(dao)
            val items = repo.menuItems.first()

            assert(items.size == 1) { "Expected 1 item, got ${items.size}" }
            val item = items[0]
            assert(item.id == id) { "id mismatch" }
            assert(item.emoji == emoji) { "emoji mismatch" }
            assert(item.name == name) { "name mismatch" }
        }
    }

    /**
     * Property 5: domain-to-entity mapping — calling insert with a MenuItem
     * produces a MenuItemEntity with identical fields.
     */
    "Property 5 — domain-to-entity mapping preserves all fields on insert" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..30),
            Arb.string(1..5),
            Arb.string(1..30)
        ) { id, emoji, name ->
            var captured: MenuItemEntity? = null
            val dao = object : MenuItemDao {
                override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(emptyList())
                override suspend fun getAllMenuItemsOnce(): List<MenuItemEntity> = emptyList()
                override suspend fun insert(item: MenuItemEntity) { captured = item }
                override suspend fun deleteById(id: String) {}
                override suspend fun deleteAll() {}
            }

            val repo = MenuRepository(dao)
            repo.insert(MenuItem(id = id, emoji = emoji, name = name))

            assert(captured != null) { "insert was never called" }
            assert(captured!!.id == id) { "id mismatch" }
            assert(captured!!.emoji == emoji) { "emoji mismatch" }
            assert(captured!!.name == name) { "name mismatch" }
        }
    }
})
