package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "delivery_items",
    foreignKeys = [
        ForeignKey(
            entity = DeliveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["deliveryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deliveryId"])]
)
data class DeliveryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deliveryId: Long,
    val productId: Long,
    val productName: String,
    val quantityAdded: Int,
    val unitCost: Double
)
