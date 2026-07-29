package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.model.Category
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first

/**
 * Property-based test for **Property 2: Category mapping round-trip** (task 8.2).
 *
 * `CategoryEntity.toDomain()` and `Category.toEntity()` are private to [CategoryRepository],
 * so the round-trip is exercised through the public surface instead:
 *
 * ```
 * Category --insert()--> CategoryEntity (stored) --getCategoriesByMenu()--> Category
 * ```
 *
 * That path traverses both mappers, so a lossy or transposed field in either one fails the test.
 *
 * **Validates: Requirements 7.4, 9.2**
 */
class CategoryRepositoryPropertyTest : StringSpec({

    "Property 2: Category mapping round-trip is lossless through insert + query" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..36),
            Arb.string(1..120),
            Arb.string(1..36)
        ) { id, name, menuId ->
            val dao = FakeCategoryDao()
            val repository = CategoryRepository(dao)
            val original = Category(id = id, name = name, associatedMenuId = menuId)

            repository.insert(original)
            val roundTripped = repository.getCategoriesByMenu(menuId).first()

            roundTripped shouldBe listOf(original)
        }
    }

    "Property 2 (corollary): the entity written to the DAO preserves every domain field" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..36),
            Arb.string(1..120),
            Arb.string(1..36)
        ) { id, name, menuId ->
            val dao = FakeCategoryDao()
            val repository = CategoryRepository(dao)

            repository.insert(Category(id = id, name = name, associatedMenuId = menuId))

            val entity = dao.insertedEntities.single()
            entity.id shouldBe id
            entity.name shouldBe name
            entity.associatedMenuId shouldBe menuId
        }
    }
})
