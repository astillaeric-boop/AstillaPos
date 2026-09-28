package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SaleWithItems(
    @Embedded
    val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)
