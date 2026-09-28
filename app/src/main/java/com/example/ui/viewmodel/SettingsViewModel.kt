package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.PosRepository
import com.example.data.sync.GoogleSheetSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = PosRepository(database)

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showScriptDialog = MutableStateFlow(false)
    val showScriptDialog: StateFlow<Boolean> = _showScriptDialog.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()

    private val _showSecondResetConfirm = MutableStateFlow(false)
    val showSecondResetConfirm: StateFlow<Boolean> = _showSecondResetConfirm.asStateFlow()

    private val _showSupportDialog = MutableStateFlow(false)
    val showSupportDialog: StateFlow<Boolean> = _showSupportDialog.asStateFlow()

    init {
        // Schedule periodic sync via WorkManager
        GoogleSheetSyncWorker.schedulePeriodicSync(application)
        // Automatically cleanup receipts older than 6 months on startup
        viewModelScope.launch {
            repository.cleanupOldReceipts()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateStoreName(name: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(storeName = name))
        }
    }

    fun updateBusinessAddress(address: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(businessAddress = address))
        }
    }

    fun updateZipCode(zip: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(zipCode = zip))
        }
    }

    fun updatePhoneNumber(phone: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(phoneNumber = phone))
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(currencySymbol = symbol))
        }
    }

    fun toggleTax(enabled: Boolean) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(taxEnabled = enabled))
        }
    }

    fun updateTaxRate(rate: Float) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(taxRate = rate))
        }
    }

    fun toggleSound(enabled: Boolean) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(soundEnabled = enabled))
        }
    }

    fun updateTheme(themeIndex: Int) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(themeColor = themeIndex))
        }
    }

    fun updateGoogleSheetLink(url: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(googleSheetLink = url))
        }
    }

    fun triggerAutoSyncOnStart() {
        val endpoint = settings.value.googleSheetLink.trim()
        if (endpoint.isNotEmpty() && endpoint.startsWith("http")) {
            GoogleSheetSyncWorker.triggerImmediateSync(getApplication())
        }
    }

    fun triggerSyncNow() {
        _isSyncing.value = true
        GoogleSheetSyncWorker.triggerImmediateSync(getApplication())
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _isSyncing.value = false
            _toastMessage.value = "Sync job dispatched to WorkManager"
        }
    }

    fun pullFromGoogleSheet() {
        val endpoint = settings.value.googleSheetLink.trim()
        if (endpoint.isEmpty() || !endpoint.startsWith("http")) {
            _toastMessage.value = "Please configure a valid Google Sheet Web App URL first."
            return
        }
        _isSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val pullUrl = if (endpoint.contains("?")) "$endpoint&action=pull" else "$endpoint?action=pull"
                val request = Request.Builder().url(pullUrl).get().build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    val root = JSONObject(jsonStr)
                    val productsArray = root.optJSONArray("products")
                    if (productsArray != null) {
                        val productList = mutableListOf<ProductEntity>()
                        for (i in 0 until productsArray.length()) {
                            val item = productsArray.getJSONObject(i)
                            productList.add(
                                ProductEntity(
                                    id = item.optLong("id", 0L),
                                    name = item.optString("name", "Product"),
                                    barcode = item.optString("barcode", ""),
                                    costPrice = item.optDouble("costPrice", 0.0),
                                    retailPrice = item.optDouble("retailPrice", 0.0),
                                    stockQuantity = item.optInt("stockQuantity", 0)
                                )
                            )
                        }
                        val updatedCount = repository.syncProductsFromSheet(productList)
                        withContext(Dispatchers.Main) {
                            _isSyncing.value = false
                            _toastMessage.value = "Pulled $updatedCount products from Google Sheet successfully!"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _isSyncing.value = false
                            _toastMessage.value = "No products found in Google Sheet response."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isSyncing.value = false
                        _toastMessage.value = "Pull failed: HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isSyncing.value = false
                    _toastMessage.value = "Pull error: ${e.message}"
                }
            }
        }
    }

    fun openScriptDialog() {
        _showScriptDialog.value = true
    }

    fun closeScriptDialog() {
        _showScriptDialog.value = false
    }

    fun openResetDialog() {
        _showResetDialog.value = true
    }

    fun closeResetDialog() {
        _showResetDialog.value = false
        _showSecondResetConfirm.value = false
    }

    fun proceedToSecondConfirm() {
        _showResetDialog.value = false
        _showSecondResetConfirm.value = true
    }

    fun executeDatabaseReset() {
        viewModelScope.launch {
            repository.resetDatabase()
            _showSecondResetConfirm.value = false
            _toastMessage.value = "Database cleared and re-initialized."
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty()
            _toastMessage.value = "Demo inventory and restock data seeded!"
        }
    }

    fun openSupportDialog() {
        _showSupportDialog.value = true
    }

    fun closeSupportDialog() {
        _showSupportDialog.value = false
    }

    fun updateMayaQrImage(imagePath: String?) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(mayaQrImagePath = imagePath))
            _toastMessage.value = if (imagePath != null) "Maya QR updated!" else "Reset to default Maya QR"
        }
    }

    fun updateGcashQrImage(imagePath: String?) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(current.copy(gcashQrImagePath = imagePath))
            _toastMessage.value = if (imagePath != null) "GCash QR updated!" else "Reset to default GCash QR"
        }
    }

    fun updateMayaDetails(name: String, handle: String, phone: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(
                current.copy(
                    mayaAccountName = name,
                    mayaHandle = handle,
                    mayaPhoneNumber = phone
                )
            )
        }
    }

    fun updateGcashDetails(name: String, phone: String, userId: String) {
        val current = settings.value
        viewModelScope.launch {
            repository.saveSettings(
                current.copy(
                    gcashAccountName = name,
                    gcashPhoneNumber = phone,
                    gcashUserId = userId
                )
            )
        }
    }
}
