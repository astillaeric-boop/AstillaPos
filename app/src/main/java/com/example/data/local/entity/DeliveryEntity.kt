package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val supplierName: String,
    val totalCost: Double,
    val notes: String = "",
    val isSynced: Boolean = false
)
