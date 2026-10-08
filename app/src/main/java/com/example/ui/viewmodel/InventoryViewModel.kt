package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.DeliveryItemEntity
import com.example.data.local.entity.DeliveryWithItems
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.PosRepository
import com.example.data.sync.GoogleSheetSyncWorker
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class StockFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK
}

data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val barcode: String = "",
    val category: String = "General",
    val imagePath: String? = null,
    val costPrice: String = "",
    val retailPrice: String = "",
    val stockQuantity: String = ""
)

data class DeliveryItemDraft(
    val tempId: String = UUID.randomUUID().toString(),
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double
)

data class DeliveryFormState(
    val supplierName: String = "",
    val totalCostInput: String = "",
    val notes: String = "",
    val items: List<DeliveryItemDraft> = emptyList(),
    val selectedProductId: Long? = null,
    val draftQuantity: String = "1",
    val draftUnitCost: String = ""
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = PosRepository(database)

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val totalInventoryValue: StateFlow<Double> = repository.totalInventoryValue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.ALL)
    val stockFilter: StateFlow<StockFilter> = _stockFilter.asStateFlow()

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

    val products: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        _searchQuery,
        _stockFilter,
        _selectedCategoryFilter
    ) { all, query, filter, catFilter ->
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
        when (filter) {
            StockFilter.ALL -> list
            StockFilter.LOW_STOCK -> list.filter { it.stockQuantity in 1..10 }
            StockFilter.OUT_OF_STOCK -> list.filter { it.stockQuantity <= 0 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allDeliveries: StateFlow<List<DeliveryEntity>> = repository.allDeliveries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDeliveriesWithItems: StateFlow<List<DeliveryWithItems>> = repository.allDeliveriesWithItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unsyncedProductCount: StateFlow<Int> = repository.getUnsyncedProductCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncNow() {
        _isSyncing.value = true
        GoogleSheetSyncWorker.triggerImmediateSync(getApplication())
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _isSyncing.value = false
            _toastMessage.value = "Sync job dispatched"
        }
    }

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    private val _productForm = MutableStateFlow(ProductFormState())
    val productForm: StateFlow<ProductFormState> = _productForm.asStateFlow()

    private val _isBarcodeScannerOpen = MutableStateFlow(false)
    val isBarcodeScannerOpen: StateFlow<Boolean> = _isBarcodeScannerOpen.asStateFlow()

    private val _isDeliveryDialogOpen = MutableStateFlow(false)
    val isDeliveryDialogOpen: StateFlow<Boolean> = _isDeliveryDialogOpen.asStateFlow()

    private val _deliveryForm = MutableStateFlow(DeliveryFormState())
    val deliveryForm: StateFlow<DeliveryFormState> = _deliveryForm.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun addNewCategory(categoryName: String) {
        viewModelScope.launch {
            repository.insertCategory(categoryName)
            _toastMessage.value = "Category '$categoryName' added"
        }
    }

    fun openAddProductDialog(productToEdit: ProductEntity? = null) {
        if (productToEdit != null) {
            _productForm.value = ProductFormState(
                id = productToEdit.id,
                name = productToEdit.name,
                barcode = productToEdit.barcode,
                category = productToEdit.category,
                imagePath = productToEdit.imagePath,
                costPrice = if (productToEdit.costPrice > 0) productToEdit.costPrice.toString() else "",
                retailPrice = if (productToEdit.retailPrice > 0) productToEdit.retailPrice.toString() else "",
                stockQuantity = productToEdit.stockQuantity.toString()
            )
        } else {
            val defaultCat = if (_selectedCategoryFilter.value != "All" && _selectedCategoryFilter.value.isNotBlank()) {
                _selectedCategoryFilter.value
            } else "General"
            _productForm.value = ProductFormState(category = defaultCat)
        }
        _isAddProductDialogOpen.value = true
    }

    fun closeAddProductDialog() {
        _isAddProductDialogOpen.value = false
        _productForm.value = ProductFormState()
    }

    fun updateProductForm(form: ProductFormState) {
        _productForm.value = form
    }

    fun openScannerForBarcode() {
        _isBarcodeScannerOpen.value = true
    }

    fun closeScannerForBarcode() {
        _isBarcodeScannerOpen.value = false
    }

    fun onBarcodeScanned(barcode: String) {
        _productForm.value = _productForm.value.copy(barcode = barcode)
        _isBarcodeScannerOpen.value = false
        _toastMessage.value = "Barcode scanned: $barcode"
    }

    fun saveProductImageFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val persistentUri = ImageStorageHelper.saveImageFromUri(context, sourceUri)
            if (persistentUri != null) {
                withContext(Dispatchers.Main) {
                    _productForm.value = _productForm.value.copy(imagePath = persistentUri)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Failed to copy image to persistent storage"
                }
            }
        }
    }

    fun setProductImage(imageUri: String?) {
        _productForm.value = _productForm.value.copy(imagePath = imageUri)
    }

    fun saveProduct() {
        val form = _productForm.value
        if (form.name.isBlank()) {
            _toastMessage.value = "Please enter a product name"
            return
        }
        val cost = form.costPrice.toDoubleOrNull() ?: 0.0
        val retail = form.retailPrice.toDoubleOrNull() ?: 0.0
        val stock = form.stockQuantity.toIntOrNull() ?: 0
        val cat = form.category.ifBlank { "General" }

        viewModelScope.launch {
            // Also ensure category exists in category repository
            repository.insertCategory(cat)

            val entity = ProductEntity(
                id = form.id,
                name = form.name.trim(),
                barcode = form.barcode.trim(),
                category = cat.trim(),
                imagePath = form.imagePath,
                costPrice = cost,
                retailPrice = retail,
                stockQuantity = stock,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateProduct(entity)
            closeAddProductDialog()
            _toastMessage.value = if (form.id == 0L) "Product added" else "Product updated"

            // Auto-trigger sync if cloud link is configured
            val settings = repository.getSettingsDirect()
            if (settings.googleSheetLink.isNotBlank() && settings.googleSheetLink.startsWith("http")) {
                GoogleSheetSyncWorker.triggerImmediateSync(getApplication())
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _toastMessage.value = "Product removed"
        }
    }

    fun openDeliveryDialog() {
        _deliveryForm.value = DeliveryFormState()
        _isDeliveryDialogOpen.value = true
    }

    fun closeDeliveryDialog() {
        _isDeliveryDialogOpen.value = false
        _deliveryForm.value = DeliveryFormState()
    }

    fun updateDeliveryForm(form: DeliveryFormState) {
        _deliveryForm.value = form
    }

    fun addProductToDelivery(productId: Long, productName: String, quantity: Int, unitCost: Double) {
        if (quantity <= 0) return
        val current = _deliveryForm.value
        val existingIndex = current.items.indexOfFirst { it.productId == productId }
        val updatedItems = if (existingIndex >= 0) {
            current.items.toMutableList().apply {
                val existing = this[existingIndex]
                this[existingIndex] = existing.copy(
                    quantity = existing.quantity + quantity,
                    unitCost = if (unitCost > 0) unitCost else existing.unitCost
                )
            }
        } else {
            current.items + DeliveryItemDraft(
                productId = productId,
                productName = productName,
                quantity = quantity,
                unitCost = unitCost
            )
        }
        val calculatedTotal = updatedItems.sumOf { it.quantity * it.unitCost }
        _deliveryForm.value = current.copy(
            items = updatedItems,
            totalCostInput = if (current.totalCostInput.isBlank() || current.totalCostInput == "0.0") {
                String.format(java.util.Locale.US, "%.2f", calculatedTotal)
            } else current.totalCostInput,
            selectedProductId = null,
            draftQuantity = "1",
            draftUnitCost = ""
        )
    }

    fun removeProductFromDelivery(tempId: String) {
        val current = _deliveryForm.value
        val updated = current.items.filterNot { it.tempId == tempId }
        val calculatedTotal = updated.sumOf { it.quantity * it.unitCost }
        _deliveryForm.value = current.copy(
            items = updated,
            totalCostInput = String.format(java.util.Locale.US, "%.2f", calculatedTotal)
        )
    }

    fun saveDelivery() {
        val form = _deliveryForm.value
        if (form.supplierName.isBlank()) {
            _toastMessage.value = "Please enter supplier name"
            return
        }
        if (form.items.isEmpty()) {
            _toastMessage.value = "Please add at least one product to this delivery"
            return
        }
        val calculatedCost = form.items.sumOf { it.quantity * it.unitCost }
        val totalCost = form.totalCostInput.toDoubleOrNull() ?: calculatedCost

        viewModelScope.launch {
            val delivery = DeliveryEntity(
                supplierName = form.supplierName.trim(),
                totalCost = totalCost,
                notes = form.notes.trim()
            )
            val items = form.items.map {
                DeliveryItemEntity(
                    deliveryId = 0,
                    productId = it.productId,
                    productName = it.productName,
                    quantityAdded = it.quantity,
                    unitCost = it.unitCost
                )
            }
            repository.logDelivery(delivery, items)
            closeDeliveryDialog()
            _toastMessage.value = "Delivery with ${items.size} products logged successfully"
        }
    }
}
