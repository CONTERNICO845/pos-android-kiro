package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.model.Product
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.first

/**
 * Unit tests for [ProductRepository] — FK propagation and active-product filter (task 9.5).
 *
 * Uses the in-memory fakes in `FakeCatalogDaos.kt`; no Room, no device.
 *
 * _Requirements: 6.2, 6.5_
 */
class ProductRepositoryTest : StringSpec({

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

    fun product(
        id: String,
        name: String = "Producto",
        isActive: Boolean = true,
        categoryId: String = "cat-1"
    ) = Product(
        id          = id,
        emoji       = "🌮",
        name        = name,
        description = "",
        basePrice   = 10.0,
        isActive    = isActive,
        categoryId  = categoryId
    )

    // ── Requirement 6.5: FK violations propagate, they are not swallowed ──────

    /**
     * A real FK violation surfaces as `android.database.sqlite.SQLiteConstraintException`, which
     * cannot be instantiated in a JVM unit test (the android.jar stub throws on every call). The
     * behaviour under test here is `ProductRepository.insert`'s *lack of a catch block*: whatever
     * the DAO throws must reach the caller unchanged.
     *
     * The genuine `SQLiteConstraintException` path is covered on-device by
     * `androidTest/data/local/CascadeDeletionTest.insertProduct_withUnknownCategoryId_throwsConstraintException`.
     */
    "insert propagates a DAO constraint failure to the caller without swallowing it" {
        val productDao = FakeProductDao()
        val expected = FakeConstraintViolation("FOREIGN KEY constraint failed (code 787)")
        productDao.insertError = expected
        val repository = newRepository(productDao = productDao)

        val thrown = shouldThrow<FakeConstraintViolation> {
            repository.insert(product(id = "p1", categoryId = "missing-category"))
        }

        // Same instance, same message: no wrapping, no substitution.
        thrown shouldBe expected
        thrown.message shouldBe "FOREIGN KEY constraint failed (code 787)"
    }

    "insert does not persist the row when the DAO rejects it" {
        val productDao = FakeProductDao()
        productDao.insertError = FakeConstraintViolation("FOREIGN KEY constraint failed")
        val repository = newRepository(productDao = productDao)

        shouldThrow<FakeConstraintViolation> {
            repository.insert(product(id = "p1", categoryId = "missing-category"))
        }

        productDao.currentRows.shouldBeEmpty()
        repository.getProductsByCategory("missing-category").first().shouldBeEmpty()
    }

    "insert succeeds normally once the DAO stops rejecting rows" {
        val productDao = FakeProductDao()
        productDao.insertError = FakeConstraintViolation("FOREIGN KEY constraint failed")
        val repository = newRepository(productDao = productDao)

        shouldThrow<FakeConstraintViolation> { repository.insert(product(id = "p1")) }

        productDao.insertError = null
        repository.insert(product(id = "p1"))

        repository.getProductsByCategory("cat-1").first().map { it.id } shouldBe listOf("p1")
    }

    // ── Requirement 6.2: active-product filter ───────────────────────────────

    "getActiveProductsByCategory returns only the active products" {
        val productDao = FakeProductDao()
        productDao.seed(
            listOf(
                ProductEntity("p1", "🌮", "Taco",     "", 10.0, true,  "cat-1"),
                ProductEntity("p2", "🥤", "Refresco", "", 20.0, false, "cat-1"),
                ProductEntity("p3", "🍰", "Pastel",   "", 30.0, true,  "cat-1"),
                ProductEntity("p4", "🍺", "Cerveza",  "", 40.0, false, "cat-1")
            )
        )
        val repository = newRepository(productDao = productDao)

        val active = repository.getActiveProductsByCategory("cat-1").first()

        active.map { it.id } shouldBe listOf("p1", "p3")
        active.all { it.isActive } shouldBe true
    }

    "getActiveProductsByCategory ignores active products from other categories" {
        val productDao = FakeProductDao()
        productDao.seed(
            listOf(
                ProductEntity("p1", "🌮", "Taco",  "", 10.0, true, "cat-1"),
                ProductEntity("p2", "🍕", "Pizza", "", 20.0, true, "cat-2")
            )
        )
        val repository = newRepository(productDao = productDao)

        repository.getActiveProductsByCategory("cat-1").first().map { it.id } shouldBe listOf("p1")
        repository.getActiveProductsByCategory("cat-2").first().map { it.id } shouldBe listOf("p2")
    }

    "getActiveProductsByCategory emits an empty list when every product is inactive" {
        val productDao = FakeProductDao()
        productDao.seed(
            listOf(
                ProductEntity("p1", "🌮", "Taco",     "", 10.0, false, "cat-1"),
                ProductEntity("p2", "🥤", "Refresco", "", 20.0, false, "cat-1")
            )
        )
        val repository = newRepository(productDao = productDao)

        repository.getActiveProductsByCategory("cat-1").first().shouldBeEmpty()
        // The unfiltered query still sees both rows.
        repository.getProductsByCategory("cat-1").first().size shouldBe 2
    }

    "getProductsByCategory returns active and inactive products alike" {
        val productDao = FakeProductDao()
        productDao.seed(
            listOf(
                ProductEntity("p1", "🌮", "Taco",     "", 10.0, true,  "cat-1"),
                ProductEntity("p2", "🥤", "Refresco", "", 20.0, false, "cat-1")
            )
        )
        val repository = newRepository(productDao = productDao)

        val all = repository.getProductsByCategory("cat-1").first()

        // The query is `ORDER BY name COLLATE NOCASE ASC, id ASC` (spec 07), so the result is
        // sorted by name — "Refresco" before "Taco" — not by insertion order.
        all.map { it.name } shouldBe listOf("Refresco", "Taco")
        all.map { it.isActive } shouldBe listOf(false, true)
    }

    // ── deleteById delegation ────────────────────────────────────────────────

    "deleteById with a non-existent id is a no-op" {
        val productDao = FakeProductDao()
        productDao.seed(listOf(ProductEntity("p1", "🌮", "Taco", "", 10.0, true, "cat-1")))
        val repository = newRepository(productDao = productDao)

        repository.deleteById("nope")   // must not throw

        productDao.deleteCallCount shouldBe 1
        productDao.currentRows.map { it.id } shouldBe listOf("p1")
    }
})

/**
 * Stand-in for `android.database.sqlite.SQLiteConstraintException`, which cannot be constructed
 * in JVM unit tests because the stubbed android.jar throws on every method call.
 */
class FakeConstraintViolation(message: String) : RuntimeException(message)
