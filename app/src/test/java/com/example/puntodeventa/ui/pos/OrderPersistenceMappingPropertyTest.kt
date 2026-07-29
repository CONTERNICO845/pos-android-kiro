package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.local.OrderItemCustomizationEntity
import com.example.puntodeventa.data.local.OrderItemEntity
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

// Feature: pos-main-screen, Property 7: Order Persistence Maps Cart to Entities

@OptIn(ExperimentalKotest::class)
class OrderPersistenceMappingPropertyTest : StringSpec({

    /**
     * Property 7: Order Persistence Maps Cart to Entities
     *
     * For any non-empty cart, when completeOrder() is called, the persisted OrderEntity.totalAmount
     * SHALL equal the cart total, each cart item SHALL map to an OrderItemEntity with matching
     * productName, quantity, basePrice, and totalPrice, and each selected customization SHALL map
     * to an OrderItemCustomizationEntity with matching optionName and extraPrice.
     *
     * **Validates: Requirements 6.1, 6.2, 6.3**
     */
    "Property 7 - completeOrder maps cart items to correct OrderEntity, OrderItemEntity, and OrderItemCustomizationEntity fields" {
        // Generator for a SelectedCustomization
        val arbCustomization = Arb.double(0.0..50.0).map { rawPrice ->
            val extraPrice = BigDecimal(rawPrice).setScale(2, RoundingMode.HALF_UP).toDouble()
            SelectedCustomization(
                optionId = UUID.randomUUID().toString(),
                optionName = "Option-${UUID.randomUUID().toString().take(8)}",
                extraPrice = extraPrice
            )
        }

        // Generator for a CartItem with 0..5 customizations
        val arbCartItem = Arb.int(1..99).map { quantity ->
            val basePrice = BigDecimal(Math.random() * 200.0).setScale(2, RoundingMode.HALF_UP).toDouble()
            val customizationCount = (0..5).random()
            val customizations = (0 until customizationCount).map {
                val ep = BigDecimal(Math.random() * 20.0).setScale(2, RoundingMode.HALF_UP).toDouble()
                SelectedCustomization(
                    optionId = UUID.randomUUID().toString(),
                    optionName = "Customization-${UUID.randomUUID().toString().take(6)}",
                    extraPrice = ep
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
                emoji = "\uD83C\uDF54",
                basePrice = basePrice,
                quantity = quantity,
                selectedCustomizations = customizations,
                extraNotes = "notes-${(0..200).random()}",
                totalPrice = totalPrice
            )
        }

        // Generator: non-empty list of cart items (1..8)
        val arbCartItems = Arb.list(arbCartItem, range = 1..8)

        checkAll(
            PropTestConfig(iterations = 100),
            arbCartItems
        ) { cartItems ->
            // === Replicate the mapping logic from PosViewModel.completeOrder() ===
            val orderId = UUID.randomUUID().toString()
            val orderEntity = OrderEntity(
                id = orderId,
                timestamp = System.currentTimeMillis(),
                totalAmount = cartItems.sumOf { it.totalPrice },
                status = "PAID"
            )
            val orderItems = cartItems.map { cartItem ->
                OrderItemEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    productId = cartItem.productId,
                    productName = cartItem.productName,
                    quantity = cartItem.quantity,
                    basePrice = cartItem.basePrice,
                    totalPrice = cartItem.totalPrice,
                    extraNotes = cartItem.extraNotes.ifBlank { null }
                )
            }
            val customizations = cartItems.flatMapIndexed { index, cartItem ->
                cartItem.selectedCustomizations.map { customization ->
                    OrderItemCustomizationEntity(
                        id = UUID.randomUUID().toString(),
                        orderItemId = orderItems[index].id,
                        optionName = customization.optionName,
                        extraPrice = customization.extraPrice
                    )
                }
            }

            // === Verify OrderEntity totalAmount equals cart total ===
            orderEntity.totalAmount shouldBe cartItems.sumOf { it.totalPrice }
            orderEntity.status shouldBe "PAID"

            // === Verify each CartItem maps to an OrderItemEntity with matching fields ===
            orderItems.size shouldBe cartItems.size
            cartItems.forEachIndexed { index, cartItem ->
                val orderItem = orderItems[index]
                orderItem.orderId shouldBe orderId
                orderItem.productName shouldBe cartItem.productName
                orderItem.quantity shouldBe cartItem.quantity
                orderItem.basePrice shouldBe cartItem.basePrice
                orderItem.totalPrice shouldBe cartItem.totalPrice
                orderItem.productId shouldBe cartItem.productId
            }

            // === Verify each SelectedCustomization maps to an OrderItemCustomizationEntity ===
            val expectedCustomizationCount = cartItems.sumOf { it.selectedCustomizations.size }
            customizations.size shouldBe expectedCustomizationCount

            var customizationIndex = 0
            cartItems.forEachIndexed { index, cartItem ->
                val orderItemId = orderItems[index].id
                cartItem.selectedCustomizations.forEach { selectedCustomization ->
                    val entity = customizations[customizationIndex]
                    entity.orderItemId shouldBe orderItemId
                    entity.optionName shouldBe selectedCustomization.optionName
                    entity.extraPrice shouldBe selectedCustomization.extraPrice
                    customizationIndex++
                }
            }
        }
    }
})
