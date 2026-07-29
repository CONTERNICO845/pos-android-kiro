package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.ProductRepository
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

// Feature: pos-main-screen, Property 8: Successful Persistence Clears Cart

@OptIn(ExperimentalKotest::class, ExperimentalCoroutinesApi::class)
class SuccessfulPersistenceClearsCartPropertyTest : StringSpec({

    /**
     * Property 8: Successful Persistence Clears Cart
     *
     * For any non-empty cart, after a successful call to completeOrder(), the cart SHALL be empty
     * and the cart total SHALL be 0.00.
     *
     * **Validates: Requirements 6.5**
     */
    "Property 8 - after successful completeOrder, cart is empty and total is 0.00" {
        // Generator for cart items with 0..3 customizations
        val arbCartItem = Arb.int(1..99).map { quantity ->
            val basePrice = BigDecimal(Math.random() * 100.0).setScale(2, RoundingMode.HALF_UP).toDouble()
            val customizations = (0..(0..3).random()).map {
                val extraPrice = BigDecimal(Math.random() * 10.0).setScale(2, RoundingMode.HALF_UP).toDouble()
                SelectedCustomization(
                    optionId = UUID.randomUUID().toString(),
                    optionName = "Option-${UUID.randomUUID().toString().take(6)}",
                    extraPrice = extraPrice
                )
            }
            val totalPrice = BigDecimal(basePrice + customizations.sumOf { it.extraPrice })
                .multiply(BigDecimal(quantity))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            CartItem(
                id = UUID.randomUUID().toString(),
                productId = "prod-${UUID.randomUUID().toString().take(6)}",
                productName = "Product-${UUID.randomUUID().toString().take(8)}",
                emoji = "🛒",
                basePrice = basePrice,
                quantity = quantity,
                selectedCustomizations = customizations,
                extraNotes = "notes-${(0..100).random()}",
                totalPrice = totalPrice
            )
        }

        // Generator: non-empty list of cart items (1..10 items)
        val arbCartItems = Arb.list(arbCartItem, range = 1..10)

        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        try {
            checkAll(
                PropTestConfig(iterations = 100),
                arbCartItems
            ) { cartItems ->
                val categoryRepository = mockk<CategoryRepository>()
                val productRepository = mockk<ProductRepository>()
                val orderRepository = mockk<OrderRepository>()

                coEvery { categoryRepository.getCategoriesByMenu(any()) } returns flowOf(emptyList())
                coEvery { orderRepository.persistOrder(any(), any(), any()) } returns Unit

                val viewModel = PosViewModel(
                    categoryRepository = categoryRepository,
                    productRepository = productRepository,
                    orderRepository = orderRepository,
                    menuId = "test-menu"
                )

                // Add items to cart
                cartItems.forEach { viewModel.addToCart(it) }

                // Call completeOrder (repository does NOT throw → success)
                viewModel.completeOrder()
                testDispatcher.scheduler.advanceUntilIdle()

                // Assert: cart is empty after successful persistence
                viewModel.uiState.value.cartItems.shouldBeEmpty()

                // Assert: cart total is 0.00
                viewModel.uiState.value.cartTotal shouldBe 0.0
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
})
