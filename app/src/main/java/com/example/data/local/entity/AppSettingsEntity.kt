package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "Astilla POS Store",
    val businessAddress: String = "",
    val zipCode: String = "",
    val phoneNumber: String = "",
    val currencySymbol: String = "₱",
    val taxEnabled: Boolean = true,
    val taxRate: Float = 12.0f,
    val soundEnabled: Boolean = true,
    val googleSheetLink: String = "",
    val themeColor: Int = 0, // 0 = Emerald POS, 1 = Sapphire Tech, 2 = Amber Amber, 3 = Crimson Modern
    val mayaQrImagePath: String? = null,
    val gcashQrImagePath: String? = null,
    val mayaAccountName: String = "JAN ERIC ASTILLA",
    val mayaHandle: String = "@embersoul",
    val mayaPhoneNumber: String = "+63 *** *** 2516",
    val gcashAccountName: String = "LE**S AN**L Z.",
    val gcashPhoneNumber: String = "+63 919 371 ....",
    val gcashUserId: String = "..........9KDQ2E"
)
