package com.example.data.sync

data class SyncPayload(
    val action: String = "sync_pos_data",
    val storeName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sales: List<SyncSale>,
    val deliveries: List<SyncDelivery>,
    val inventory: List<SyncProduct>
)

data class SyncSale(
    val id: Long,
    val timestamp: Long,
    val dateString: String,
    val totalAmount: Double,
    val taxAmount: Double,
    val paymentType: String,
    val itemsSummary: String
)

data class SyncDelivery(
    val id: Long,
    val timestamp: Long,
    val dateString: String,
    val supplierName: String,
    val totalCost: Double,
    val notes: String
)

data class SyncProduct(
    val id: Long,
    val name: String,
    val barcode: String,
    val category: String = "General",
    val costPrice: Double,
    val retailPrice: Double,
    val stockQuantity: Int
)

data class SyncResponse(
    val status: String?,
    val message: String?,
    val syncedSales: Int?,
    val syncedDeliveries: Int?
)
