package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getSalesBetweenDirect(startTime: Long, endTime: Long): List<SaleWithItems>

    /**
     * Sum of all completed sales (Cash + Credit) in the timeframe (Accrual Basis).
     */
    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate")
    fun getGrossSales(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun getGrossSalesDirect(startDate: Long, endDate: Long): Double

    /**
     * Sum of sales paid via immediate tender (Cash, GCash, Maya, Bank).
     */
    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate AND paymentType NOT LIKE '%Utang%' AND paymentType NOT LIKE '%Credit%'")
    fun getCashSales(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate AND paymentType NOT LIKE '%Utang%' AND paymentType NOT LIKE '%Credit%'")
    suspend fun getCashSalesDirect(startDate: Long, endDate: Long): Double

    /**
     * Sum of sales charged to Credit / Utang receivables.
     */
    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate AND (paymentType LIKE '%Utang%' OR paymentType LIKE '%Credit%')")
    fun getCreditSales(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE timestamp >= :startDate AND timestamp <= :endDate AND (paymentType LIKE '%Utang%' OR paymentType LIKE '%Credit%')")
    suspend fun getCreditSalesDirect(startDate: Long, endDate: Long): Double

    /**
     * Cost of Goods Sold (COGS): Sum of unit cost * quantity for items sold/borrowed in the timeframe.
     */
    @Query("SELECT COALESCE(SUM(si.unitCost * si.quantity), 0.0) FROM sale_items si INNER JOIN sales s ON si.saleId = s.id WHERE s.timestamp >= :startDate AND s.timestamp <= :endDate")
    fun getTotalCOGS(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(si.unitCost * si.quantity), 0.0) FROM sale_items si INNER JOIN sales s ON si.saleId = s.id WHERE s.timestamp >= :startDate AND s.timestamp <= :endDate")
    suspend fun getTotalCOGSDirect(startDate: Long, endDate: Long): Double

    @Transaction
    @Query("SELECT * FROM sales WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedSales(): List<SaleWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("UPDATE sales SET isSynced = 1 WHERE id IN (:saleIds)")
    suspend fun markSalesAsSynced(saleIds: List<Long>)

    @Query("DELETE FROM sales WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteSalesOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM sale_items WHERE saleId NOT IN (SELECT id FROM sales)")
    suspend fun cleanOrphanedSaleItems(): Int

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun deleteAllSaleItems()
}
