package com.example.puntodeventa.ui.newproduct

import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CategoryDao
import com.example.puntodeventa.data.local.CategoryEntity
import com.example.puntodeventa.data.local.CustomizationGroupDao
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionDao
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.MenuItemDao
import com.example.puntodeventa.data.local.MenuItemEntity
import com.example.puntodeventa.data.local.ProductDao
import com.example.puntodeventa.data.local.ProductEntity
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// ── Fake DAOs ─────────────────────────────────────────────────────────────────

private class FakeMenuItemDao : MenuItemDao {
    private val _flow = MutableStateFlow<List<MenuItemEntity>>(emptyList())
    override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = _flow.asStateFlow()
    override suspend fun getAllMenuItemsOnce(): List<MenuItemEntity> = _flow.value
    override suspend fun insert(item: MenuItemEntity) { /* no-op */ }
    override suspend fun deleteById(id: String) { /* no-op */ }
    override suspend fun deleteAll() { /* no-op */ }
}

private class FakeCategoryDao : CategoryDao {
    override fun getCategoriesByMenu(menuId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getCategoriesByMenuOnce(menuId: String): List<CategoryEntity> = emptyList()
    override suspend fun insert(category: CategoryEntity) { /* no-op */ }
    override suspend fun deleteById(id: String) { /* no-op */ }
    override suspend fun deleteAll() { /* no-op */ }
}

private class FakeProductDao : ProductDao {
    override suspend fun insert(product: ProductEntity) { /* no-op */ }
    override fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>> = flowOf(emptyList())
    override suspend fun getProductsByCategoryOnce(categoryId: String): List<ProductEntity> = emptyList()
    override fun getActiveProductsByCategory(categoryId: String): Flow<List<ProductEntity>> = flowOf(emptyList())
    override suspend fun deleteById(id: String) { /* no-op */ }
    override suspend fun deleteAll() { /* no-op */ }
}

private class FakeCustomizationGroupDao : CustomizationGroupDao {
    override suspend fun insertInternal(group: CustomizationGroupEntity) { /* no-op */ }
    override fun getGroupsByProduct(productId: String): Flow<List<CustomizationGroupEntity>> = flowOf(emptyList())
    override suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity> = emptyList()
    override suspend fun deleteById(id: String) { /* no-op */ }
    override suspend fun deleteAll() { /* no-op */ }
}

private class FakeCustomizationOptionDao : CustomizationOptionDao {
    override suspend fun insert(option: CustomizationOptionEntity) { /* no-op */ }
    override fun getOptionsByGroup(groupId: String): Flow<List<CustomizationOptionEntity>> = flowOf(emptyList())
    override suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity> = emptyList()
    override suspend fun deleteById(id: String) { /* no-op */ }
    override suspend fun deleteAll() { /* no-op */ }
}

// ── Helper: build a ViewModel under a controlled TestScope ───────────────────

/**
 * Creates a [NewProductViewModel] wired to fake repositories.
 * The [AppDatabase] is a relaxed MockK mock — never exercised in group/option tests
 * (no `save()` or `loadForEdit()` call), but required as non-null by the constructor.
 */
private fun makeViewModel(scope: TestScope): NewProductViewModel {
    val fakeDb       = mockk<AppDatabase>(relaxed = true)
    val menuRepo     = MenuRepository(FakeMenuItemDao())
    val categoryRepo = CategoryRepository(FakeCategoryDao())
    val productRepo  = ProductRepository(FakeProductDao(), FakeCustomizationGroupDao(), FakeCustomizationOptionDao(), fakeDb)

    return NewProductViewModel(
        productRepository  = productRepo,
        categoryRepository = categoryRepo,
        menuRepository     = menuRepo,
        database           = fakeDb
    )
}

// ── Property-Based Tests ──────────────────────────────────────────────────────

class NewProductViewModelPropertyTest : StringSpec({

    /**
     * Property 1: addGroup size invariant
     *
     * For N ≥ 0 calls to `addGroup()` from empty state, `groups.size == N`
     *
     * Validates: Requirements 11.1
     */
    "Property 1 - addGroup size invariant: N calls produce groups.size == N" {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            checkAll(PropTestConfig(iterations = 20), Arb.int(0..10)) { n ->
                val scope = TestScope(dispatcher)
                val vm = makeViewModel(scope)

                // Start from empty state (default)
                assert(vm.uiState.value.groups.isEmpty()) {
                    "Expected empty groups on init, got ${vm.uiState.value.groups.size}"
                }

                // Call addGroup() exactly N times
                repeat(n) { vm.addGroup() }

                // Assert size invariant
                val actualSize = vm.uiState.value.groups.size
                assert(actualSize == n) {
                    "Expected groups.size == $n after $n addGroup() calls, got $actualSize"
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    /**
     * Property 2: addGroup / removeGroup inverse
     *
     * Add N groups then remove all in descending index order → `groups` is empty
     *
     * Validates: Requirements 11.2
     */
    "Property 2 - addGroup/removeGroup inverse: remove all in descending order leaves empty list" {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            checkAll(PropTestConfig(iterations = 20), Arb.int(0..10)) { n ->
                val scope = TestScope(dispatcher)
                val vm = makeViewModel(scope)

                // Add N groups
                repeat(n) { vm.addGroup() }
                assert(vm.uiState.value.groups.size == n) {
                    "Setup failed: expected $n groups, got ${vm.uiState.value.groups.size}"
                }

                // Remove all in descending index order: N-1 down to 0
                for (i in (n - 1) downTo 0) {
                    vm.removeGroup(i)
                }

                // Assert list is now empty
                val remaining = vm.uiState.value.groups.size
                assert(remaining == 0) {
                    "Expected empty groups after removing all $n groups in descending order, got $remaining"
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    /**
     * Property 3: GroupDraft draftId global uniqueness
     *
     * After any sequence of `addGroup()` calls, all `draftId` values are pairwise distinct.
     *
     * Validates: Requirements 11.3, 10.1
     */
    "Property 3 - GroupDraft draftId uniqueness: all draftIds are pairwise distinct after N addGroup() calls" {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            checkAll(PropTestConfig(iterations = 20), Arb.int(0..10)) { n ->
                val scope = TestScope(dispatcher)
                val vm = makeViewModel(scope)

                // Add N groups
                repeat(n) { vm.addGroup() }

                val groups = vm.uiState.value.groups
                val draftIds = groups.map { it.draftId }
                val uniqueIds = draftIds.toSet()

                assert(uniqueIds.size == draftIds.size) {
                    "Expected all $n draftIds to be distinct, but found duplicates. " +
                    "Unique count: ${uniqueIds.size}, total count: ${draftIds.size}. " +
                    "IDs: $draftIds"
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
})
