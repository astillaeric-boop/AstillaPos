package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DeliveryWithItems(
    @Embedded val delivery: DeliveryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "deliveryId"
    )
    val items: List<DeliveryItemEntity>
)
