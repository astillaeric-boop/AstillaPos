package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UtangRepository(
    private val database: AppDatabase,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val customerDao = database.customerDao()
    private val creditTransactionDao = database.creditTransactionDao()
    private val appSettingsDao = database.appSettingsDao()

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    /**
     * Syncs a customer record to Google Sheets ("Customers" tab) via SYNC_CUSTOMER action.
     * Upon receiving {"status": "SUCCESS"}, marks local isSynced = 1.
     */
    suspend fun syncCustomer(customer: CustomerEntity, customEndpoint: String? = null): Boolean = withContext(Dispatchers.IO) {
        val endpoint = (customEndpoint ?: appSettingsDao.getSettingsDirect()?.googleSheetLink ?: "").trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            return@withContext false
        }

        try {
            val customerObj = JSONObject().apply {
                put("customerId", customer.id)
                put("name", customer.name)
                put("phone", customer.phoneNumber.ifBlank { "N/A" })
                put("currentBalance", customer.currentBalance)
                put("updatedAt", isoDateFormat.format(Date(customer.lastUpdated)))
            }

            val payload = JSONObject().apply {
                put("action", "SYNC_CUSTOMER")
                put("customer", customerObj)
                // Also provide top-level fields for maximum compatibility
                put("customerId", customer.id)
                put("name", customer.name)
                put("customerName", customer.name)
                put("phone", customer.phoneNumber.ifBlank { "N/A" })
                put("phoneNumber", customer.phoneNumber.ifBlank { "N/A" })
                put("currentBalance", customer.currentBalance)
                put("updatedAt", isoDateFormat.format(Date(customer.lastUpdated)))
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = try { JSONObject(respStr) } catch (_: Exception) { JSONObject() }
                val status = respJson.optString("status", "")
                if (status.equals("SUCCESS", ignoreCase = true)) {
                    customerDao.markCustomerSynced(customer.id)
                    Log.d("UtangRepository", "Customer ${customer.id} synced successfully.")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("UtangRepository", "Failed to sync customer ${customer.id}: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Syncs a borrowed credit transaction to Google Sheets ("Utang_Transactions" tab)
     * via LOG_UTANG_TRANSACTION action.
     * Contains Customer Name, Transaction No., Goods Borrowed, Total Amount, and Time of Transaction.
     * Upon receiving {"status": "SUCCESS"}, marks local isSynced = 1.
     */
    suspend fun logUtangTransaction(
        transaction: CreditTransactionEntity,
        customer: CustomerEntity,
        customEndpoint: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val endpoint = (customEndpoint ?: appSettingsDao.getSettingsDirect()?.googleSheetLink ?: "").trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            return@withContext false
        }

        try {
            val txNumber = "TX-${transaction.id}"
            val utangPayload = JSONObject().apply {
                put("action", "LOG_UTANG_TRANSACTION")
                put("transactionNumber", txNumber)
                put("transactionId", txNumber)
                put("customerId", customer.id)
                put("customerName", customer.name)
                put("goodsBorrowed", transaction.itemSummary.ifBlank { "Store Items" })
                put("itemsSummary", transaction.itemSummary.ifBlank { "Store Items" })
                put("totalAmount", transaction.amount)
                put("amountBorrowed", transaction.amount)
                put("newBalance", customer.currentBalance)
                put("remainingBalance", transaction.remainingBalance)
                put("phone", customer.phoneNumber.ifBlank { "N/A" })
                put("phoneNumber", customer.phoneNumber.ifBlank { "N/A" })
                put("timestamp", isoDateFormat.format(Date(transaction.timestamp)))
            }

            val body = utangPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = try { JSONObject(respStr) } catch (_: Exception) { JSONObject() }
                val status = respJson.optString("status", "")
                if (status.equals("SUCCESS", ignoreCase = true)) {
                    creditTransactionDao.markTransactionSynced(transaction.id)
                    customerDao.markCustomerSynced(customer.id)
                    Log.d("UtangRepository", "Utang transaction ${transaction.id} synced successfully.")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("UtangRepository", "Failed to sync utang transaction ${transaction.id}: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Syncs a credit repayment log to Google Sheets ("Payment_Logs" tab) via LOG_UTANG_PAYMENT action.
     * Upon receiving {"status": "SUCCESS"}, marks local isSynced = 1.
     */
    suspend fun logUtangPayment(
        transaction: CreditTransactionEntity,
        customer: CustomerEntity,
        customEndpoint: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val endpoint = (customEndpoint ?: appSettingsDao.getSettingsDirect()?.googleSheetLink ?: "").trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            return@withContext false
        }

        try {
            val paymentNumber = "PAY-${transaction.id}"
            val paymentPayload = JSONObject().apply {
                put("action", "LOG_UTANG_PAYMENT")
                put("paymentId", paymentNumber)
                put("paymentNumber", paymentNumber)
                put("customerId", customer.id)
                put("customerName", customer.name)
                put("amountPaid", transaction.amount)
                put("totalAmount", transaction.amount)
                put("remainingBalance", transaction.remainingBalance)
                put("newBalance", customer.currentBalance)
                put("phone", customer.phoneNumber.ifBlank { "N/A" })
                put("phoneNumber", customer.phoneNumber.ifBlank { "N/A" })
                put("timestamp", isoDateFormat.format(Date(transaction.timestamp)))
            }

            val body = paymentPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = try { JSONObject(respStr) } catch (_: Exception) { JSONObject() }
                val status = respJson.optString("status", "")
                if (status.equals("SUCCESS", ignoreCase = true)) {
                    creditTransactionDao.markTransactionSynced(transaction.id)
                    customerDao.markCustomerSynced(customer.id)
                    Log.d("UtangRepository", "Utang payment ${transaction.id} synced successfully.")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("UtangRepository", "Failed to sync utang payment ${transaction.id}: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Scans Room DB for any unsynced customers or credit transactions and uploads them sequentially.
     * Called during automatic WorkManager sync or manual "Sync Now".
     */
    suspend fun syncAllUnsyncedUtang(customEndpoint: String? = null): Int = withContext(Dispatchers.IO) {
        val endpoint = (customEndpoint ?: appSettingsDao.getSettingsDirect()?.googleSheetLink ?: "").trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            return@withContext 0
        }

        var syncedCount = 0

        // 1. Unsynced Customers
        val unsyncedCustomers = customerDao.getUnsyncedCustomers()
        for (c in unsyncedCustomers) {
            if (syncCustomer(c, endpoint)) {
                syncedCount++
            }
        }

        // 2. Unsynced Transactions
        val unsyncedTxs = creditTransactionDao.getUnsyncedTransactions()
        for (tx in unsyncedTxs) {
            val customer = customerDao.getCustomerById(tx.customerId)
            if (customer != null) {
                val success = if (tx.transactionType.contains("PAYMENT", ignoreCase = true)) {
                    logUtangPayment(tx, customer, endpoint)
                } else {
                    logUtangTransaction(tx, customer, endpoint)
                }
                if (success) {
                    syncedCount++
                }
            }
        }

        return@withContext syncedCount
    }
}
