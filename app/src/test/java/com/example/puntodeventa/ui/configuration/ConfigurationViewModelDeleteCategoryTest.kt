package com.example.puntodeventa.ui.configuration

import app.cash.turbine.test
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.ProductRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for the three new ViewModel functions for category deletion.
 *
 * Tests Properties 4, 5, 6, and 8 from the design document.
 *
 * **Validates: Requirements 2.6, 2.7, 2.8, 2.10, 2.11**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationViewModelDeleteCategoryTest : FunSpec({

    /**
     * Property 4: Cancel preserves all state
     *
     * Call requestDeleteCategory() then dismissDeleteCategoryDialog();
     * assert showDeleteCategoryDialog == false, selectedCategory unchanged,
     * deleteById never called.
     *
     * **Validates: Requirements 2.6**
     */
    test("Property 4: Cancel preserves all state").config(coroutineTestScope = true) {
        runTest {
            // Arrange
            val categoryRepository = mockk<CategoryRepository>(relaxed = true)
            val productRepository = mockk<ProductRepository>()
            val menuId = "menu-1"
            val category = Category(id = "cat-1", name = "Test Category", associatedMenuId = menuId)

            coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
            coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())

            val viewModel = ConfigurationViewModel(categoryRepository, productRepository, mockk(relaxed = true), menuId)

            // Wait for initial state to stabilize using Turbine
            viewModel.uiState.test {
                val initialState = awaitItem()
                // Skip until we get a state with the selected category
                var currentState = initialState
                while (currentState.selectedCategory == null) {
                    currentState = awaitItem()
                }
                currentState.selectedCategory shouldBe category

                // Act - open dialog then dismiss
                viewModel.requestDeleteCategory()
                val dialogOpenState = awaitItem()
                dialogOpenState.showDeleteCategoryDialog shouldBe true

                viewModel.dismissDeleteCategoryDialog()
                val dialogClosedState = awaitItem()

                // Assert
                dialogClosedState.showDeleteCategoryDialog shouldBe false
                dialogClosedState.selectedCategory shouldBe category

                // Verify deleteById was never called
                coVerify(exactly = 0) { categoryRepository.deleteById(any()) }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /**
     * Property 5: Confirm calls deleteById with correct id
     *
     * Seed _selectedCategory with a known category; call confirmDeleteCategory();
     * verify deleteById(category.id) called exactly once.
     *
     * **Validates: Requirements 2.7**
     */
    test("Property 5: Confirm calls deleteById with correct id") {
        runTest {
            // Arrange
            val categoryRepository = mockk<CategoryRepository>()
            val productRepository = mockk<ProductRepository>()
            val menuId = "menu-1"
            val category = Category(id = "cat-abc-123", name = "Test Category", associatedMenuId = menuId)

            coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
            coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())
            coEvery { categoryRepository.deleteById(category.id) } returns Unit

            val viewModel = ConfigurationViewModel(categoryRepository, productRepository, mockk(relaxed = true), menuId)

            // Wait for initialization
            viewModel.uiState.test {
                var currentState = awaitItem()
                while (currentState.selectedCategory == null) {
                    currentState = awaitItem()
                }

                // Act
                viewModel.confirmDeleteCategory()

                // Assert - verify deleteById was called with the correct id exactly once
                coVerify(exactly = 1) { categoryRepository.deleteById(category.id) }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /**
     * Property 6: Successful deletion clears dialog flag
     *
     * Mock deleteById to succeed; call confirmDeleteCategory();
     * assert showDeleteCategoryDialog == false immediately after call.
     *
     * **Validates: Requirements 2.8**
     */
    test("Property 6: Successful deletion clears dialog flag") {
        runTest {
            // Arrange
            val categoryRepository = mockk<CategoryRepository>()
            val productRepository = mockk<ProductRepository>()
            val menuId = "menu-1"
            val category = Category(id = "cat-1", name = "Test Category", associatedMenuId = menuId)

            coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
            coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())
            coEvery { categoryRepository.deleteById(category.id) } returns Unit

            val viewModel = ConfigurationViewModel(categoryRepository, productRepository, mockk(relaxed = true), menuId)

            // Wait for initialization
            viewModel.uiState.test {
                var currentState = awaitItem()
                while (currentState.selectedCategory == null) {
                    currentState = awaitItem()
                }

                // First open the dialog
                viewModel.requestDeleteCategory()
                var dialogOpenState = awaitItem()
                while (!dialogOpenState.showDeleteCategoryDialog) {
                    dialogOpenState = awaitItem()
                }
                dialogOpenState.showDeleteCategoryDialog shouldBe true

                // Act - confirm deletion
                viewModel.confirmDeleteCategory()

                // The dialog should be closed (this happens synchronously before the coroutine)
                // Wait for the state change
                var postDeleteState = awaitItem()
                while (postDeleteState.showDeleteCategoryDialog) {
                    postDeleteState = awaitItem()
                }

                // Assert - dialog should be closed
                postDeleteState.showDeleteCategoryDialog shouldBe false

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /**
     * Property 8: Error handling preserves selectedCategory
     *
     * Mock deleteById to throw RuntimeException("error msg");
     * call confirmDeleteCategory();
     * assert selectedCategory unchanged, error == "error msg",
     * showDeleteCategoryDialog == false.
     *
     * **Validates: Requirements 2.10, 2.11**
     */
    test("Property 8: Error handling preserves selectedCategory and sets error field").config(coroutineTestScope = true) {
        runTest {
            // Arrange
            val categoryRepository = mockk<CategoryRepository>()
            val productRepository = mockk<ProductRepository>()
            val menuId = "menu-1"
            val category = Category(id = "cat-1", name = "Test Category", associatedMenuId = menuId)
            val errorMessage = "Database connection failed"

            coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
            coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())
            coEvery { categoryRepository.deleteById(category.id) } throws RuntimeException(errorMessage)

            val viewModel = ConfigurationViewModel(categoryRepository, productRepository, mockk(relaxed = true), menuId)

            // Wait for initial state to stabilize using Turbine
            viewModel.uiState.test {
                val initialState = awaitItem()
                // Skip until we get a state with the selected category
                var currentState = initialState
                while (currentState.selectedCategory == null) {
                    currentState = awaitItem()
                }
                currentState.selectedCategory shouldBe category

                // Act
                viewModel.confirmDeleteCategory()

                // Wait for the error state
                var errorState = awaitItem()
                while (errorState.error == null) {
                    errorState = awaitItem()
                }

                // Assert
                errorState.selectedCategory shouldBe category  // Preserved
                errorState.error shouldBe errorMessage         // Error set
                errorState.showDeleteCategoryDialog shouldBe false  // Dialog closed

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
})
