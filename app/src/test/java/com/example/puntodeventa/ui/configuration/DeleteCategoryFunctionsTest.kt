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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for the delete category workflow in ConfigurationViewModel.
 *
 * Tests Task 2.3: requestDeleteCategory(), dismissDeleteCategoryDialog(), and confirmDeleteCategory()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteCategoryFunctionsTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    test("requestDeleteCategory sets showDeleteCategoryDialog to true") {
        // Arrange
        val categoryRepository = mockk<CategoryRepository>()
        val productRepository = mockk<ProductRepository>()
        val menuId = "menu-1"

        coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(emptyList())

        val viewModel = ConfigurationViewModel(categoryRepository, productRepository, menuId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.requestDeleteCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            state.showDeleteCategoryDialog shouldBe true
        }
    }

    test("dismissDeleteCategoryDialog sets showDeleteCategoryDialog to false") {
        // Arrange
        val categoryRepository = mockk<CategoryRepository>()
        val productRepository = mockk<ProductRepository>()
        val menuId = "menu-1"

        coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(emptyList())

        val viewModel = ConfigurationViewModel(categoryRepository, productRepository, menuId)
        testDispatcher.scheduler.advanceUntilIdle()

        // First open the dialog
        viewModel.requestDeleteCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.dismissDeleteCategoryDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            state.showDeleteCategoryDialog shouldBe false
        }
    }

    test("confirmDeleteCategory with null selectedCategory does nothing") {
        // Arrange
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val productRepository = mockk<ProductRepository>()
        val menuId = "menu-1"

        coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(emptyList())

        val viewModel = ConfigurationViewModel(categoryRepository, productRepository, menuId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.confirmDeleteCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - deleteById should not be called
        coVerify(exactly = 0) { categoryRepository.deleteById(any()) }
    }

    test("confirmDeleteCategory calls deleteById with correct id and closes dialog") {
        // Arrange
        val categoryRepository = mockk<CategoryRepository>()
        val productRepository = mockk<ProductRepository>()
        val menuId = "menu-1"
        val category = Category(id = "cat-1", name = "Test Category", associatedMenuId = menuId)

        coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
        coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())
        coEvery { categoryRepository.deleteById(category.id) } returns Unit

        val viewModel = ConfigurationViewModel(categoryRepository, productRepository, menuId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.confirmDeleteCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { categoryRepository.deleteById(category.id) }
        viewModel.uiState.test {
            val state = awaitItem()
            state.showDeleteCategoryDialog shouldBe false
        }
    }

    test("confirmDeleteCategory on exception sets error and preserves selectedCategory") {
        // Arrange
        val categoryRepository = mockk<CategoryRepository>()
        val productRepository = mockk<ProductRepository>()
        val menuId = "menu-1"
        val category = Category(id = "cat-1", name = "Test Category", associatedMenuId = menuId)
        val errorMessage = "Database error"

        coEvery { categoryRepository.getCategoriesByMenu(menuId) } returns flowOf(listOf(category))
        coEvery { productRepository.getProductsByCategory(category.id) } returns flowOf(emptyList())
        coEvery { categoryRepository.deleteById(category.id) } throws RuntimeException(errorMessage)

        val viewModel = ConfigurationViewModel(categoryRepository, productRepository, menuId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.confirmDeleteCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            state.error shouldBe errorMessage
            state.selectedCategory shouldBe category
            state.showDeleteCategoryDialog shouldBe false
        }
    }
})
