package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.DeliveryItemEntity
import com.example.data.local.entity.DeliveryWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY timestamp DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntity>>

    @Transaction
    @Query("SELECT * FROM deliveries ORDER BY timestamp DESC")
    fun getAllDeliveriesWithItems(): Flow<List<DeliveryWithItems>>

    @Query("SELECT * FROM deliveries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getDeliveriesBetween(startTime: Long, endTime: Long): Flow<List<DeliveryEntity>>

    @Transaction
    @Query("SELECT * FROM deliveries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getDeliveriesWithItemsBetween(startTime: Long, endTime: Long): Flow<List<DeliveryWithItems>>

    @Query("SELECT * FROM deliveries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getDeliveriesBetweenDirect(startTime: Long, endTime: Long): List<DeliveryEntity>

    @Transaction
    @Query("SELECT * FROM deliveries WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedDeliveriesWithItems(): List<DeliveryWithItems>

    @Query("SELECT * FROM deliveries WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedDeliveries(): List<DeliveryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryItems(items: List<DeliveryItemEntity>)

    @Query("UPDATE deliveries SET isSynced = 1 WHERE id IN (:deliveryIds)")
    suspend fun markDeliveriesAsSynced(deliveryIds: List<Long>)

    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)

    @Query("DELETE FROM deliveries")
    suspend fun deleteAllDeliveries()
}
