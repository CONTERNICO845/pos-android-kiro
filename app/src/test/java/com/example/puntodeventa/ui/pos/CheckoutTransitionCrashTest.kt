package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.ProductRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Integration test for the checkout transition crash.
 *
 * Bug: When the user presses the TOTAL button (triggering showCheckout()), the app crashes
 * with IllegalStateException because CheckoutPanel uses .verticalScroll(rememberScrollState())
 * and CashKeypad previously used LazyVerticalGrid inside it — a nested scrolling violation in
 * Jetpack Compose.
 *
 * This test validates that the ViewModel-level transition to checkout works correctly.
 * The actual crash occurred at the composition layer (CheckoutPanel rendering) due to
 * nested scrolling — a LazyVerticalGrid (infinite height) inside a vertically-scrollable
 * Column. The fix replaces LazyVerticalGrid with a static Column+Row layout in CashKeypad.kt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutTransitionCrashTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()
    val menuId = "test-menu-id"

    // -- Test data ---------------------------------------------------------------

    val category1 = Category(id = "cat-1", name = "Bebidas", associatedMenuId = menuId)

    val product1 = Product(
        id = "prod-1", emoji = "🍺", name = "Cerveza",
        description = "Beer", basePrice = 45.0, isActive = true, categoryId = "cat-1"
    )

    // -- Mocks -------------------------------------------------------------------

    lateinit var categoryRepository: CategoryRepository
    lateinit var productRepository: ProductRepository
    lateinit var orderRepository: OrderRepository
    lateinit var categoriesFlow: MutableStateFlow<List<Category>>
    lateinit var productsFlow: MutableStateFlow<List<Product>>

    fun createViewModel(): PosViewModel {
        return PosViewModel(categoryRepository, productRepository, orderRepository, menuId)
    }

    beforeEach {
        Dispatchers.setMain(testDispatcher)

        categoriesFlow = MutableStateFlow(listOf(category1))
        productsFlow = MutableStateFlow(listOf(product1))

        categoryRepository = mockk()
        productRepository = mockk()
        orderRepository = mockk()

        every { categoryRepository.getCategoriesByMenu(menuId) } returns categoriesFlow
        every { productRepository.getActiveProductsByCategory("cat-1") } returns productsFlow
    }

    afterEach {
        Dispatchers.resetMain()
    }

    // -- Checkout transition tests -----------------------------------------------

    test("showCheckout sets isCheckoutVisible to true when cart has items") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Add a product to the cart
            val cartItem = CartItem(
                id = "item-1",
                productId = "prod-1",
                productName = "Cerveza",
                emoji = "🍺",
                basePrice = 45.0,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = 45.0
            )
            viewModel.addToCart(cartItem)
            advanceUntilIdle()

            // Trigger checkout transition (equivalent to pressing the TOTAL button)
            viewModel.showCheckout()
            advanceUntilIdle()

            // The ViewModel correctly sets isCheckoutVisible = true.
            // NOTE: The actual crash occurred at the composition layer when CheckoutPanel
            // attempted to render CashKeypad's LazyVerticalGrid inside a verticalScroll
            // Column — a nested scrolling violation. This test validates the ViewModel-level
            // state transition is correct; the UI fix is in CashKeypad.kt where
            // LazyVerticalGrid was replaced with a static Column+Row layout.
            viewModel.uiState.value.isCheckoutVisible shouldBe true
        }
    }

    test("showCheckout does NOT set isCheckoutVisible when cart is empty") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Attempt checkout with empty cart
            viewModel.showCheckout()
            advanceUntilIdle()

            viewModel.uiState.value.isCheckoutVisible shouldBe false
        }
    }

    test("showCheckout resets checkoutState to fresh instance") {
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Add product and show checkout
            val cartItem = CartItem(
                id = "item-1",
                productId = "prod-1",
                productName = "Cerveza",
                emoji = "🍺",
                basePrice = 45.0,
                quantity = 1,
                selectedCustomizations = emptyList(),
                extraNotes = "",
                totalPrice = 45.0
            )
            viewModel.addToCart(cartItem)
            advanceUntilIdle()

            // Modify checkout state before showing checkout again
            viewModel.showCheckout()
            advanceUntilIdle()
            viewModel.updateCustomerName("Juan")
            viewModel.addDenomination(100)
            advanceUntilIdle()

            // Hide and re-show checkout — state should be fresh
            viewModel.hideCheckout()
            viewModel.showCheckout()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.isCheckoutVisible shouldBe true
            state.checkoutState.customerName shouldBe ""
            state.checkoutState.cashReceived shouldBe 0.0
            state.checkoutState.denominationCounts shouldBe emptyMap()
        }
    }
})
