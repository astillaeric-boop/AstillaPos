package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.util.CreditReceiptHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MessengerBlue = Color(0xFF0084FF)
private val SmsGreen = Color(0xFF0F9D58)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailSheet(
    customer: CustomerEntity,
    transactions: List<CreditTransactionEntity>,
    settings: AppSettingsEntity,
    onBack: () -> Unit,
    onRecordBorrow: () -> Unit,
    onRecordPayment: () -> Unit,
    onEditCustomer: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onViewReceipt: (CreditTransactionEntity) -> Unit,
    onSendStatement: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_detail_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEditCustomer,
                        modifier = Modifier.testTag("edit_customer_top_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(
                        onClick = onDeleteCustomer,
                        modifier = Modifier.testTag("delete_customer_top_btn")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Customer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.currentBalance > 0) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Utang Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₱${String.format(Locale.US, "%,.2f", customer.currentBalance)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else SmsGreen,
                                    modifier = Modifier.testTag("customer_balance_text")
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (customer.currentBalance > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        else SmsGreen.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (customer.currentBalance > 0) "HAS UTANG" else "CLEARED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else SmsGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone & Messenger Contacts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = customer.phoneNumber.ifBlank { "No phone" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MessengerBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = customer.messengerContact.ifBlank { "No messenger" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MessengerBlue
                                )
                            }
                        }
                    }
                }
            }

            // Quick Transaction Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRecordBorrow,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_borrow_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Utang", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onRecordPayment,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_payment_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SmsGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayad", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Reminder & Statement Buttons (if customer has active utang)
            if (customer.currentBalance > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Send Statement of Account Reminder",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val text = CreditReceiptHelper.buildStatementReminder(settings, customer)
                                        CreditReceiptHelper.sendNativeSms(context, customer.phoneNumber, text)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("send_sms_statement_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SmsGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SIM SMS", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val text = CreditReceiptHelper.buildStatementReminder(settings, customer)
                                        CreditReceiptHelper.openMessengerReceipt(context, customer.messengerContact, text)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("send_messenger_statement_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MessengerBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Messenger", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = onSendStatement,
                                    modifier = Modifier.testTag("preview_statement_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section Header: Transaction History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transaction History (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions recorded yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val isBorrow = tx.transactionType.equals("BORROW", ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_item_${tx.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isBorrow) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else SmsGreen.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isBorrow) "BORROW" else "PAYMENT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (isBorrow) MaterialTheme.colorScheme.error else SmsGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormatter.format(Date(tx.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${if (isBorrow) "+" else "-"}₱${String.format(Locale.US, "%,.2f", tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isBorrow) MaterialTheme.colorScheme.error else SmsGreen
                                )
                            }

                            if (tx.itemSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tx.itemSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Balance: ₱${String.format(Locale.US, "%,.2f", tx.remainingBalance)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { onViewReceipt(tx) },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("view_receipt_tx_${tx.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receipt", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
