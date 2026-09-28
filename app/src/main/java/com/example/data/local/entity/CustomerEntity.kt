package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String = "",          // Used for Phone SIM SMS
    val messengerContact: String = "",    // Used for Facebook Messenger m.me link
    val currentBalance: Double = 0.0,      // Total accumulated utang
    val lastUpdated: Long = System.currentTimeMillis()
)
