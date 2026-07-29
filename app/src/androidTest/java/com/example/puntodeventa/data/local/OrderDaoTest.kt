package com.example.puntodeventa.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for OrderDao verifying:
 * - Insert and query round-trip for OrderEntity, OrderItemEntity, OrderItemCustomizationEntity
 * - CASCADE deletion: deleting OrderEntity removes OrderItemEntities and OrderItemCustomizationEntities
 * - CASCADE deletion: deleting OrderItemEntity removes OrderItemCustomizationEntities
 * - Transaction atomicity via database.withTransaction
 *
 * Validates: Requirements 1.4, 1.5, 6.4
 */
@RunWith(AndroidJUnit4::class)
class OrderDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var orderDao: OrderDao

    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val order = OrderEntity(
        id = "order-1",
        timestamp = 1700000000000L,
        totalAmount = 25.50,
        status = "COMPLETED"
    )

    private val orderItem1 = OrderItemEntity(
        id = "item-1",
        orderId = "order-1",
        productId = "prod-1",
        productName = "Classic Burger",
        quantity = 2,
        basePrice = 9.99,
        totalPrice = 19.98,
        extraNotes = "No onions"
    )

    private val orderItem2 = OrderItemEntity(
        id = "item-2",
        orderId = "order-1",
        productId = "prod-2",
        productName = "French Fries",
        quantity = 1,
        basePrice = 5.52,
        totalPrice = 5.52,
        extraNotes = null
    )

    private val customization1 = OrderItemCustomizationEntity(
        id = "cust-1",
        orderItemId = "item-1",
        optionName = "Extra Cheese",
        extraPrice = 1.50
    )

    private val customization2 = OrderItemCustomizationEntity(
        id = "cust-2",
        orderItemId = "item-1",
        optionName = "Large Size",
        extraPrice = 2.00
    )

    private val customization3 = OrderItemCustomizationEntity(
        id = "cust-3",
        orderItemId = "item-2",
        optionName = "Ketchup",
        extraPrice = 0.00
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .addCallback(foreignKeyCallback)
            .build()
        orderDao = db.orderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helper: insert full order hierarchy ───────────────────────────────────

    private suspend fun insertFullHierarchy() {
        orderDao.insertOrder(order)
        orderDao.insertOrderItems(listOf(orderItem1, orderItem2))
        orderDao.insertOrderItemCustomizations(listOf(customization1, customization2, customization3))
    }

    // ── Helper: query rows using raw SQL ──────────────────────────────────────

    private fun queryOrders(): List<OrderEntity> {
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM orders")
        val results = mutableListOf<OrderEntity>()
        while (cursor.moveToNext()) {
            results.add(
                OrderEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("totalAmount")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                )
            )
        }
        cursor.close()
        return results
    }

    private fun queryOrderItems(): List<OrderItemEntity> {
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM order_items")
        val results = mutableListOf<OrderItemEntity>()
        while (cursor.moveToNext()) {
            results.add(
                OrderItemEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    orderId = cursor.getString(cursor.getColumnIndexOrThrow("orderId")),
                    productId = cursor.getString(cursor.getColumnIndexOrThrow("productId")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("productName")),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                    basePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("basePrice")),
                    totalPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("totalPrice")),
                    extraNotes = cursor.getString(cursor.getColumnIndexOrThrow("extraNotes"))
                )
            )
        }
        cursor.close()
        return results
    }

    private fun queryOrderItemCustomizations(): List<OrderItemCustomizationEntity> {
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM order_item_customizations")
        val results = mutableListOf<OrderItemCustomizationEntity>()
        while (cursor.moveToNext()) {
            results.add(
                OrderItemCustomizationEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    orderItemId = cursor.getString(cursor.getColumnIndexOrThrow("orderItemId")),
                    optionName = cursor.getString(cursor.getColumnIndexOrThrow("optionName")),
                    extraPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("extraPrice"))
                )
            )
        }
        cursor.close()
        return results
    }

    // ── Test: Insert and query round-trip ─────────────────────────────────────
    //
    // Validates: Requirements 1.4, 1.5
    //
    // Insert OrderEntity, OrderItemEntity, and OrderItemCustomizationEntity,
    // then query them back and verify all fields match.

    @Test
    fun insertAndQueryRoundTrip_allFieldsMatch() = runBlocking {
        insertFullHierarchy()

        // Verify OrderEntity
        val orders = queryOrders()
        assertEquals("Should have 1 order", 1, orders.size)
        assertEquals(order.id, orders[0].id)
        assertEquals(order.timestamp, orders[0].timestamp)
        assertEquals(order.totalAmount, orders[0].totalAmount, 0.001)
        assertEquals(order.status, orders[0].status)

        // Verify OrderItemEntities
        val items = queryOrderItems()
        assertEquals("Should have 2 order items", 2, items.size)

        val item1 = items.find { it.id == "item-1" }!!
        assertEquals(orderItem1.orderId, item1.orderId)
        assertEquals(orderItem1.productId, item1.productId)
        assertEquals(orderItem1.productName, item1.productName)
        assertEquals(orderItem1.quantity, item1.quantity)
        assertEquals(orderItem1.basePrice, item1.basePrice, 0.001)
        assertEquals(orderItem1.totalPrice, item1.totalPrice, 0.001)
        assertEquals(orderItem1.extraNotes, item1.extraNotes)

        val item2 = items.find { it.id == "item-2" }!!
        assertEquals(orderItem2.orderId, item2.orderId)
        assertEquals(orderItem2.productId, item2.productId)
        assertEquals(orderItem2.productName, item2.productName)
        assertEquals(orderItem2.quantity, item2.quantity)
        assertEquals(orderItem2.basePrice, item2.basePrice, 0.001)
        assertEquals(orderItem2.totalPrice, item2.totalPrice, 0.001)
        assertEquals(orderItem2.extraNotes, item2.extraNotes)

        // Verify OrderItemCustomizationEntities
        val customizations = queryOrderItemCustomizations()
        assertEquals("Should have 3 customizations", 3, customizations.size)

        val cust1 = customizations.find { it.id == "cust-1" }!!
        assertEquals(customization1.orderItemId, cust1.orderItemId)
        assertEquals(customization1.optionName, cust1.optionName)
        assertEquals(customization1.extraPrice, cust1.extraPrice, 0.001)

        val cust2 = customizations.find { it.id == "cust-2" }!!
        assertEquals(customization2.orderItemId, cust2.orderItemId)
        assertEquals(customization2.optionName, cust2.optionName)
        assertEquals(customization2.extraPrice, cust2.extraPrice, 0.001)

        val cust3 = customizations.find { it.id == "cust-3" }!!
        assertEquals(customization3.orderItemId, cust3.orderItemId)
        assertEquals(customization3.optionName, cust3.optionName)
        assertEquals(customization3.extraPrice, cust3.extraPrice, 0.001)
    }

    // ── Test: CASCADE delete OrderEntity removes OrderItems and Customizations ─
    //
    // Validates: Requirement 1.4
    //
    // WHEN an OrderEntity is deleted, THE database SHALL cascade-delete all
    // associated OrderItemEntity records AND their OrderItemCustomizationEntity records.

    @Test
    fun deleteOrder_cascadesOrderItemsAndCustomizations() = runBlocking {
        insertFullHierarchy()

        // Delete the order via raw SQL
        db.openHelper.writableDatabase.execSQL("DELETE FROM orders WHERE id = 'order-1'")

        val items = queryOrderItems()
        val customizations = queryOrderItemCustomizations()

        assertTrue("order_items should be empty after order deletion", items.isEmpty())
        assertTrue("order_item_customizations should be empty after order deletion", customizations.isEmpty())
    }

    // ── Test: CASCADE delete OrderItemEntity removes its Customizations ───────
    //
    // Validates: Requirement 1.5
    //
    // WHEN an OrderItemEntity is deleted, THE database SHALL cascade-delete all
    // associated OrderItemCustomizationEntity records.

    @Test
    fun deleteOrderItem_cascadesCustomizations() = runBlocking {
        insertFullHierarchy()

        // Delete only item-1 (which has customizations cust-1 and cust-2)
        db.openHelper.writableDatabase.execSQL("DELETE FROM order_items WHERE id = 'item-1'")

        val items = queryOrderItems()
        val customizations = queryOrderItemCustomizations()

        // item-2 should remain
        assertEquals("Should have 1 remaining order item", 1, items.size)
        assertEquals("item-2", items[0].id)

        // Only cust-3 (belonging to item-2) should remain
        assertEquals("Should have 1 remaining customization", 1, customizations.size)
        assertEquals("cust-3", customizations[0].id)
    }

    // ── Test: Transaction atomicity ──────────────────────────────────────────
    //
    // Validates: Requirement 6.4
    //
    // WHEN all entities are inserted within a single transaction via
    // database.withTransaction, all records SHALL be committed atomically.

    @Test
    fun transactionAtomicity_allEntitiesPersistedTogether() = runBlocking {
        db.withTransaction {
            orderDao.insertOrder(order)
            orderDao.insertOrderItems(listOf(orderItem1, orderItem2))
            orderDao.insertOrderItemCustomizations(listOf(customization1, customization2, customization3))
        }

        val orders = queryOrders()
        val items = queryOrderItems()
        val customizations = queryOrderItemCustomizations()

        assertEquals("Should have 1 order after transaction", 1, orders.size)
        assertEquals("Should have 2 items after transaction", 2, items.size)
        assertEquals("Should have 3 customizations after transaction", 3, customizations.size)
    }

    // ── Test: Transaction rollback on failure ────────────────────────────────
    //
    // Validates: Requirement 6.4
    //
    // IF an exception occurs within a transaction, THEN no records SHALL be
    // persisted (all-or-nothing semantics).

    @Test
    fun transactionRollback_noEntitiesPersistedOnFailure() = runBlocking {
        try {
            db.withTransaction {
                orderDao.insertOrder(order)
                orderDao.insertOrderItems(listOf(orderItem1))
                // Force a failure mid-transaction
                throw RuntimeException("Simulated failure")
            }
        } catch (_: RuntimeException) {
            // Expected
        }

        val orders = queryOrders()
        val items = queryOrderItems()

        assertTrue("orders should be empty after failed transaction", orders.isEmpty())
        assertTrue("order_items should be empty after failed transaction", items.isEmpty())
    }
}
