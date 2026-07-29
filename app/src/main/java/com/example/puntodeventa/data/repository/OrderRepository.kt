package com.example.puntodeventa.data.repository

import androidx.room.withTransaction
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.OrderDao
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.local.OrderItemCustomizationEntity
import com.example.puntodeventa.data.local.OrderItemEntity
import com.example.puntodeventa.data.model.ProductSaleSummary
import kotlinx.coroutines.flow.Flow

class OrderRepository(
    private val orderDao: OrderDao,
    private val database: AppDatabase
) {

    suspend fun getOrderCount(): Int = orderDao.getOrderCount()

    fun getRecentOrdersFlow(start: Long, end: Long): Flow<List<OrderEntity>> =
        orderDao.getRecentOrdersFlow(start, end)

    fun getTotalRevenueFlow(start: Long, end: Long): Flow<Double> =
        orderDao.getTotalRevenueFlow(start, end)

    fun getOrderCountFlow(start: Long, end: Long): Flow<Int> =
        orderDao.getOrderCountFlow(start, end)

    fun getCustomerCountFlow(start: Long, end: Long): Flow<Int> =
        orderDao.getCustomerCountFlow(start, end)

    fun getTopProductsFlow(start: Long, end: Long): Flow<List<ProductSaleSummary>> =
        orderDao.getTopProductsFlow(start, end)

    suspend fun getOrdersByTimeRange(start: Long, end: Long): List<OrderEntity> =
        orderDao.getOrdersByTimeRange(start, end)

    suspend fun persistOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        customizations: List<OrderItemCustomizationEntity>,
        customerName: String? = null,
        clientTicketText: String? = null,
        internalTicketText: String? = null
    ) {
        val enrichedOrder = order.copy(
            customerName = customerName ?: order.customerName,
            clientTicketText = clientTicketText ?: order.clientTicketText,
            internalTicketText = internalTicketText ?: order.internalTicketText
        )
        database.withTransaction {
            orderDao.insertOrder(enrichedOrder)
            orderDao.insertOrderItems(items)
            orderDao.insertOrderItemCustomizations(customizations)
        }
    }
}
