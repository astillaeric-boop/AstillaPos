package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String,
    val imagePath: String? = null,
    val costPrice: Double,
    val retailPrice: Double,
    val stockQuantity: Int
)
