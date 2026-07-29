package com.example.puntodeventa.data.repository

import com.example.puntodeventa.data.local.CategoryDao
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.CustomizationGroupDao
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionDao
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.ProductDao
import com.example.puntodeventa.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake DAOs for pure JVM (non-instrumented) repository tests.
 *
 * Each fake mimics the SQL semantics of its real Room counterpart:
 * - `@Insert(onConflict = REPLACE)` → upsert keyed on the primary key `id`
 * - `WHERE <fk> = :value`          → list filter
 * - `ORDER BY ...`                 → equivalent Kotlin sort
 * - `DELETE WHERE id = :id`        → no-op when the id is absent
 *
 * They also record call counts so tests can assert that a validation guard
 * short-circuited *before* reaching the DAO.
 *
 * Note: real foreign-key enforcement and true SQL ordering/collation are covered by the
 * instrumented tests in `androidTest/data/local/` (tasks 11.1–11.5). These fakes exist to
 * test the repository's mapping and validation logic without a device.
 */

// ── CategoryDao ───────────────────────────────────────────────────────────────

class FakeCategoryDao : CategoryDao {

    private val rows = MutableStateFlow<List<CategoryEntity>>(emptyList())

    /** Entities handed to [insert], in call order. */
    val insertedEntities = mutableListOf<CategoryEntity>()

    var insertCallCount = 0
        private set
    var deleteCallCount = 0
        private set

    /** Current table contents, for assertions. */
    val currentRows: List<CategoryEntity> get() = rows.value

    /** Seeds rows directly, bypassing [insert] (does not affect call counters). */
    fun seed(entities: List<CategoryEntity>) {
        rows.value = entities
    }

    override suspend fun insert(category: CategoryEntity) {
        insertCallCount++
        insertedEntities += category
        // onConflict = REPLACE, keyed on the primary key
        rows.value = rows.value.filterNot { it.id == category.id } + category
    }

    override fun getCategoriesByMenu(menuId: String): Flow<List<CategoryEntity>> =
        rows.map { all -> all.filter { it.associatedMenuId == menuId } }

    override suspend fun deleteById(id: String) {
        deleteCallCount++
        rows.value = rows.value.filterNot { it.id == id }
    }
}

// ── ProductDao ────────────────────────────────────────────────────────────────

class FakeProductDao : ProductDao {

    private val rows = MutableStateFlow<List<ProductEntity>>(emptyList())

    /** Entities handed to [insert], in call order. */
    val insertedEntities = mutableListOf<ProductEntity>()

    var insertCallCount = 0
        private set
    var deleteCallCount = 0
        private set

    /** When set, [insert] throws this instead of storing the row (simulates an FK violation). */
    var insertError: Throwable? = null

    val currentRows: List<ProductEntity> get() = rows.value

    fun seed(entities: List<ProductEntity>) {
        rows.value = entities
    }

    override suspend fun insert(product: ProductEntity) {
        insertCallCount++
        insertError?.let { throw it }
        insertedEntities += product
        rows.value = rows.value.filterNot { it.id == product.id } + product
    }

    /** Mirrors `ORDER BY name COLLATE NOCASE ASC, id ASC`. */
    override fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>> =
        rows.map { all ->
            all.filter { it.categoryId == categoryId }
                .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
        }

    /** Mirrors `WHERE categoryId = :categoryId AND isActive = 1`. */
    override fun getActiveProductsByCategory(categoryId: String): Flow<List<ProductEntity>> =
        rows.map { all -> all.filter { it.categoryId == categoryId && it.isActive } }

    override suspend fun deleteById(id: String) {
        deleteCallCount++
        rows.value = rows.value.filterNot { it.id == id }
    }
}

// ── CustomizationGroupDao ─────────────────────────────────────────────────────

class FakeCustomizationGroupDao : CustomizationGroupDao {

    private val rows = MutableStateFlow<List<CustomizationGroupEntity>>(emptyList())

    val insertedEntities = mutableListOf<CustomizationGroupEntity>()

    var insertCallCount = 0
        private set

    val currentRows: List<CustomizationGroupEntity> get() = rows.value

    fun seed(entities: List<CustomizationGroupEntity>) {
        rows.value = entities
    }

    override suspend fun insertInternal(group: CustomizationGroupEntity) {
        insertCallCount++
        insertedEntities += group
        rows.value = rows.value.filterNot { it.id == group.id } + group
    }

    override fun getGroupsByProduct(productId: String): Flow<List<CustomizationGroupEntity>> =
        rows.map { all -> all.filter { it.productId == productId } }

    override suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity> =
        rows.value.filter { it.productId == productId }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}

// ── CustomizationOptionDao ────────────────────────────────────────────────────

class FakeCustomizationOptionDao : CustomizationOptionDao {

    private val rows = MutableStateFlow<List<CustomizationOptionEntity>>(emptyList())

    val insertedEntities = mutableListOf<CustomizationOptionEntity>()

    var insertCallCount = 0
        private set

    val currentRows: List<CustomizationOptionEntity> get() = rows.value

    fun seed(entities: List<CustomizationOptionEntity>) {
        rows.value = entities
    }

    override suspend fun insert(option: CustomizationOptionEntity) {
        insertCallCount++
        insertedEntities += option
        rows.value = rows.value.filterNot { it.id == option.id } + option
    }

    override fun getOptionsByGroup(groupId: String): Flow<List<CustomizationOptionEntity>> =
        rows.map { all -> all.filter { it.groupId == groupId } }

    override suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity> =
        rows.value.filter { it.groupId == groupId }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
