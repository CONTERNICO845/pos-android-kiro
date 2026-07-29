package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.local.OrderItemCustomizationEntity
import com.example.puntodeventa.data.local.OrderItemEntity
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.ProductRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for [PosViewModel].
 *
 * Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 6.5, 6.6, 6.7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()
    val menuId = "test-menu-id"

    // -- Test data ---------------------------------------------------------------

    val category1 = Category(id = "cat-1", name = "Bebidas", associatedMenuId = menuId)
    val category2 = Category(id = "cat-2", name = "Alimentos", associatedMenuId = menuId)

    val product1 = Product(
        id = "prod-1", emoji = "B", name = "Cerveza",
        description = "Beer", basePrice = 45.0, isActive = true, categoryId = "cat-1"
    )
    val product2 = Product(
        id = "prod-2", emoji = "S", name = "Refresco",
        description = "Soda", basePrice = 25.0, isActive = true, categoryId = "cat-1"
    )
    val product3 = Product(
        id = "prod-3", emoji = "T", name = "Taco",
        description = "Taco", basePrice = 35.0, isActive = true, categoryId = "cat-2"
    )

    val sampleCartItem = CartItem(
        id = "item-1",
        productId = "prod-1",
        productName = "Cerveza",
        emoji = "B",
        basePrice = 45.0,
        quantity = 2,
        selectedCustomizations = emptyList(),
        extraNotes = "",
        totalPrice = 90.0
    )

    val sampleCartItem2 = CartItem(
        id = "item-2",
        productId = "prod-3",
        productName = "Taco",
        emoji = "T",
        basePrice = 35.0,
        quantity = 1,
        selectedCustomizations = listOf(
            SelectedCustomization(optionId = "opt-1", optionName = "Extra queso", extraPrice = 10.0)
        ),
        extraNotes = "Sin cebolla",
        totalPrice = 45.0
    )

    // -- Mocks -------------------------------------------------------------------

    lateinit var categoryRepository: CategoryRepository
    lateinit var productRepository: ProductRepository
    lateinit var orderRepository: OrderRepository
    lateinit var categoriesFlow: MutableStateFlow<List<Category>>
    lateinit var productsFlowCat1: MutableStateFlow<List<Product>>
    lateinit var productsFlowCat2: MutableStateFlow<List<Product>>

    fun createViewModel(): PosViewModel {
        return PosViewModel(categoryRepository, productRepository, orderRepository, menuId)
    }

    beforeEach {
        Dispatchers.setMain(testDispatcher)

        categoriesFlow = MutableStateFlow(listOf(category1, category2))
        productsFlowCat1 = MutableStateFlow(listOf(product1, product2))
        productsFlowCat2 = MutableStateFlow(listOf(product3))

        categoryRepository = mockk()
        productRepository = mockk()
        orderRepository = mockk()

        every { categoryRepository.getCategoriesByMenu(menuId) } returns categoriesFlow
        every { productRepository.getActiveProductsByCategory("cat-1") } returns productsFlowCat1
        every { productRepository.getActiveProductsByCategory("cat-2") } returns productsFlowCat2
    }

    afterEach {
        Dispatchers.resetMain()
    }

    // -- Initialization tests ----------------------------------------------------

    test("initialization: cart is empty on start") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            // Start collecting to activate the WhileSubscribed stateIn
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems.shouldBeEmpty()
            state.cartTotal shouldBeExactly 0.0
        }
    }

    test("initialization: categories are loaded from repository") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.categories shouldHaveSize 2
            state.categories shouldContainExactly listOf(category1, category2)
        }
    }

    test("initialization: TODO tab is selected by default (selectedCategory = null)") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.selectedCategory.shouldBeNull()
        }
    }

    test("initialization: TODO tab loads all active products sorted by name") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // Products sorted by name case-insensitive: Cerveza, Refresco, Taco
            state.products shouldHaveSize 3
            state.products.map { it.name } shouldContainExactly listOf("Cerveza", "Refresco", "Taco")
        }
    }

    // -- selectCategory tests ----------------------------------------------------

    test("selectCategory updates product list to only that category's products") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.selectCategory(category2)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.selectedCategory shouldBe category2
            state.products shouldHaveSize 1
            state.products.first().name shouldBe "Taco"
        }
    }

    test("selectCategory with null reverts to TODO tab showing all products") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Select a specific category first
            viewModel.selectCategory(category1)
            advanceUntilIdle()
            viewModel.uiState.value.products shouldHaveSize 2

            // Then go back to TODO
            viewModel.selectCategory(null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.selectedCategory.shouldBeNull()
            state.products shouldHaveSize 3
        }
    }

    test("selectCategory with zero products emits empty list") {
        runTest(testDispatcher) {
            val emptyCategory = Category(id = "cat-empty", name = "Empty", associatedMenuId = menuId)
            every { productRepository.getActiveProductsByCategory("cat-empty") } returns flowOf(emptyList())

            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.selectCategory(emptyCategory)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.products.shouldBeEmpty()
        }
    }

    // -- addToCart tests ----------------------------------------------------------

    test("addToCart adds item with correct price to cart") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems shouldHaveSize 1
            state.cartItems.first() shouldBe sampleCartItem
            state.cartTotal shouldBeExactly 90.0
        }
    }

    test("addToCart accumulates multiple items and updates total") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            viewModel.addToCart(sampleCartItem2)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems shouldHaveSize 2
            state.cartTotal shouldBeExactly 135.0 // 90.0 + 45.0
        }
    }

    // -- removeFromCart tests -----------------------------------------------------

    test("removeFromCart removes item and updates total") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            viewModel.addToCart(sampleCartItem2)
            advanceUntilIdle()

            viewModel.removeFromCart(sampleCartItem.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems shouldHaveSize 1
            state.cartItems.first() shouldBe sampleCartItem2
            state.cartTotal shouldBeExactly 45.0
        }
    }

    test("removeFromCart with non-existing id does not change cart") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            advanceUntilIdle()

            viewModel.removeFromCart("non-existing-id")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems shouldHaveSize 1
            state.cartTotal shouldBeExactly 90.0
        }
    }

    // -- completeOrder success tests ---------------------------------------------

    test("completeOrder success: cart cleared and repository called") {
        runTest(testDispatcher) {
            coEvery { orderRepository.persistOrder(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            viewModel.addToCart(sampleCartItem2)
            advanceUntilIdle()

            viewModel.completeOrder()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems.shouldBeEmpty()
            state.cartTotal shouldBeExactly 0.0
            state.error.shouldBeNull()

            coVerify(exactly = 1) {
                orderRepository.persistOrder(
                    match<OrderEntity> { it.totalAmount == 135.0 && it.status == "PAID" },
                    match<List<OrderItemEntity>> { it.size == 2 },
                    match<List<OrderItemCustomizationEntity>> { it.size == 1 }
                )
            }
        }
    }

    test("completeOrder success: persisted order has correct total amount") {
        runTest(testDispatcher) {
            coEvery { orderRepository.persistOrder(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            advanceUntilIdle()

            viewModel.completeOrder()
            advanceUntilIdle()

            coVerify {
                orderRepository.persistOrder(
                    match<OrderEntity> { it.totalAmount == 90.0 },
                    any(),
                    any()
                )
            }
        }
    }

    // -- completeOrder failure tests ---------------------------------------------

    test("completeOrder failure: cart preserved and error state set") {
        runTest(testDispatcher) {
            coEvery { orderRepository.persistOrder(any(), any(), any()) } throws
                RuntimeException("Database write failed")

            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addToCart(sampleCartItem)
            viewModel.addToCart(sampleCartItem2)
            advanceUntilIdle()

            viewModel.completeOrder()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // Cart should be preserved unchanged
            state.cartItems shouldHaveSize 2
            state.cartItems shouldContainExactly listOf(sampleCartItem, sampleCartItem2)
            state.cartTotal shouldBeExactly 135.0
            // Error should be set
            state.error.shouldNotBeNull()
            state.error!!.shouldNotBeBlank()
        }
    }

    // -- completeOrder empty cart tests -------------------------------------------

    test("completeOrder with empty cart is a no-op") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Cart is empty, completeOrder should do nothing
            viewModel.completeOrder()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.cartItems.shouldBeEmpty()
            state.cartTotal shouldBeExactly 0.0
            state.error.shouldBeNull()

            // Repository should never be called
            coVerify(exactly = 0) { orderRepository.persistOrder(any(), any(), any()) }
        }
    }
})
