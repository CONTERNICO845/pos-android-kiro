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
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

// ── Fake DAOs (same no-op pattern as NewProductViewModelPropertyTest) ─────────

private class FakeMenuItemDao2 : MenuItemDao {
    override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(emptyList())
    override suspend fun getAllMenuItemsOnce(): List<MenuItemEntity> = emptyList()
    override suspend fun insert(item: MenuItemEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteAll() {}
}

private class FakeCategoryDao2 : CategoryDao {
    override fun getCategoriesByMenu(menuId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getCategoriesByMenuOnce(menuId: String): List<CategoryEntity> = emptyList()
    override suspend fun insert(category: CategoryEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteAll() {}
}

private class FakeProductDao2 : ProductDao {
    override suspend fun insert(product: ProductEntity) {}
    override fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>> = flowOf(emptyList())
    override suspend fun getProductsByCategoryOnce(categoryId: String): List<ProductEntity> = emptyList()
    override fun getActiveProductsByCategory(categoryId: String): Flow<List<ProductEntity>> = flowOf(emptyList())
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteAll() {}
}

private class FakeCustomizationGroupDao2 : CustomizationGroupDao {
    override suspend fun insertInternal(group: CustomizationGroupEntity) {}
    override fun getGroupsByProduct(productId: String): Flow<List<CustomizationGroupEntity>> = flowOf(emptyList())
    override suspend fun getGroupsByProductOnce(productId: String): List<CustomizationGroupEntity> = emptyList()
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteAll() {}
}

private class FakeCustomizationOptionDao2 : CustomizationOptionDao {
    override suspend fun insert(option: CustomizationOptionEntity) {}
    override fun getOptionsByGroup(groupId: String): Flow<List<CustomizationOptionEntity>> = flowOf(emptyList())
    override suspend fun getOptionsByGroupOnce(groupId: String): List<CustomizationOptionEntity> = emptyList()
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteAll() {}
}

// ── Helper: build a ViewModel under a controlled TestScope ───────────────────

/**
 * Creates a [NewProductViewModel] wired to fake repositories.
 * [AppDatabase] is a relaxed MockK mock because these tests never call [NewProductViewModel.save].
 */
private fun buildViewModel2(): NewProductViewModel {
    val fakeDb       = mockk<AppDatabase>(relaxed = true)
    val menuRepo     = MenuRepository(FakeMenuItemDao2())
    val categoryRepo = CategoryRepository(FakeCategoryDao2())
    val productRepo  = ProductRepository(FakeProductDao2(), FakeCustomizationGroupDao2(), FakeCustomizationOptionDao2(), fakeDb)
    return NewProductViewModel(
        productRepository  = productRepo,
        categoryRepository = categoryRepo,
        menuRepository     = menuRepo,
        database           = fakeDb
    )
}

// ── Property tests ────────────────────────────────────────────────────────────

class NewProductViewModelPropertyTest2 : StringSpec({

    /**
     * Property 4: addOption size invariant
     *
     * For M ≥ 0 calls to `addOption(groupIndex)` on a newly created group,
     * `group.options.size == M + 1`
     * (the group starts with 1 option by default).
     *
     * **Validates: Requirements 11.4**
     */
    "Property 4 — addOption size invariant: M extra addOption calls yield M+1 options (Validates: Requirements 11.4)" {
        runTest(UnconfinedTestDispatcher()) {
            forAll(PropTestConfig(iterations = 20), Arb.int(0..10)) { m ->
                val vm = buildViewModel2()
                vm.addGroup()                  // groups[0] starts with 1 option

                repeat(m) { vm.addOption(0) }  // add M more options

                val options = vm.uiState.value.groups[0].options
                options.size == m + 1
            }
        }
    }

    /**
     * Property 5: updateGroupName preserves all draftIds
     *
     * Calling `updateGroupName(index, newName)` leaves every GroupDraft.draftId unchanged.
     *
     * **Validates: Requirements 11.5, 10.4**
     */
    "Property 5 — updateGroupName preserves all GroupDraft draftIds (Validates: Requirements 11.5, 10.4)" {
        runTest(UnconfinedTestDispatcher()) {
            val groupCount = 5
            forAll(
                PropTestConfig(iterations = 20),
                Arb.int(0 until groupCount),   // targetIndex
                Arb.string(0..120)             // newName (arbitrary, may be blank)
            ) { targetIndex, newName ->
                val vm = buildViewModel2()
                repeat(groupCount) { vm.addGroup() }

                val before = vm.uiState.value.groups.map { it.draftId }

                vm.updateGroupName(targetIndex, newName)

                val after = vm.uiState.value.groups.map { it.draftId }
                before == after
            }
        }
    }

    /**
     * Property 6: updateOptionName preserves all option draftIds within group
     *
     * Calling `updateOptionName(g, o, name)` leaves every OptionDraft.draftId in the group unchanged.
     *
     * **Validates: Requirements 11.6, 10.5**
     */
    "Property 6 — updateOptionName preserves all OptionDraft draftIds within group (Validates: Requirements 11.6, 10.5)" {
        runTest(UnconfinedTestDispatcher()) {
            val optionCount = 5
            forAll(
                PropTestConfig(iterations = 20),
                Arb.int(0 until optionCount),  // targetOptionIndex
                Arb.string(0..120)             // newName (arbitrary, may be blank)
            ) { targetOptionIndex, newName ->
                val vm = buildViewModel2()
                vm.addGroup()  // groups[0] starts with 1 option

                // Add more options so we have optionCount total
                repeat(optionCount - 1) { vm.addOption(0) }

                val before = vm.uiState.value.groups[0].options.map { it.draftId }

                vm.updateOptionName(0, targetOptionIndex, newName)

                val after = vm.uiState.value.groups[0].options.map { it.draftId }
                before == after
            }
        }
    }

    /**
     * Property 7: OptionDraft draftId uniqueness within a group
     *
     * After any sequence of `addOption(groupIndex)` calls, all draftId values within
     * that group are pairwise distinct.
     *
     * **Validates: Requirements 11.7, 10.3**
     */
    "Property 7 — OptionDraft draftId uniqueness within a group (Validates: Requirements 11.7, 10.3)" {
        runTest(UnconfinedTestDispatcher()) {
            forAll(PropTestConfig(iterations = 20), Arb.int(0..10)) { m ->
                val vm = buildViewModel2()
                vm.addGroup()  // groups[0] starts with 1 option

                repeat(m) { vm.addOption(0) }  // add M more options

                val options = vm.uiState.value.groups[0].options
                val draftIds = options.map { it.draftId }
                val uniqueIds = draftIds.toSet()
                uniqueIds.size == draftIds.size
            }
        }
    }
})
