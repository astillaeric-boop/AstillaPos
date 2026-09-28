package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleWithItems
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PrinterManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Search & Quick Scan
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.storeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Point of Sale • Ready to scan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Top scanner icon button for quick access
                        IconButton(
                            onClick = { viewModel.openScanner() },
                            modifier = Modifier.testTag("top_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search by name, SKU, or barcode...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_search_input")
                    )
                }
            }

            // Products Grid
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No products in catalog yet" else "No matching items found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan a barcode or add products in the Inventory tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductPosCard(
                            product = product,
                            currency = settings.currencySymbol,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Bottom Sticky Cart Action Bar (Single-Handed Thumb Reach)
        AnimatedVisibility(
            visible = uiState.cartItems.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { viewModel.openCartSheet() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${uiState.cartItems.sumOf { it.quantity }}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Cart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${settings.currencySymbol}${String.format(Locale.US, "%.2f", uiState.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Button(
                        onClick = { viewModel.openCartSheet() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("view_cart_button")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checkout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Camera Barcode Scanner Button
        ExtendedFloatingActionButton(
            onClick = { viewModel.openScanner() },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode"
                )
            },
            text = { Text("Scan SKU", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (uiState.cartItems.isNotEmpty()) 90.dp else 24.dp
                )
                .testTag("floating_scanner_button")
        )

        // Integrated CameraX Barcode Scanner Modal
        if (uiState.isScannerOpen) {
            CameraBarcodeScannerDialog(
                onBarcodeScanned = { code ->
                    viewModel.onBarcodeScanned(code)
                },
                onDismiss = { viewModel.closeScanner() }
            )
        }

        // Cart Modal Bottom Sheet
        if (uiState.isCartSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeCartSheet() },
                sheetState = cartSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CartSheetContent(
                    uiState = uiState,
                    settings = settings,
                    customers = customers,
                    onIncrement = { viewModel.addToCart(it) },
                    onDecrement = { viewModel.decrementQuantity(it) },
                    onRemove = { viewModel.removeFromCart(it) },
                    onClear = { viewModel.clearCart() },
                    onSelectPayment = { viewModel.setPaymentType(it) },
                    onSelectCreditCustomer = { viewModel.setSelectedCreditCustomerId(it) },
                    onCheckout = { viewModel.checkout() },
                    onClose = { viewModel.closeCartSheet() }
                )
            }
        }

        // Printable Receipt Dialog
        if (uiState.isReceiptDialogOpen && uiState.lastCompletedSale != null) {
            ReceiptDialog(
                settings = settings,
                saleWithItems = uiState.lastCompletedSale!!,
                onDismiss = { viewModel.dismissReceiptDialog() },
                onPrintPdf = {
                    val html = PrinterManager.generateReceiptHtml(
                        settings = settings,
                        sale = uiState.lastCompletedSale!!.sale,
                        items = uiState.lastCompletedSale!!.items
                    )
                    PrinterManager.printHtmlDocument(
                        context = context,
                        jobName = "Receipt_${uiState.lastCompletedSale!!.sale.id}",
                        htmlContent = html
                    )
                },
                onThermalShare = {
                    val escBytes = PrinterManager.buildEscPosReceiptBytes(
                        settings = settings,
                        sale = uiState.lastCompletedSale!!.sale,
                        items = uiState.lastCompletedSale!!.items
                    )
                    val textSummary = buildReceiptPlainText(
                        settings,
                        uiState.lastCompletedSale!!.sale,
                        uiState.lastCompletedSale!!.items
                    )
                    PrinterManager.shareReceiptText(context, textSummary)
                }
            )
        }
    }
}

@Composable
fun ProductPosCard(
    product: ProductEntity,
    currency: String,
    onAddToCart: () -> Unit
) {
    Card(
        onClick = onAddToCart,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_product_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Product photo or fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
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
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                // Low stock indicator badge
                if (product.stockQuantity <= 10) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (product.stockQuantity <= 0) Color(0xFFDC2626) else Color(0xFFD97706),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (product.stockQuantity <= 0) "OUT" else "${product.stockQuantity} left",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", product.retailPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to cart",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartSheetContent(
    uiState: PosUiState,
    settings: AppSettingsEntity,
    customers: List<CustomerEntity>,
    onIncrement: (ProductEntity) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onSelectPayment: (String) -> Unit,
    onSelectCreditCustomer: (Long?) -> Unit,
    onCheckout: () -> Unit,
    onClose: () -> Unit
) {
    val currency = settings.currencySymbol

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Shopping Cart (${uiState.cartItems.sumOf { it.quantity }})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onClear) {
                Text("Clear All", color = Color(0xFFDC2626))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cart items list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(240.dp)
        ) {
            items(uiState.cartItems, key = { it.product.id }) { item ->
                CartItemRow(
                    cartItem = item,
                    currency = currency,
                    onIncrement = { onIncrement(item.product) },
                    onDecrement = { onDecrement(item.product.id) },
                    onRemove = { onRemove(item.product.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Type Selector
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Cash", "Card", "GCash", "Credit (Utang)").forEach { type ->
                val isSelected = uiState.selectedPaymentType == type
                FilledTonalButton(
                    onClick = { onSelectPayment(type) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Customer selection when Credit (Utang) is selected
        if (uiState.selectedPaymentType == "Credit (Utang)") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Charge Utang to Customer:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (customers.isEmpty()) {
                Text(
                    text = "No customers registered yet. Please add a customer in the Utang tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customers.forEach { customer ->
                        val isSelected = uiState.selectedCreditCustomerId == customer.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCreditCustomer(customer.id) },
                            label = {
                                Text(
                                    "${customer.name} (₱${String.format(Locale.US, "%,.2f", customer.currentBalance)})",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Price Breakdown
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency${String.format(Locale.US, "%.2f", uiState.subtotal)}", fontWeight = FontWeight.SemiBold)
                }

                if (settings.taxEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax (${settings.taxRate}%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$currency${String.format(Locale.US, "%.2f", uiState.taxAmount)}", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", uiState.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCheckout,
            enabled = uiState.cartItems.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("checkout_confirm_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Sale • $currency${String.format(Locale.US, "%.2f", uiState.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    currency: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", cartItem.product.retailPrice)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${cartItem.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    settings: AppSettingsEntity,
    saleWithItems: SaleWithItems,
    onDismiss: () -> Unit,
    onPrintPdf: () -> Unit,
    onThermalShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(saleWithItems.sale.timestamp))
    val currency = settings.currencySymbol

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = settings.storeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Receipt #${saleWithItems.sale.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        saleWithItems.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.productName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currency${String.format(Locale.US, "%.2f", item.unitPrice * item.quantity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (settings.taxEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tax (${settings.taxRate}%):", style = MaterialTheme.typography.bodySmall)
                                Text("$currency${String.format(Locale.US, "%.2f", saleWithItems.sale.taxAmount)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid (${saleWithItems.sale.paymentType}):", fontWeight = FontWeight.Bold)
                            Text(
                                "$currency${String.format(Locale.US, "%.2f", saleWithItems.sale.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Printing Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Print", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onThermalShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thermal/Share", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Sale")
            }
        }
    )
}

private fun buildReceiptPlainText(
    settings: AppSettingsEntity,
    sale: com.example.data.local.entity.SaleEntity,
    items: List<com.example.data.local.entity.SaleItemEntity>
): String {
    val sb = StringBuilder()
    sb.appendLine("================================")
    sb.appendLine(settings.storeName.uppercase())
    sb.appendLine("OFFICIAL RECEIPT #${sale.id}")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    sb.appendLine(dateFormat.format(Date(sale.timestamp)))
    sb.appendLine("--------------------------------")
    for (item in items) {
        sb.appendLine("${item.quantity}x ${item.productName}")
        sb.appendLine("   @ ${settings.currencySymbol}${item.unitPrice} = ${settings.currencySymbol}${item.unitPrice * item.quantity}")
    }
    sb.appendLine("--------------------------------")
    if (settings.taxEnabled) {
        sb.appendLine("Tax: ${settings.currencySymbol}${sale.taxAmount}")
    }
    sb.appendLine("TOTAL: ${settings.currencySymbol}${sale.totalAmount}")
    sb.appendLine("Paid via: ${sale.paymentType}")
    sb.appendLine("================================")
    sb.appendLine("Thank you for your purchase!")
    return sb.toString()
}
