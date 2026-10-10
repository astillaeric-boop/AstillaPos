package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.PosRepository
import com.example.data.repository.UtangRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PosRepository(database)
        val settings = repository.getSettingsDirect()

        val endpoint = settings.googleSheetLink.trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            Log.d("SyncWorker", "No Google Sheet Web App URL configured. Skipping sync.")
            return@withContext Result.success()
        }

        // Ensure all individual unsynced Utang customer & transaction actions are uploaded
        try {
            val utangRepo = UtangRepository(database)
            utangRepo.syncAllUnsyncedUtang(endpoint)
        } catch (e: Exception) {
            Log.e("SyncWorker", "UtangRepository sync failed: ${e.message}")
        }

        val unsyncedSales = repository.getUnsyncedSales()
        val unsyncedDeliveries = repository.getUnsyncedDeliveries()
        val unsyncedProducts = repository.getUnsyncedProducts()
        val unsyncedCreditTxs = repository.getUnsyncedCreditTransactions()
        val unsyncedCustomers = repository.getUnsyncedCustomers()
        val allProducts = repository.allProducts.firstOrNull() ?: emptyList()
        val allCustomers = repository.allCustomers.firstOrNull() ?: emptyList()

        if (unsyncedSales.isEmpty() && unsyncedDeliveries.isEmpty() && unsyncedProducts.isEmpty() &&
            unsyncedCreditTxs.isEmpty() && unsyncedCustomers.isEmpty()
        ) {
            Log.d("SyncWorker", "No unsynced records found.")
            return@withContext Result.success()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val salesJson = JSONArray()
        for (saleWithItems in unsyncedSales) {
            val saleObj = JSONObject()
            saleObj.put("id", saleWithItems.sale.id)
            saleObj.put("transaction_id", "tx-${saleWithItems.sale.id}")
            saleObj.put("timestamp", saleWithItems.sale.timestamp)
            saleObj.put("dateString", dateFormat.format(Date(saleWithItems.sale.timestamp)))
            saleObj.put("totalAmount", saleWithItems.sale.totalAmount)
            saleObj.put("total_amount", saleWithItems.sale.totalAmount)
            saleObj.put("taxAmount", saleWithItems.sale.taxAmount)
            saleObj.put("paymentType", saleWithItems.sale.paymentType)
            val itemsSummary = saleWithItems.items.joinToString("; ") {
                "${it.quantity}x ${it.productName} (@${it.unitPrice})"
            }
            saleObj.put("itemsSummary", itemsSummary)

            val itemsArr = JSONArray()
            for (item in saleWithItems.items) {
                val itObj = JSONObject()
                itObj.put("productId", item.productId)
                itObj.put("productName", item.productName)
                itObj.put("quantity", item.quantity)
                itObj.put("unitPrice", item.unitPrice)
                itObj.put("unitCost", item.unitCost)
                // Match product barcode if available
                val matchedProd = allProducts.find { it.id == item.productId }
                itObj.put("barcode", matchedProd?.barcode ?: "")
                itemsArr.put(itObj)
            }
            saleObj.put("items", itemsArr)
            salesJson.put(saleObj)
        }

        val deliveriesJson = JSONArray()
        for (delivery in unsyncedDeliveries) {
            val delObj = JSONObject()
            delObj.put("id", delivery.id)
            delObj.put("timestamp", delivery.timestamp)
            delObj.put("dateString", dateFormat.format(Date(delivery.timestamp)))
            delObj.put("supplierName", delivery.supplierName)
            delObj.put("totalCost", delivery.totalCost)
            delObj.put("notes", delivery.notes)
            deliveriesJson.put(delObj)
        }

        val inventoryJson = JSONArray()
        for (product in allProducts) {
            val pObj = JSONObject()
            pObj.put("id", product.id)
            pObj.put("name", product.name)
            pObj.put("productName", product.name)
            pObj.put("barcode", product.barcode)
            pObj.put("category", product.category)
            pObj.put("costPrice", product.costPrice)
            pObj.put("retailPrice", product.retailPrice)
            pObj.put("stockQuantity", product.stockQuantity)
            pObj.put("updatedAt", if (product.updatedAt > 0) dateFormat.format(Date(product.updatedAt)) else dateFormat.format(Date()))
            inventoryJson.put(pObj)
        }

        val customersJson = JSONArray()
        for (customer in allCustomers) {
            val cObj = JSONObject()
            cObj.put("customerId", customer.id)
            cObj.put("customerName", customer.name)
            cObj.put("phoneNumber", customer.phoneNumber)
            cObj.put("currentBalance", customer.currentBalance)
            cObj.put("lastUpdated", if (customer.lastUpdated > 0) dateFormat.format(Date(customer.lastUpdated)) else dateFormat.format(Date()))
            customersJson.put(cObj)
        }

        val utangTransactionsJson = JSONArray()
        val utangPaymentsJson = JSONArray()
        for (tx in unsyncedCreditTxs) {
            val customer = allCustomers.find { it.id == tx.customerId }
            val custName = customer?.name ?: "Customer"
            val custPhone = customer?.phoneNumber ?: ""
            if (tx.transactionType.contains("PAYMENT", ignoreCase = true)) {
                val pObj = JSONObject()
                pObj.put("paymentId", "pay-${tx.id}")
                pObj.put("customerId", tx.customerId)
                pObj.put("customerName", custName)
                pObj.put("phoneNumber", custPhone)
                pObj.put("amountPaid", tx.amount)
                pObj.put("amount", tx.amount)
                pObj.put("remainingBalance", tx.remainingBalance)
                pObj.put("timestamp", dateFormat.format(Date(tx.timestamp)))
                utangPaymentsJson.put(pObj)
            } else {
                val uObj = JSONObject()
                uObj.put("transactionId", "utang-${tx.id}")
                uObj.put("transactionNumber", "TX-UTANG-${tx.id}")
                uObj.put("customerId", tx.customerId)
                uObj.put("customerName", custName)
                uObj.put("phoneNumber", custPhone)
                uObj.put("itemsSummary", tx.itemSummary)
                uObj.put("goodsBorrowed", tx.itemSummary)
                uObj.put("amountBorrowed", tx.amount)
                uObj.put("totalAmount", tx.amount)
                uObj.put("remainingBalance", tx.remainingBalance)
                uObj.put("timestamp", dateFormat.format(Date(tx.timestamp)))
                utangTransactionsJson.put(uObj)
            }
        }

        val rootPayload = JSONObject()
        rootPayload.put("action", "sync_pos_data")
        rootPayload.put("storeName", settings.storeName)
        rootPayload.put("timestamp", System.currentTimeMillis())
        rootPayload.put("sales", salesJson)
        rootPayload.put("deliveries", deliveriesJson)
        rootPayload.put("inventory", inventoryJson)
        rootPayload.put("customers", customersJson)
        rootPayload.put("utangTransactions", utangTransactionsJson)
        rootPayload.put("utangPayments", utangPaymentsJson)

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val body = rootPayload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string().orEmpty()
                Log.d("SyncWorker", "Sync successful: $responseStr")

                // Mark records as synced in Room DB
                val saleIds = unsyncedSales.map { it.sale.id }
                if (saleIds.isNotEmpty()) {
                    repository.markSalesAsSynced(saleIds)
                }
                val deliveryIds = unsyncedDeliveries.map { it.id }
                if (deliveryIds.isNotEmpty()) {
                    repository.markDeliveriesAsSynced(deliveryIds)
                }
                val productIds = unsyncedProducts.map { it.id }
                if (productIds.isNotEmpty()) {
                    repository.markProductsAsSynced(productIds)
                }
                val txIds = unsyncedCreditTxs.map { it.id }
                if (txIds.isNotEmpty()) {
                    repository.markCreditTransactionsSynced(txIds)
                }
                val custIds = unsyncedCustomers.map { it.id }
                if (custIds.isNotEmpty()) {
                    repository.markCustomersSynced(custIds)
                }

                Result.success()
            } else {
                Log.e("SyncWorker", "Sync HTTP error: ${response.code}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync connection failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "astilla_pos_sheet_sync_periodic"
        private const val ONE_TIME_WORK_TAG = "astilla_pos_sheet_sync_now"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<GoogleSheetSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<GoogleSheetSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
