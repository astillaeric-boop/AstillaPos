package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String,
    val category: String = "General",
    val imagePath: String? = null,
    val costPrice: Double = 0.0,
    val retailPrice: Double = 0.0,
    val stockQuantity: Int = 0
)
