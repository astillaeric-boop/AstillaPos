package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.DeliveryItemEntity
import com.example.data.local.entity.DeliveryWithItems
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val saleDao = database.saleDao()
    private val deliveryDao = database.deliveryDao()
    private val appSettingsDao = database.appSettingsDao()
    private val customerDao = database.customerDao()
    private val creditTransactionDao = database.creditTransactionDao()

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()
    val allDeliveries: Flow<List<DeliveryEntity>> = deliveryDao.getAllDeliveries()
    val allDeliveriesWithItems: Flow<List<DeliveryWithItems>> = deliveryDao.getAllDeliveriesWithItems()
    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allCreditTransactions: Flow<List<CreditTransactionEntity>> = creditTransactionDao.getAllCreditTransactions()
    val totalOutstandingCredit: Flow<Double?> = customerDao.getTotalOutstandingCredit()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
        if (category == "All" || category.isBlank()) productDao.getAllProducts() else productDao.getProductsByCategory(category)

    fun getSalesByCategory(startDate: Long, endDate: Long): Flow<List<CategorySalesSummary>> =
        productDao.getSalesByCategory(startDate, endDate)

    suspend fun getSalesByCategoryDirect(startDate: Long, endDate: Long): List<CategorySalesSummary> =
        productDao.getSalesByCategoryDirect(startDate, endDate)

    suspend fun insertCategory(name: String): Long {
        val trimmed = name.trim()
        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing != null) return existing.id
        return categoryDao.insertCategory(CategoryEntity(name = trimmed))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? =
        productDao.getProductByBarcode(barcode.trim())

    suspend fun insertOrUpdateProduct(product: ProductEntity): Long {
        return if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun checkoutSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): Long {
        return database.withTransaction {
            val saleId = saleDao.insertSale(sale)
            val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
            saleDao.insertSaleItems(itemsWithSaleId)

            // Decrement inventory stock
            items.forEach { item ->
                productDao.decreaseStock(item.productId, item.quantity)
            }
            saleId
        }
    }

    suspend fun logDelivery(
        delivery: DeliveryEntity,
        items: List<DeliveryItemEntity> = emptyList()
    ): Long {
        return database.withTransaction {
            val deliveryId = deliveryDao.insertDelivery(delivery)
            if (items.isNotEmpty()) {
                val itemsWithDeliveryId = items.map { it.copy(deliveryId = deliveryId) }
                deliveryDao.insertDeliveryItems(itemsWithDeliveryId)
                items.forEach { item ->
                    productDao.increaseStock(item.productId, item.quantityAdded)
                    if (item.unitCost > 0) {
                        val existing = productDao.getProductById(item.productId)
                        if (existing != null) {
                            productDao.updateProduct(existing.copy(costPrice = item.unitCost))
                        }
                    }
                }
            }
            deliveryId
        }
    }

    suspend fun cleanupOldReceipts(): Int {
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        val deletedCount = saleDao.deleteSalesOlderThan(sixMonthsAgo)
        saleDao.cleanOrphanedSaleItems()
        return deletedCount
    }

    suspend fun syncProductsFromSheet(importedProducts: List<ProductEntity>): Int {
        return database.withTransaction {
            var count = 0
            for (item in importedProducts) {
                val existingByBarcode = if (item.barcode.isNotBlank()) productDao.getProductByBarcode(item.barcode) else null
                if (existingByBarcode != null) {
                    productDao.updateProduct(
                        existingByBarcode.copy(
                            name = item.name.ifBlank { existingByBarcode.name },
                            costPrice = if (item.costPrice > 0) item.costPrice else existingByBarcode.costPrice,
                            retailPrice = if (item.retailPrice > 0) item.retailPrice else existingByBarcode.retailPrice,
                            stockQuantity = item.stockQuantity
                        )
                    )
                    count++
                } else {
                    productDao.insertProduct(item)
                    count++
                }
            }
            count
        }
    }

    suspend fun getSettingsDirect(): AppSettingsEntity {
        return appSettingsDao.getSettingsDirect() ?: AppSettingsEntity()
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        appSettingsDao.insertOrUpdate(settings)
    }

    suspend fun getUnsyncedSales(): List<SaleWithItems> = saleDao.getUnsyncedSales()

    suspend fun getUnsyncedDeliveries(): List<DeliveryEntity> = deliveryDao.getUnsyncedDeliveries()

    suspend fun markSalesAsSynced(ids: List<Long>) = saleDao.markSalesAsSynced(ids)

    suspend fun markDeliveriesAsSynced(ids: List<Long>) = deliveryDao.markDeliveriesAsSynced(ids)

    suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?> = customerDao.getCustomerByIdFlow(id)

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = customerDao.searchCustomers(query)

    fun getCustomerTransactions(customerId: Long): Flow<List<CreditTransactionEntity>> =
        customerDao.getTransactionsForCustomer(customerId)

    suspend fun insertOrUpdateCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        database.withTransaction {
            customerDao.deleteTransactionsForCustomer(customer.id)
            customerDao.deleteCustomer(customer)
        }
    }

    suspend fun recordBorrowTransaction(
        customerId: Long,
        borrowedItems: List<Pair<ProductEntity, Int>>
    ): CreditTransactionEntity {
        return database.withTransaction {
            val customer = customerDao.getCustomerById(customerId)
                ?: throw IllegalArgumentException("Customer not found with id $customerId")

            var totalAmount = 0.0
            val summaryItems = mutableListOf<String>()

            for ((product, qty) in borrowedItems) {
                if (qty <= 0) continue
                // Deduct stock in ProductEntity table
                productDao.decreaseStock(product.id, qty)
                val lineTotal = product.retailPrice * qty
                totalAmount += lineTotal
                val formattedPrice = if (lineTotal % 1.0 == 0.0) {
                    String.format(java.util.Locale.US, "%.0f", lineTotal)
                } else {
                    String.format(java.util.Locale.US, "%.2f", lineTotal)
                }
                summaryItems.add("${qty}x ${product.name} (₱$formattedPrice)")
            }

            val itemSummary = summaryItems.joinToString(", ")
            val newBalance = customer.currentBalance + totalAmount
            val now = System.currentTimeMillis()

            // Record Sale (Accrual Basis: record sales & COGS on the date items leave inventory)
            val sale = SaleEntity(
                timestamp = now,
                totalAmount = totalAmount,
                taxAmount = 0.0,
                paymentType = "Credit (Utang)",
                isSynced = false
            )
            val saleId = saleDao.insertSale(sale)
            val saleItems = borrowedItems.filter { it.second > 0 }.map { (product, qty) ->
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    unitCost = product.costPrice,
                    unitPrice = product.retailPrice
                )
            }
            if (saleItems.isNotEmpty()) {
                saleDao.insertSaleItems(saleItems)
            }

            val transaction = CreditTransactionEntity(
                customerId = customerId,
                timestamp = now,
                transactionType = "BORROW",
                amount = totalAmount,
                itemSummary = itemSummary,
                remainingBalance = newBalance
            )
            val txId = customerDao.insertCreditTransaction(transaction)
            customerDao.updateCustomerBalance(customerId, newBalance, now)
            transaction.copy(id = txId)
        }
    }

    /**
     * Sum of all completed sales (Cash + Credit) in the timeframe (Accrual Basis).
     */
    fun getGrossSales(startDate: Long, endDate: Long): Flow<Double> =
        saleDao.getGrossSales(startDate, endDate)

    /**
     * Actual Cash Flow: Cash Sales + Utang Payments Received in the timeframe.
     */
    fun getCashCollected(startDate: Long, endDate: Long): Flow<Double> {
        return combine(
            saleDao.getCashSales(startDate, endDate),
            creditTransactionDao.getPaymentsCollected(startDate, endDate)
        ) { cashSales, payments ->
            cashSales + payments
        }
    }

    /**
     * Cost of Goods Sold (COGS) for the timeframe.
     */
    fun getTotalCOGS(startDate: Long, endDate: Long): Flow<Double> =
        saleDao.getTotalCOGS(startDate, endDate)

    /**
     * Real-time sum of all active customer unpaid utang balances.
     */
    fun getOutstandingUtangBalance(): Flow<Double> =
        customerDao.getOutstandingUtangBalance()

    suspend fun recordCreditTransaction(
        customerId: Long,
        transactionType: String, // "BORROW" or "PAYMENT"
        amount: Double,
        itemSummary: String
    ): CreditTransactionEntity {
        return database.withTransaction {
            val customer = customerDao.getCustomerById(customerId)
                ?: throw IllegalArgumentException("Customer not found with id $customerId")

            val newBalance = if (transactionType.equals("BORROW", ignoreCase = true)) {
                customer.currentBalance + amount
            } else {
                maxOf(0.0, customer.currentBalance - amount)
            }

            val now = System.currentTimeMillis()
            val transaction = CreditTransactionEntity(
                customerId = customerId,
                timestamp = now,
                transactionType = transactionType.uppercase(),
                amount = amount,
                itemSummary = itemSummary,
                remainingBalance = newBalance
            )
            val txId = customerDao.insertCreditTransaction(transaction)
            customerDao.updateCustomerBalance(customerId, newBalance, now)
            transaction.copy(id = txId)
        }
    }

    suspend fun resetDatabase() {
        database.withTransaction {
            creditTransactionDao.deleteAllCreditTransactions()
            customerDao.deleteAllCreditTransactions()
            customerDao.deleteAllCustomers()
            saleDao.deleteAllSaleItems()
            saleDao.deleteAllSales()
            deliveryDao.deleteAllDeliveries()
            productDao.deleteAllProducts()
            categoryDao.deleteAllCategories()
            appSettingsDao.clearSettings()
            appSettingsDao.insertOrUpdate(AppSettingsEntity())
        }
    }

    suspend fun seedDemoDataIfEmpty() {
        // Also run automatic cleanup of receipts older than 6 months on startup
        cleanupOldReceipts()

        // Seed default categories if empty
        val existingCategories = categoryDao.getAllCategories().firstOrNull()
        if (existingCategories.isNullOrEmpty()) {
            val defaultCategories = listOf(
                "Beverages",
                "Snacks",
                "Canned Goods",
                "Toiletries",
                "General"
            )
            categoryDao.insertCategories(defaultCategories.map { CategoryEntity(name = it) })
        }

        // Seed demo customers if empty
        val existingCustomers = customerDao.getAllCustomers().firstOrNull()
        if (existingCustomers.isNullOrEmpty()) {
            val c1Id = customerDao.insertCustomer(
                CustomerEntity(
                    name = "Aling Nena Stores",
                    phoneNumber = "09171234567",
                    messengerContact = "aling.nena.official",
                    currentBalance = 460.00,
                    lastUpdated = System.currentTimeMillis() - 86400000L * 2
                )
            )
            val c2Id = customerDao.insertCustomer(
                CustomerEntity(
                    name = "Mang Tomas Bakery",
                    phoneNumber = "09289876543",
                    messengerContact = "mangtomasbakery",
                    currentBalance = 925.00,
                    lastUpdated = System.currentTimeMillis() - 86400000L
                )
            )
            val c3Id = customerDao.insertCustomer(
                CustomerEntity(
                    name = "Kuya Jobert Delivery",
                    phoneNumber = "09985551234",
                    messengerContact = "jobert.official",
                    currentBalance = 0.00,
                    lastUpdated = System.currentTimeMillis() - 86400000L * 5
                )
            )

            // Seed sample credit transactions for demo
            customerDao.insertCreditTransactions(
                listOf(
                    CreditTransactionEntity(
                        customerId = c1Id,
                        timestamp = System.currentTimeMillis() - 86400000L * 3,
                        transactionType = "BORROW",
                        amount = 370.00,
                        itemSummary = "2x Arabica Premium Coffee 250g",
                        remainingBalance = 370.00
                    ),
                    CreditTransactionEntity(
                        customerId = c1Id,
                        timestamp = System.currentTimeMillis() - 86400000L * 2,
                        transactionType = "BORROW",
                        amount = 90.00,
                        itemSummary = "1x Dark Chocolate Bar 70%",
                        remainingBalance = 460.00
                    ),
                    CreditTransactionEntity(
                        customerId = c2Id,
                        timestamp = System.currentTimeMillis() - 86400000L * 2,
                        transactionType = "BORROW",
                        amount = 1425.00,
                        itemSummary = "5x Almond Milk 1L, 3x Cold Brew 240ml, 4x Honey Oat Cookies",
                        remainingBalance = 1425.00
                    ),
                    CreditTransactionEntity(
                        customerId = c2Id,
                        timestamp = System.currentTimeMillis() - 86400000L,
                        transactionType = "PAYMENT",
                        amount = 500.00,
                        itemSummary = "Partial payment via Cash",
                        remainingBalance = 925.00
                    ),
                    CreditTransactionEntity(
                        customerId = c3Id,
                        timestamp = System.currentTimeMillis() - 86400000L * 6,
                        transactionType = "BORROW",
                        amount = 250.00,
                        itemSummary = "10x Mineral Spring Water 500ml",
                        remainingBalance = 250.00
                    ),
                    CreditTransactionEntity(
                        customerId = c3Id,
                        timestamp = System.currentTimeMillis() - 86400000L * 5,
                        transactionType = "PAYMENT",
                        amount = 250.00,
                        itemSummary = "Full payment via GCash (Reference #8921)",
                        remainingBalance = 0.00
                    )
                )
            )
        }

        val current = productDao.getAllProducts().firstOrNull()
        if (current.isNullOrEmpty()) {
            val demoProducts = listOf(
                ProductEntity(
                    name = "Arabica Premium Coffee 250g",
                    barcode = "8901234567890",
                    category = "Beverages",
                    imagePath = null,
                    costPrice = 120.00,
                    retailPrice = 185.00,
                    stockQuantity = 24
                ),
                ProductEntity(
                    name = "Organic Green Tea 100g",
                    barcode = "8901234567891",
                    category = "Beverages",
                    imagePath = null,
                    costPrice = 75.00,
                    retailPrice = 110.00,
                    stockQuantity = 18
                ),
                ProductEntity(
                    name = "Dark Chocolate Bar 70%",
                    barcode = "8901234567892",
                    category = "Snacks",
                    imagePath = null,
                    costPrice = 55.00,
                    retailPrice = 90.00,
                    stockQuantity = 8 // Low stock alert!
                ),
                ProductEntity(
                    name = "Cold Brew Can 240ml",
                    barcode = "8901234567893",
                    category = "Beverages",
                    imagePath = null,
                    costPrice = 45.00,
                    retailPrice = 75.00,
                    stockQuantity = 45
                ),
                ProductEntity(
                    name = "Almond Milk 1L",
                    barcode = "8901234567894",
                    category = "Beverages",
                    imagePath = null,
                    costPrice = 110.00,
                    retailPrice = 160.00,
                    stockQuantity = 12
                ),
                ProductEntity(
                    name = "Honey Oat Cookies 150g",
                    barcode = "8901234567895",
                    category = "Snacks",
                    imagePath = null,
                    costPrice = 60.00,
                    retailPrice = 95.00,
                    stockQuantity = 3 // Critical stock alert
                ),
                ProductEntity(
                    name = "Mineral Spring Water 500ml",
                    barcode = "8901234567896",
                    category = "Beverages",
                    imagePath = null,
                    costPrice = 12.00,
                    retailPrice = 25.00,
                    stockQuantity = 60
                )
            )
            productDao.insertProducts(demoProducts)

            val existingSettings = appSettingsDao.getSettingsDirect()
            if (existingSettings == null) {
                appSettingsDao.insertOrUpdate(AppSettingsEntity())
            }

            // Seed initial restock delivery record for accounting with multiple items
            val initialDelivery = DeliveryEntity(
                timestamp = System.currentTimeMillis() - 86400000L * 3,
                supplierName = "Highland Beverage Co.",
                totalCost = 3500.00,
                notes = "Initial shop opening inventory stock"
            )
            val dId = deliveryDao.insertDelivery(initialDelivery)
            val sampleItems = listOf(
                DeliveryItemEntity(
                    deliveryId = dId,
                    productId = 1,
                    productName = "Arabica Premium Coffee 250g",
                    quantityAdded = 20,
                    unitCost = 120.00
                ),
                DeliveryItemEntity(
                    deliveryId = dId,
                    productId = 4,
                    productName = "Cold Brew Can 240ml",
                    quantityAdded = 30,
                    unitCost = 45.00
                )
            )
            deliveryDao.insertDeliveryItems(sampleItems)
        }
    }
}
