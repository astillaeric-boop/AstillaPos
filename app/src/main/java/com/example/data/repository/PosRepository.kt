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
    val totalInventoryValue: Flow<Double> = productDao.getTotalInventoryValue()
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
                            stockQuantity = item.stockQuantity,
                            isSynced = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    count++
                } else {
                    productDao.insertProduct(
                        item.copy(
                            isSynced = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    count++
                }
            }
            count
        }
    }

    suspend fun getUnsyncedProducts(): List<ProductEntity> = productDao.getUnsyncedProducts()

    fun getUnsyncedProductCount(): Flow<Int> = productDao.getUnsyncedProductCount()

    suspend fun markProductsAsSynced(ids: List<Long>) = productDao.markProductsAsSynced(ids)

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

    suspend fun getUnsyncedCustomers(): List<CustomerEntity> = customerDao.getUnsyncedCustomers()

    suspend fun markCustomersSynced(ids: List<Long>) = customerDao.markCustomersSynced(ids)

    suspend fun getUnsyncedCreditTransactions(): List<CreditTransactionEntity> = creditTransactionDao.getUnsyncedTransactions()

    suspend fun markCreditTransactionsSynced(ids: List<Long>) = creditTransactionDao.markTransactionsSynced(ids)

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

    suspend fun ensureDefaultSettings() {
        val existingSettings = appSettingsDao.getSettingsDirect()
        if (existingSettings == null) {
            appSettingsDao.insertOrUpdate(AppSettingsEntity())
        }
    }
}
