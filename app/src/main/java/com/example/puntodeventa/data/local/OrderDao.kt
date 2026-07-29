package com.example.puntodeventa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
