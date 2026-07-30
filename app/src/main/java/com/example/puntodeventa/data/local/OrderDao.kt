package com.example.puntodeventa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.puntodeventa.data.model.OrderTotalPoint
import com.example.puntodeventa.data.model.PaymentMethodRevenue
import com.example.puntodeventa.data.model.PeriodSummary
import com.example.puntodeventa.data.model.ProductSaleSummary

@Dao
interface OrderDao {

    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert
    suspend fun insertOrderItemCustomizations(customizations: List<OrderItemCustomizationEntity>)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT * FROM orders WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC LIMIT 20")
    fun getRecentOrdersFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<List<OrderEntity>>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE timestamp >= :start AND timestamp <= :end")
    fun getTotalRevenueFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<Double>

    @Query("SELECT COUNT(*) FROM orders WHERE timestamp >= :start AND timestamp <= :end")
    fun getOrderCountFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT COUNT(DISTINCT customerName) FROM orders WHERE timestamp >= :start AND timestamp <= :end AND customerName IS NOT NULL")
    fun getCustomerCountFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<Int>

    @Query("""
        SELECT oi.productName AS productName,
               COALESCE(SUM(oi.quantity), 0) AS totalQuantity,
               COALESCE(SUM(oi.totalPrice), 0.0) AS totalRevenue
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.timestamp >= :start AND o.timestamp <= :end
        GROUP BY oi.productName
        ORDER BY totalQuantity DESC
        LIMIT 50
    """)
    fun getTopProductsFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<List<ProductSaleSummary>>

    @Query("""
        SELECT * FROM orders 
        WHERE timestamp >= :start 
          AND timestamp <= :end 
        ORDER BY timestamp DESC
    """)
    suspend fun getOrdersByTimeRange(start: Long, end: Long): List<OrderEntity>

    // ── Enterprise dashboard (v2) ─────────────────────────────────────────────

    /**
     * Revenue, order count and distinct customer count of one window in a single row. (Req 2.8)
     * Collected twice per dashboard emission: selected period + previous comparison period.
     */
    @Query("""
        SELECT COALESCE(SUM(totalAmount), 0.0) AS totalRevenue,
               COUNT(*)                        AS orderCount,
               COUNT(DISTINCT customerName)    AS customerCount
        FROM orders
        WHERE timestamp >= :start AND timestamp <= :end
    """)
    fun getPeriodSummaryFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<PeriodSummary>

    /**
     * Revenue split by tender type. (Req 2.9)
     * `paymentMethod ASC` is the stable tie-breaker so equal revenues never reorder between emissions.
     */
    @Query("""
        SELECT paymentMethod                   AS paymentMethod,
               COALESCE(SUM(totalAmount), 0.0) AS totalRevenue,
               COUNT(*)                        AS orderCount
        FROM orders
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY paymentMethod
        ORDER BY totalRevenue DESC, paymentMethod ASC
    """)
    fun getPaymentMethodBreakdownFlow(
        start: Long,
        end: Long
    ): kotlinx.coroutines.flow.Flow<List<PaymentMethodRevenue>>

    /**
     * Every order in range reduced to (timestamp, amount) for the sales trend chart. (Req 2.10)
     * Bucketing into hours/days/months is done by `SalesTrendCalculator`, not by SQL.
     */
    @Query("""
        SELECT timestamp AS timestamp, totalAmount AS amount
        FROM orders
        WHERE timestamp >= :start AND timestamp <= :end
        ORDER BY timestamp ASC
    """)
    fun getOrderTotalsFlow(start: Long, end: Long): kotlinx.coroutines.flow.Flow<List<OrderTotalPoint>>
}
