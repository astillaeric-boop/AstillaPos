package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.CategoryFilterRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.DeliveryWithItems
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.viewmodel.DeliveryFormState
import com.example.ui.viewmodel.DeliveryItemDraft
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.ProductFormState
import com.example.ui.viewmodel.StockFilter
import com.example.util.ImageStorageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val deliveriesWithItems by viewModel.allDeliveriesWithItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stockFilter by viewModel.stockFilter.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isAddDialogOpen by viewModel.isAddProductDialogOpen.collectAsStateWithLifecycle()
    val productForm by viewModel.productForm.collectAsStateWithLifecycle()
    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsStateWithLifecycle()
    val isDeliveryDialogOpen by viewModel.isDeliveryDialogOpen.collectAsStateWithLifecycle()
    val deliveryForm by viewModel.deliveryForm.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Tabs: Products vs Delivery Logs
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Inventory & Stock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${products.size} Products • ${deliveriesWithItems.size} Delivery Records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (selectedTabIndex == 0) {
                                    viewModel.openAddProductDialog()
                                } else {
                                    viewModel.openDeliveryDialog()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("inventory_primary_action_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedTabIndex == 0) "Product" else "Delivery")
                        }
                    }

                    SecondaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Product Catalog", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Delivery Log", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            if (selectedTabIndex == 0) {
                // Product Catalog View
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search and Filter row
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search catalog by name or SKU...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Filter Row
                        CategoryFilterRow(
                            categories = availableCategories,
                            selectedCategory = selectedCategoryFilter,
                            onCategorySelected = { viewModel.setCategoryFilter(it) },
                            testTagPrefix = "inventory_category_chip"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = stockFilter == StockFilter.ALL,
                                onClick = { viewModel.setStockFilter(StockFilter.ALL) },
                                label = { Text("All (${products.size})") }
                            )
                            FilterChip(
                                selected = stockFilter == StockFilter.LOW_STOCK,
                                onClick = { viewModel.setStockFilter(StockFilter.LOW_STOCK) },
                                label = { Text("Low Stock") }
                            )
                            FilterChip(
                                selected = stockFilter == StockFilter.OUT_OF_STOCK,
                                onClick = { viewModel.setStockFilter(StockFilter.OUT_OF_STOCK) },
                                label = { Text("Out of Stock") }
                            )
                        }
                    }

                    // Product List
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductInventoryCard(
                                product = product,
                                currency = settings.currencySymbol,
                                onEdit = { viewModel.openAddProductDialog(product) },
                                onDelete = { viewModel.deleteProduct(product) }
                            )
                        }
                    }
                }
            } else {
                // Delivery Log View
                DeliveryLogView(
                    deliveriesWithItems = deliveriesWithItems,
                    currency = settings.currencySymbol,
                    onAddDelivery = { viewModel.openDeliveryDialog() }
                )
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = {
                if (selectedTabIndex == 0) {
                    viewModel.openAddProductDialog()
                } else {
                    viewModel.openDeliveryDialog()
                }
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(if (selectedTabIndex == 0) "New Product" else "Log Delivery", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("inventory_fab")
        )

        // Add / Edit Product Dialog
        if (isAddDialogOpen) {
            AddEditProductDialog(
                form = productForm,
                currency = settings.currencySymbol,
                availableCategories = availableCategories,
                onFormChange = { viewModel.updateProductForm(it) },
                onScanBarcode = { viewModel.openScannerForBarcode() },
                onPickImageFromUri = { uri -> viewModel.saveProductImageFromUri(context, uri) },
                onDirectImageCaptured = { path -> viewModel.setProductImage(path) },
                onAddNewCategory = { viewModel.addNewCategory(it) },
                onSave = { viewModel.saveProduct() },
                onDismiss = { viewModel.closeAddProductDialog() }
            )
        }

        // Camera Barcode Scanner for Barcode Input
        if (isBarcodeScannerOpen) {
            CameraBarcodeScannerDialog(
                onBarcodeScanned = { code ->
                    viewModel.onBarcodeScanned(code)
                },
                onDismiss = { viewModel.closeScannerForBarcode() }
            )
        }

        // Add Multi-Product Delivery Dialog
        if (isDeliveryDialogOpen) {
            AddDeliveryDialog(
                form = deliveryForm,
                products = products,
                currency = settings.currencySymbol,
                onFormChange = { viewModel.updateDeliveryForm(it) },
                onAddItem = { productId, name, qty, cost ->
                    viewModel.addProductToDelivery(productId, name, qty, cost)
                },
                onRemoveItem = { tempId ->
                    viewModel.removeProductFromDelivery(tempId)
                },
                onSave = { viewModel.saveDelivery() },
                onDismiss = { viewModel.closeDeliveryDialog() }
            )
        }
    }
}

@Composable
fun ProductInventoryCard(
    product: ProductEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "SKU: ${product.barcode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sell: $currency${String.format(Locale.US, "%.2f", product.retailPrice)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Cost: $currency${String.format(Locale.US, "%.2f", product.costPrice)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // Stock badge & actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        product.stockQuantity <= 0 -> Color(0xFFFEE2E2)
                        product.stockQuantity <= 10 -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                ) {
                    Text(
                        text = "${product.stockQuantity} in stock",
                        color = when {
                            product.stockQuantity <= 0 -> Color(0xFFDC2626)
                            product.stockQuantity <= 10 -> Color(0xFFB45309)
                            else -> Color(0xFF15803D)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryLogView(
    deliveriesWithItems: List<DeliveryWithItems>,
    currency: String,
    onAddDelivery: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    if (deliveriesWithItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Deliveries Logged",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Log supplier restocks to receive multiple products per invoice and track cost accounting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddDelivery) {
                    Text("Log Incoming Delivery")
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(deliveriesWithItems, key = { it.delivery.id }) { itemRecord ->
                val delivery = itemRecord.delivery
                val items = itemRecord.items

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = delivery.supplierName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateFormat.format(Date(delivery.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${String.format(Locale.US, "%.2f", delivery.totalCost)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${items.size} ${if (items.size == 1) "product" else "products"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    items.forEach { line ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${line.quantityAdded}x ${line.productName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = if (line.unitCost > 0) "@ $currency${String.format(Locale.US, "%.2f", line.unitCost)}" else "",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (delivery.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notes: ${delivery.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeliveryDialog(
    form: DeliveryFormState,
    products: List<ProductEntity>,
    currency: String,
    onFormChange: (DeliveryFormState) -> Unit,
    onAddItem: (Long, String, Int, Double) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var productDropdownExpanded by remember { mutableStateOf(false) }
    val selectedProduct = products.find { it.id == form.selectedProductId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Supplier Delivery", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.supplierName,
                    onValueChange = { onFormChange(form.copy(supplierName = it)) },
                    label = { Text("Supplier Name *") },
                    placeholder = { Text("e.g. Highland Beverage Co.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Section: Products Delivered from this Supplier
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Products in Delivery (${form.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (form.items.isEmpty()) {
                            Text(
                                text = "Select products below to add to this supplier delivery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                form.items.forEach { draftItem ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(draftItem.productName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text(
                                                "${draftItem.quantity} pcs @ $currency${String.format(Locale.US, "%.2f", draftItem.unitCost)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "$currency${String.format(Locale.US, "%.2f", draftItem.quantity * draftItem.unitCost)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        IconButton(
                                            onClick = { onRemoveItem(draftItem.tempId) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Product subsection
                        Text("Add Product to Delivery:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: "Select a product...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false }
                            ) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (Stock: ${p.stockQuantity})") },
                                        onClick = {
                                            onFormChange(
                                                form.copy(
                                                    selectedProductId = p.id,
                                                    draftUnitCost = if (p.costPrice > 0) p.costPrice.toString() else form.draftUnitCost
                                                )
                                            )
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedProduct != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = form.draftQuantity,
                                    onValueChange = { onFormChange(form.copy(draftQuantity = it)) },
                                    label = { Text("Qty Added") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = form.draftUnitCost,
                                    onValueChange = { onFormChange(form.copy(draftUnitCost = it)) },
                                    label = { Text("Cost ($currency)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val qty = form.draftQuantity.toIntOrNull() ?: 1
                                    val cost = form.draftUnitCost.toDoubleOrNull() ?: selectedProduct.costPrice
                                    onAddItem(selectedProduct.id, selectedProduct.name, qty, cost)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Product to Delivery")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = form.totalCostInput,
                    onValueChange = { onFormChange(form.copy(totalCostInput = it)) },
                    label = { Text("Total Invoice Cost ($currency) *") },
                    placeholder = { Text("Auto-calculated from items") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { onFormChange(form.copy(notes = it)) },
                    label = { Text("Delivery / Invoice Notes (Optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save Delivery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

