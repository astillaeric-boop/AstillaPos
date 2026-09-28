package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CreditTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val transactionType: String,          // "BORROW" or "PAYMENT"
    val amount: Double,
    val itemSummary: String,              // Itemized string of borrowed products
    val remainingBalance: Double
)
