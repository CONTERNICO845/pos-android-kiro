package com.example.puntodeventa.data.repository

import androidx.room.withTransaction
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.OrderDao
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.local.OrderItemCustomizationEntity
import com.example.puntodeventa.data.local.OrderItemEntity
import com.example.puntodeventa.data.model.OrderTotalPoint
import com.example.puntodeventa.data.model.PaymentMethodRevenue
import com.example.puntodeventa.data.model.PeriodSummary
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

    // ── Enterprise dashboard (v2) ─────────────────────────────────────────────

    /** Summary of one window; used for the selected period and its comparison baseline. (Req 2.8) */
    fun getPeriodSummaryFlow(start: Long, end: Long): Flow<PeriodSummary> =
        orderDao.getPeriodSummaryFlow(start, end)

    /** Revenue per tender type in the window. (Req 2.9) */
    fun getPaymentMethodBreakdownFlow(start: Long, end: Long): Flow<List<PaymentMethodRevenue>> =
        orderDao.getPaymentMethodBreakdownFlow(start, end)

    /** Raw (timestamp, amount) points feeding the sales trend chart. (Req 2.10) */
    fun getOrderTotalsFlow(start: Long, end: Long): Flow<List<OrderTotalPoint>> =
        orderDao.getOrderTotalsFlow(start, end)

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
