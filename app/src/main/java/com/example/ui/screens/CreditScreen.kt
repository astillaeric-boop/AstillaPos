package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.ui.components.AddEditCustomerDialog
import com.example.ui.components.CreditReceiptDialog
import com.example.ui.components.CustomerDetailSheet
import com.example.ui.components.RecordBorrowDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.viewmodel.CreditViewModel
import java.util.Locale

private val MessengerBlue = Color(0xFF0084FF)
private val SmsGreen = Color(0xFF0F9D58)

@Composable
fun CreditScreen(
    viewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val totalOutstanding by viewModel.totalOutstandingCredit.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()

    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val selectedCustomerTransactions by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("ALL") } // ALL, WITH_BALANCE, CLEARED

    val filteredCustomers = remember(customers, filterType) {
        when (filterType) {
            "WITH_BALANCE" -> customers.filter { it.currentBalance > 0 }
            "CLEARED" -> customers.filter { it.currentBalance <= 0 }
            else -> customers
        }
    }

    val debtorsCount = remember(customers) {
        customers.count { it.currentBalance > 0 }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // If customer detail is open, show CustomerDetailSheet
    if (selectedCustomer != null) {
        BackHandler {
            viewModel.selectCustomer(null)
        }
        CustomerDetailSheet(
            customer = selectedCustomer!!,
            transactions = selectedCustomerTransactions,
            settings = settings,
            onBack = { viewModel.selectCustomer(null) },
            onRecordBorrow = { viewModel.openBorrowDialog(selectedCustomer!!) },
            onRecordPayment = { viewModel.openPaymentDialog(selectedCustomer!!) },
            onEditCustomer = { viewModel.openEditCustomerDialog(selectedCustomer!!) },
            onDeleteCustomer = { viewModel.confirmDeleteCustomer(selectedCustomer!!) },
            onViewReceipt = { tx -> viewModel.openReceiptForTransaction(selectedCustomer!!, tx) },
            onSendStatement = { viewModel.openReceiptForTransaction(selectedCustomer!!, null) }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openAddCustomerDialog() },
                    modifier = Modifier.testTag("add_customer_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Total Receivables / Utang Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("total_utang_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Total Outstanding Utang",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (unsyncedCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "$unsyncedCount pending",
                                                color = Color(0xFFB45309),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    if (settings.googleSheetLink.isNotBlank() && settings.googleSheetLink.startsWith("http")) {
                                        IconButton(
                                            onClick = { viewModel.syncNow() },
                                            enabled = !isSyncing,
                                            modifier = Modifier.size(36.dp).testTag("credit_sync_now_btn")
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (unsyncedCount > 0) Icons.Default.CloudSync else Icons.Default.Sync,
                                                    contentDescription = "Sync Utang to Cloud",
                                                    tint = if (unsyncedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$debtorsCount debtors",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "₱${String.format(Locale.US, "%,.2f", totalOutstanding)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("total_utang_amount")
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Accumulated credit across ${customers.size} registered customers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input"),
                        placeholder = { Text("Search name, phone, or messenger...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${customers.size})",
                            "WITH_BALANCE" to "With Utang ($debtorsCount)",
                            "CLEARED" to "Cleared (${customers.size - debtorsCount})"
                        ).forEach { (type, label) ->
                            val isSelected = filterType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { filterType = type }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Customer List
                if (filteredCustomers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No customers match '$searchQuery'" else "No customers found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the '+' button below to add your first customer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerListItem(
                            customer = customer,
                            onClick = { viewModel.selectCustomer(customer) },
                            onBorrow = { viewModel.openBorrowDialog(customer) },
                            onPayment = { viewModel.openPaymentDialog(customer) },
                            onReceipt = { viewModel.openReceiptForTransaction(customer, null) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showAddEditCustomerDialog) {
        AddEditCustomerDialog(
            customer = uiState.editingCustomer,
            onDismiss = { viewModel.closeAddEditCustomerDialog() },
            onSave = { id, name, phone, messenger ->
                viewModel.saveCustomer(id, name, phone, messenger)
            }
        )
    }

    if (uiState.showBorrowDialog && uiState.targetCustomer != null) {
        RecordBorrowDialog(
            customer = uiState.targetCustomer!!,
            availableProducts = products,
            onDismiss = { viewModel.closeBorrowDialog() },
            onConfirm = { customerId, items ->
                viewModel.recordBorrow(customerId, items)
            }
        )
    }

    if (uiState.showPaymentDialog && uiState.targetCustomer != null) {
        RecordPaymentDialog(
            customer = uiState.targetCustomer!!,
            onDismiss = { viewModel.closePaymentDialog() },
            onConfirm = { customerId, amount, paymentMethod ->
                viewModel.recordPayment(customerId, amount, paymentMethod)
            }
        )
    }

    if (uiState.showReceiptDialog && uiState.receiptCustomer != null) {
        CreditReceiptDialog(
            customer = uiState.receiptCustomer!!,
            transaction = uiState.receiptTransaction,
            settings = settings,
            onDismiss = { viewModel.closeReceiptDialog() }
        )
    }

    if (uiState.showDeleteConfirm && uiState.customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteConfirm() },
            title = { Text("Delete Customer?") },
            text = {
                Text("Are you sure you want to delete '${uiState.customerToDelete?.name}' and all their credit transaction history? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCustomer() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteConfirm() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomerListItem(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onBorrow: () -> Unit,
    onPayment: () -> Unit,
    onReceipt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (customer.phoneNumber.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customer.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (customer.messengerContact.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MessengerBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customer.messengerContact,
                                style = MaterialTheme.typography.bodySmall,
                                color = MessengerBlue
                            )
                        }
                    }
                }

                // Balance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", customer.currentBalance)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else SmsGreen
                    )
                    Text(
                        text = if (customer.currentBalance > 0) "UTANG" else "CLEARED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else SmsGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBorrow,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("borrow_btn_${customer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Utang", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }

                OutlinedButton(
                    onClick = onPayment,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("payment_btn_${customer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SmsGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bayad", fontSize = 11.sp, color = SmsGreen)
                }

                OutlinedButton(
                    onClick = onReceipt,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("receipt_btn_${customer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send", fontSize = 11.sp)
                }
            }
        }
    }
}
