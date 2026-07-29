package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.model.Category
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first

/**
 * Unit tests for [CategoryRepository] query delegation and no-op delete (task 8.3).
 *
 * Uses [FakeCategoryDao] — no Room, no device.
 *
 * _Requirements: 7.1, 7.3_
 */
class CategoryRepositoryTest : StringSpec({

    // ── getCategoriesByMenu ──────────────────────────────────────────────────

    "getCategoriesByMenu maps a CategoryEntity list to a Category list, preserving order" {
        val dao = FakeCategoryDao()
        dao.seed(
            listOf(
                CategoryEntity(id = "c1", name = "Tacos",    associatedMenuId = "m1"),
                CategoryEntity(id = "c2", name = "Bebidas",  associatedMenuId = "m1"),
                CategoryEntity(id = "c3", name = "Postres",  associatedMenuId = "m1")
            )
        )
        val repository = CategoryRepository(dao)

        repository.getCategoriesByMenu("m1").first() shouldBe listOf(
            Category(id = "c1", name = "Tacos",   associatedMenuId = "m1"),
            Category(id = "c2", name = "Bebidas", associatedMenuId = "m1"),
            Category(id = "c3", name = "Postres", associatedMenuId = "m1")
        )
    }

    "getCategoriesByMenu returns only the categories belonging to the requested menu" {
        val dao = FakeCategoryDao()
        dao.seed(
            listOf(
                CategoryEntity(id = "c1", name = "Tacos",   associatedMenuId = "m1"),
                CategoryEntity(id = "c2", name = "Pizzas",  associatedMenuId = "m2"),
                CategoryEntity(id = "c3", name = "Bebidas", associatedMenuId = "m1")
            )
        )
        val repository = CategoryRepository(dao)

        repository.getCategoriesByMenu("m1").first().map { it.id } shouldBe listOf("c1", "c3")
        repository.getCategoriesByMenu("m2").first().map { it.id } shouldBe listOf("c2")
    }

    "getCategoriesByMenu emits an empty list when the menu has no categories" {
        val repository = CategoryRepository(FakeCategoryDao())

        repository.getCategoriesByMenu("does-not-exist").first().shouldBeEmpty()
    }

    // ── insert ───────────────────────────────────────────────────────────────

    "insert delegates to the DAO exactly once per call" {
        val dao = FakeCategoryDao()
        val repository = CategoryRepository(dao)

        repository.insert(Category(id = "c1", name = "Tacos", associatedMenuId = "m1"))

        dao.insertCallCount shouldBe 1
        dao.currentRows shouldBe listOf(CategoryEntity("c1", "Tacos", "m1"))
    }

    "re-inserting the same id replaces the existing row instead of duplicating it" {
        val dao = FakeCategoryDao()
        val repository = CategoryRepository(dao)

        repository.insert(Category(id = "c1", name = "Tacos", associatedMenuId = "m1"))
        repository.insert(Category(id = "c1", name = "Tacos al pastor", associatedMenuId = "m1"))

        repository.getCategoriesByMenu("m1").first() shouldBe
            listOf(Category(id = "c1", name = "Tacos al pastor", associatedMenuId = "m1"))
    }

    // ── deleteById ───────────────────────────────────────────────────────────

    "deleteById removes the matching row and leaves the rest untouched" {
        val dao = FakeCategoryDao()
        dao.seed(
            listOf(
                CategoryEntity(id = "c1", name = "Tacos",   associatedMenuId = "m1"),
                CategoryEntity(id = "c2", name = "Bebidas", associatedMenuId = "m1")
            )
        )
        val repository = CategoryRepository(dao)

        repository.deleteById("c1")

        repository.getCategoriesByMenu("m1").first().map { it.id } shouldBe listOf("c2")
    }

    "deleteById with a non-existent id is a no-op: no exception and no rows affected" {
        val dao = FakeCategoryDao()
        dao.seed(listOf(CategoryEntity(id = "c1", name = "Tacos", associatedMenuId = "m1")))
        val repository = CategoryRepository(dao)

        repository.deleteById("nope")   // must not throw

        dao.deleteCallCount shouldBe 1
        dao.currentRows shouldBe listOf(CategoryEntity(id = "c1", name = "Tacos", associatedMenuId = "m1"))
    }

    "deleteById on an empty table is a no-op" {
        val dao = FakeCategoryDao()
        val repository = CategoryRepository(dao)

        repository.deleteById("anything")   // must not throw

        dao.currentRows.shouldBeEmpty()
    }
})
