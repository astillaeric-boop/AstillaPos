package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.PosRepository
import com.example.data.repository.UtangRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CreditUiState(
    val showAddEditCustomerDialog: Boolean = false,
    val editingCustomer: CustomerEntity? = null,
    val showBorrowDialog: Boolean = false,
    val showPaymentDialog: Boolean = false,
    val targetCustomer: CustomerEntity? = null,
    val showReceiptDialog: Boolean = false,
    val receiptTransaction: CreditTransactionEntity? = null,
    val receiptCustomer: CustomerEntity? = null,
    val showDeleteConfirm: Boolean = false,
    val customerToDelete: CustomerEntity? = null,
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CreditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = PosRepository(database)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(CreditUiState())
    val uiState: StateFlow<CreditUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val customers: StateFlow<List<CustomerEntity>> = combine(
        repository.allCustomers,
        _searchQuery
    ) { all, query ->
        if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter {
                it.name.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q) ||
                    it.messengerContact.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalOutstandingCredit: StateFlow<Double> = repository.totalOutstandingCredit
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getCustomerByIdFlow(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val selectedCustomerTransactions: StateFlow<List<CreditTransactionEntity>> =
        _selectedCustomerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCustomerTransactions(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomerId.value = customer?.id
    }

    fun openAddCustomerDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditCustomerDialog = true,
            editingCustomer = null
        )
    }

    fun openEditCustomerDialog(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(
            showAddEditCustomerDialog = true,
            editingCustomer = customer
        )
    }

    fun closeAddEditCustomerDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditCustomerDialog = false,
            editingCustomer = null
        )
    }

    fun saveCustomer(id: Long, name: String, phone: String, messenger: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Customer name cannot be empty")
            return
        }
        viewModelScope.launch {
            val customer = CustomerEntity(
                id = id,
                name = name.trim(),
                phoneNumber = phone.trim(),
                messengerContact = messenger.trim(),
                currentBalance = _uiState.value.editingCustomer?.currentBalance ?: 0.0,
                lastUpdated = System.currentTimeMillis()
            )
            val savedId = repository.insertOrUpdateCustomer(customer)
            val savedCustomer = customer.copy(id = savedId)
            dispatchSyncCustomer(savedCustomer)
            closeAddEditCustomerDialog()
            _uiState.value = _uiState.value.copy(
                toastMessage = if (id == 0L) "Customer added successfully" else "Customer updated"
            )
        }
    }

    fun confirmDeleteCustomer(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            customerToDelete = customer
        )
    }

    fun closeDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            customerToDelete = null
        )
    }

    fun deleteCustomer() {
        val toDelete = _uiState.value.customerToDelete ?: return
        viewModelScope.launch {
            repository.deleteCustomer(toDelete)
            if (_selectedCustomerId.value == toDelete.id) {
                _selectedCustomerId.value = null
            }
            closeDeleteConfirm()
            _uiState.value = _uiState.value.copy(toastMessage = "Customer deleted")
        }
    }

    fun openBorrowDialog(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(
            showBorrowDialog = true,
            targetCustomer = customer
        )
    }

    fun closeBorrowDialog() {
        _uiState.value = _uiState.value.copy(
            showBorrowDialog = false,
            targetCustomer = null
        )
    }

    fun recordBorrow(customerId: Long, items: List<Pair<ProductEntity, Int>>) {
        val validItems = items.filter { it.second > 0 }
        if (validItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please select at least one item to borrow")
            return
        }
        viewModelScope.launch {
            try {
                val customer = repository.getCustomerById(customerId) ?: return@launch
                val tx = repository.recordBorrowTransaction(
                    customerId = customerId,
                    borrowedItems = validItems
                )
                closeBorrowDialog()
                val updatedCustomer = repository.getCustomerById(customerId) ?: customer
                dispatchLogUtangTransaction(tx, updatedCustomer)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Utang recorded & inventory updated!",
                    showReceiptDialog = true,
                    receiptCustomer = updatedCustomer,
                    receiptTransaction = tx
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "Error: ${e.message}")
            }
        }
    }

    fun recordBorrow(customerId: Long, amount: Double, itemSummary: String) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter a valid amount")
            return
        }
        viewModelScope.launch {
            try {
                val customer = repository.getCustomerById(customerId) ?: return@launch
                val tx = repository.recordCreditTransaction(
                    customerId = customerId,
                    transactionType = "BORROW",
                    amount = amount,
                    itemSummary = itemSummary.ifBlank { "Store Utang Credit" }
                )
                closeBorrowDialog()
                // Update target customer for receipt
                val updatedCustomer = repository.getCustomerById(customerId) ?: customer
                dispatchLogUtangTransaction(tx, updatedCustomer)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Utang recorded successfully!",
                    showReceiptDialog = true,
                    receiptCustomer = updatedCustomer,
                    receiptTransaction = tx
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "Error: ${e.message}")
            }
        }
    }

    fun openPaymentDialog(customer: CustomerEntity) {
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = true,
            targetCustomer = customer
        )
    }

    fun closePaymentDialog() {
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = false,
            targetCustomer = null
        )
    }

    fun recordPayment(customerId: Long, amount: Double, paymentMethod: String) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter a valid payment amount")
            return
        }
        viewModelScope.launch {
            try {
                val customer = repository.getCustomerById(customerId) ?: return@launch
                val note = "Paid via $paymentMethod"
                val tx = repository.recordCreditTransaction(
                    customerId = customerId,
                    transactionType = "CUSTOMER_PAYMENT",
                    amount = amount,
                    itemSummary = note
                )
                closePaymentDialog()
                val updatedCustomer = repository.getCustomerById(customerId) ?: customer
                dispatchLogUtangPayment(tx, updatedCustomer)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Payment recorded successfully!",
                    showReceiptDialog = true,
                    receiptCustomer = updatedCustomer,
                    receiptTransaction = tx
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "Error: ${e.message}")
            }
        }
    }

    fun openReceiptForTransaction(customer: CustomerEntity, transaction: CreditTransactionEntity?) {
        _uiState.value = _uiState.value.copy(
            showReceiptDialog = true,
            receiptCustomer = customer,
            receiptTransaction = transaction
        )
    }

    fun closeReceiptDialog() {
        _uiState.value = _uiState.value.copy(
            showReceiptDialog = false,
            receiptCustomer = null,
            receiptTransaction = null
        )
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    private fun dispatchSyncCustomer(customer: CustomerEntity) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val payload = JSONObject().apply {
            put("action", "SYNC_CUSTOMER")
            put("customerId", customer.id)
            put("customerName", customer.name)
            put("phoneNumber", customer.phoneNumber)
            put("currentBalance", customer.currentBalance)
            put("lastUpdated", dateFormat.format(Date(customer.lastUpdated)))
        }
        dispatchSheetAction(payload)
    }

    private fun dispatchLogUtangTransaction(tx: CreditTransactionEntity, customer: CustomerEntity) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val payload = JSONObject().apply {
            put("action", "LOG_UTANG_TRANSACTION")
            put("transactionId", "utang-${tx.id}")
            put("transactionNumber", "TX-UTANG-${tx.id}")
            put("customerId", customer.id)
            put("customerName", customer.name)
            put("phoneNumber", customer.phoneNumber)
            put("itemsSummary", tx.itemSummary)
            put("goodsBorrowed", tx.itemSummary)
            put("amountBorrowed", tx.amount)
            put("totalAmount", tx.amount)
            put("remainingBalance", tx.remainingBalance)
            put("timestamp", dateFormat.format(Date(tx.timestamp)))
        }
        dispatchSheetAction(payload)
    }

    private fun dispatchLogUtangPayment(tx: CreditTransactionEntity, customer: CustomerEntity) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val payload = JSONObject().apply {
            put("action", "LOG_UTANG_PAYMENT")
            put("paymentId", "pay-${tx.id}")
            put("paymentNumber", "PAY-${tx.id}")
            put("customerId", customer.id)
            put("customerName", customer.name)
            put("phoneNumber", customer.phoneNumber)
            put("amountPaid", tx.amount)
            put("amount", tx.amount)
            put("totalAmount", tx.amount)
            put("remainingBalance", tx.remainingBalance)
            put("timestamp", dateFormat.format(Date(tx.timestamp)))
        }
        dispatchSheetAction(payload)
    }

    private fun dispatchSheetAction(payload: JSONObject) {
        val endpoint = settings.value.googleSheetLink.trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()
                client.newCall(request).execute().close()
            } catch (_: Exception) {
                // Background dispatch failure safe, batch WorkManager will synchronize
            }
        }
    }
}
