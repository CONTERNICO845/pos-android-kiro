package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.SelectionType
import com.example.puntodeventa.data.model.Product
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.string
import io.kotest.property.assume
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.flow.first

/**
 * Property-based tests for [ProductRepository] (tasks 9.2, 9.3, 9.4).
 *
 * Covers:
 * - **Property 1**: Product mapping round-trip
 * - **Property 8**: Invalid `selectionType` is rejected
 * - **Property 9**: Negative `extraPrice` is rejected
 *
 * The `AppDatabase` constructor argument is only used by `deepCopyProduct` (out of scope for
 * Phase 1), so it is supplied as a relaxed mock and never touched by these tests.
 *
 * **Validates: Requirements 6.6, 9.1, 3.3, 9.8, 4.3**
 */
class ProductRepositoryPropertyTest : StringSpec({

    fun newRepository(
        productDao: FakeProductDao = FakeProductDao(),
        groupDao: FakeCustomizationGroupDao = FakeCustomizationGroupDao(),
        optionDao: FakeCustomizationOptionDao = FakeCustomizationOptionDao()
    ) = ProductRepository(
        productDao = productDao,
        groupDao   = groupDao,
        optionDao  = optionDao,
        database   = mockk<AppDatabase>(relaxed = true)
    )

    // ── Property 1: Product mapping round-trip ───────────────────────────────

    /**
     * `ProductEntity.toDomain()` / `Product.toEntity()` are private, so the round-trip runs
     * through the public surface: insert() maps domain → entity, and getProductsByCategory()
     * maps entity → domain. Both mappers must be lossless across all 7 fields.
     */
    "Property 1: Product mapping round-trip is lossless through insert + query" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..36),
            Arb.string(1..8),
            Arb.string(1..120),
            Arb.string(1..500),
            Arb.double(0.0, 9_999.99),
            Arb.boolean(),
            Arb.string(1..36)
        ) { id, emoji, name, description, basePrice, isActive, categoryId ->
            val productDao = FakeProductDao()
            val repository = newRepository(productDao = productDao)
            val original = Product(
                id          = id,
                emoji       = emoji,
                name        = name,
                description = description,
                basePrice   = basePrice,
                isActive    = isActive,
                categoryId  = categoryId
            )

            repository.insert(original)
            val roundTripped = repository.getProductsByCategory(categoryId).first()

            roundTripped shouldBe listOf(original)
        }
    }

    "Property 1 (corollary): the entity written to the DAO preserves all 7 domain fields" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..36),
            Arb.string(1..8),
            Arb.string(1..120),
            Arb.string(1..500),
            Arb.double(0.0, 9_999.99),
            Arb.boolean(),
            Arb.string(1..36)
        ) { id, emoji, name, description, basePrice, isActive, categoryId ->
            val productDao = FakeProductDao()
            val repository = newRepository(productDao = productDao)

            repository.insert(
                Product(id, emoji, name, description, basePrice, isActive, categoryId)
            )

            val entity = productDao.insertedEntities.single()
            entity.id shouldBe id
            entity.emoji shouldBe emoji
            entity.name shouldBe name
            entity.description shouldBe description
            entity.basePrice shouldBe basePrice
            entity.isActive shouldBe isActive
            entity.categoryId shouldBe categoryId
        }
    }

    // ── Property 8: Invalid selectionType is rejected ─────────────────────────

    "Property 8: insertGroup rejects any selectionType outside the declared values" {
        val validValues = SelectionType.entries.map { it.value }.toSet()

        checkAll(PropTestConfig(iterations = 100), Arb.string()) { raw ->
            assume(raw !in validValues)

            val groupDao = FakeCustomizationGroupDao()
            val repository = newRepository(groupDao = groupDao)
            val group = CustomizationGroupEntity(
                id            = "g1",
                productId     = "p1",
                groupName     = "Ingredientes",
                selectionType = raw
            )

            shouldThrow<IllegalArgumentException> { repository.insertGroup(group) }

            // The guard must short-circuit before the DAO is reached.
            groupDao.insertCallCount shouldBe 0
            groupDao.currentRows.shouldBeEmpty()
        }
    }

    "Property 8 (positive case): insertGroup accepts every declared selectionType" {
        SelectionType.entries.forEach { selectionType ->
            val groupDao = FakeCustomizationGroupDao()
            val repository = newRepository(groupDao = groupDao)
            val group = CustomizationGroupEntity(
                id            = "g-${selectionType.value}",
                productId     = "p1",
                groupName     = "Ingredientes",
                selectionType = selectionType.value
            )

            repository.insertGroup(group)

            groupDao.insertCallCount shouldBe 1
            groupDao.currentRows shouldBe listOf(group)
        }
    }

    // ── Property 9: Negative extraPrice is rejected ───────────────────────────

    /**
     * Note: the task text suggests `Arb.double(Double.MIN_VALUE..-0.001)`, but in Kotlin
     * `Double.MIN_VALUE` is the smallest *positive* value (4.9E-324), which would make that
     * range empty. The intent — arbitrary strictly negative prices — is expressed here with an
     * explicit negative range, plus the boundary cases asserted separately below.
     */
    "Property 9: insertOption rejects any strictly negative extraPrice" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.double(-1_000_000.0, -0.001)
        ) { negativePrice ->
            assume(negativePrice < 0.0)

            val optionDao = FakeCustomizationOptionDao()
            val repository = newRepository(optionDao = optionDao)
            val option = CustomizationOptionEntity(
                id         = "o1",
                groupId    = "g1",
                optionName = "Extra queso",
                extraPrice = negativePrice
            )

            shouldThrow<IllegalArgumentException> { repository.insertOption(option) }

            // The guard must short-circuit before the DAO is reached.
            optionDao.insertCallCount shouldBe 0
            optionDao.currentRows.shouldBeEmpty()
        }
    }

    "Property 9 (boundary): insertOption accepts extraPrice == 0.0 and any positive value" {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.double(0.0, 9_999.99)
        ) { nonNegativePrice ->
            assume(nonNegativePrice >= 0.0)

            val optionDao = FakeCustomizationOptionDao()
            val repository = newRepository(optionDao = optionDao)
            val option = CustomizationOptionEntity(
                id         = "o1",
                groupId    = "g1",
                optionName = "Extra queso",
                extraPrice = nonNegativePrice
            )

            repository.insertOption(option)

            optionDao.insertCallCount shouldBe 1
            optionDao.currentRows shouldBe listOf(option)
        }
    }
})
