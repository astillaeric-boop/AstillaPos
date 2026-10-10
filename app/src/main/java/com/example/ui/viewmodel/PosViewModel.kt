package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleWithItems
import com.example.data.repository.PosRepository
import com.example.data.sync.GoogleSheetSyncWorker
import com.example.util.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)

data class PosUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val isScannerOpen: Boolean = false,
    val isCartSheetOpen: Boolean = false,
    val isCashCheckoutDialogOpen: Boolean = false,
    val selectedPaymentType: String = "Cash",
    val selectedCreditCustomerId: Long? = null,
    val lastCompletedSale: SaleWithItems? = null,
    val isReceiptDialogOpen: Boolean = false,
    val toastMessage: String? = null
)

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = PosRepository(database)
    val soundManager = SoundManager()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    val availableCategories: StateFlow<List<String>> = repository.allCategories
        .map { list ->
            list.map { it.name }.distinct()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val products: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategoryFilter
    ) { all, query, catFilter ->
        var list = all
        if (catFilter != "All" && catFilter.isNotBlank()) {
            list = list.filter { it.category.equals(catFilter, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.barcode.lowercase().contains(q)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onCategorySelected(category: String) {
        _selectedCategoryFilter.value = category
    }

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
            repository.cleanupOldReceipts()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openScanner() {
        _uiState.value = _uiState.value.copy(isScannerOpen = true)
    }

    fun closeScanner() {
        _uiState.value = _uiState.value.copy(isScannerOpen = false)
    }

    fun openCartSheet() {
        _uiState.value = _uiState.value.copy(isCartSheetOpen = true)
    }

    fun closeCartSheet() {
        _uiState.value = _uiState.value.copy(isCartSheetOpen = false)
    }

    fun setPaymentType(paymentType: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentType = paymentType)
    }

    fun setSelectedCreditCustomerId(id: Long?) {
        _uiState.value = _uiState.value.copy(selectedCreditCustomerId = id)
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            val soundEnabled = settings.value.soundEnabled
            if (product != null) {
                if (product.stockQuantity <= 0) {
                    soundManager.playErrorTone(soundEnabled)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Item out of stock! (${product.name})",
                        isScannerOpen = false
                    )
                    return@launch
                }
                val existingCartItem = _uiState.value.cartItems.find { it.product.id == product.id }
                val currentCartQty = existingCartItem?.quantity ?: 0
                if (currentCartQty + 1 > product.stockQuantity) {
                    soundManager.playErrorTone(soundEnabled)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Cannot exceed available stock (Max: ${product.stockQuantity})",
                        isScannerOpen = false
                    )
                    return@launch
                }
                soundManager.playScanBeep(soundEnabled)
                addToCart(product)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Added: ${product.name}",
                    isScannerOpen = false // Dismiss scanner after scan or leave open
                )
            } else {
                soundManager.playErrorTone(soundEnabled)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Unrecognized Barcode: $barcode"
                )
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        if (product.stockQuantity <= 0) {
            soundManager.playErrorTone(settings.value.soundEnabled)
            _uiState.value = _uiState.value.copy(
                toastMessage = "Item out of stock!"
            )
            return
        }

        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentItems[index]
            if (existing.quantity + 1 > product.stockQuantity) {
                soundManager.playErrorTone(settings.value.soundEnabled)
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Cannot exceed available stock (Max: ${product.stockQuantity})"
                )
                return
            }
            currentItems[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentItems.add(CartItem(product = product, quantity = 1))
        }
        recalculateTotals(currentItems)
    }

    fun incrementCartItem(product: ProductEntity) {
        addToCart(product)
    }

    fun decrementQuantity(productId: Long) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = currentItems[index]
            if (existing.quantity > 1) {
                currentItems[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                currentItems.removeAt(index)
            }
            recalculateTotals(currentItems)
        }
    }

    fun removeFromCart(productId: Long) {
        val currentItems = _uiState.value.cartItems.filter { it.product.id != productId }
        recalculateTotals(currentItems)
    }

    fun clearCart() {
        recalculateTotals(emptyList())
    }

    private fun recalculateTotals(cartItems: List<CartItem>) {
        val subtotal = cartItems.sumOf { it.product.retailPrice * it.quantity }
        val currentSettings = settings.value
        val taxAmount = if (currentSettings.taxEnabled) {
            subtotal * (currentSettings.taxRate / 100.0)
        } else {
            0.0
        }
        val total = subtotal + taxAmount

        _uiState.value = _uiState.value.copy(
            cartItems = cartItems,
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalAmount = total
        )
    }

    fun initiateCheckout() {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) return

        if (currentState.selectedPaymentType.equals("Cash", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(isCashCheckoutDialogOpen = true)
        } else {
            checkout(cashTendered = 0.0, changeDue = 0.0)
        }
    }

    fun dismissCashCheckoutDialog() {
        _uiState.value = _uiState.value.copy(isCashCheckoutDialogOpen = false)
    }

    fun checkout(cashTendered: Double = 0.0, changeDue: Double = 0.0) {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) return

        viewModelScope.launch {
            // Strict Inventory Verification against latest Room DB state
            for (cartItem in currentState.cartItems) {
                val freshProduct = repository.getProductById(cartItem.product.id)
                if (freshProduct == null) {
                    soundManager.playErrorTone(settings.value.soundEnabled)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Item not found in catalog: ${cartItem.product.name}",
                        isCashCheckoutDialogOpen = false
                    )
                    return@launch
                }
                if (freshProduct.stockQuantity <= 0) {
                    soundManager.playErrorTone(settings.value.soundEnabled)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Item is out of stock: ${freshProduct.name}. Please adjust cart.",
                        isCashCheckoutDialogOpen = false
                    )
                    return@launch
                }
                if (cartItem.quantity > freshProduct.stockQuantity) {
                    soundManager.playErrorTone(settings.value.soundEnabled)
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Cannot checkout: ${freshProduct.name} only has ${freshProduct.stockQuantity} in stock (Cart: ${cartItem.quantity})",
                        isCashCheckoutDialogOpen = false
                    )
                    return@launch
                }
            }

            val saleEntity = SaleEntity(
                timestamp = System.currentTimeMillis(),
                totalAmount = currentState.totalAmount,
                taxAmount = currentState.taxAmount,
                paymentType = currentState.selectedPaymentType,
                cashTendered = cashTendered,
                changeDue = changeDue,
                isSynced = false
            )

            val saleItems = currentState.cartItems.map { cartItem ->
                SaleItemEntity(
                    saleId = 0L,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitCost = cartItem.product.costPrice,
                    unitPrice = cartItem.product.retailPrice
                )
            }

            val saleId = repository.checkoutSale(saleEntity, saleItems)
            val completedSale = SaleWithItems(
                sale = saleEntity.copy(id = saleId),
                items = saleItems.map { it.copy(saleId = saleId) }
            )

            if (currentState.selectedPaymentType.contains("Utang", ignoreCase = true) && currentState.selectedCreditCustomerId != null) {
                val itemSummary = currentState.cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
                try {
                    val tx = repository.recordCreditTransaction(
                        customerId = currentState.selectedCreditCustomerId,
                        transactionType = "BORROW",
                        amount = currentState.totalAmount,
                        itemSummary = itemSummary
                    )
                    val customer = repository.getCustomerById(currentState.selectedCreditCustomerId)
                    if (customer != null) {
                        dispatchLogUtangTransaction(tx, customer)
                    }
                } catch (e: Exception) {
                    // Log or handle
                }
            }

            // Immediately trigger background sync to Google Sheets
            GoogleSheetSyncWorker.triggerImmediateSync(getApplication())

            soundManager.playSuccessCheckout(settings.value.soundEnabled)

            _uiState.value = _uiState.value.copy(
                cartItems = emptyList(),
                subtotal = 0.0,
                taxAmount = 0.0,
                totalAmount = 0.0,
                isCartSheetOpen = false,
                isCashCheckoutDialogOpen = false,
                lastCompletedSale = completedSale,
                isReceiptDialogOpen = true,
                toastMessage = if (currentState.selectedPaymentType.contains("Utang", ignoreCase = true)) {
                    "Utang recorded & sale completed!"
                } else {
                    "Checkout completed successfully!"
                }
            )
        }
    }

    fun dismissReceiptDialog() {
        _uiState.value = _uiState.value.copy(
            isReceiptDialogOpen = false,
            lastCompletedSale = null
        )
    }

    private fun dispatchLogUtangTransaction(tx: CreditTransactionEntity, customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val directSettings = repository.getSettingsDirect()
            val endpoint = directSettings.googleSheetLink.trim()
            if (endpoint.isEmpty() || !endpoint.startsWith("http")) return@launch
            try {
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
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    repository.markCreditTransactionsSynced(listOf(tx.id))
                    repository.markCustomersSynced(listOf(customer.id))
                }
                response.close()
            } catch (_: Exception) {
                // Background dispatch failure safe, batch WorkManager will synchronize
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
