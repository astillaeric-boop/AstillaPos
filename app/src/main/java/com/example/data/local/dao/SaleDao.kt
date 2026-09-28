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
