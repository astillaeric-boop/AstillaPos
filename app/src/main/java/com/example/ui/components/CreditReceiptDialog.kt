package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.util.CreditReceiptHelper

// Facebook Messenger Brand Blue
private val MessengerBlue = Color(0xFF0084FF)
private val SmsGreen = Color(0xFF0F9D58)

@Composable
fun CreditReceiptDialog(
    customer: CustomerEntity,
    transaction: CreditTransactionEntity?, // Null means statement of account reminder
    settings: AppSettingsEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val receiptText = remember(customer, transaction, settings) {
        if (transaction != null) {
            CreditReceiptHelper.buildTransactionReceipt(settings, customer, transaction)
        } else {
            CreditReceiptHelper.buildStatementReminder(settings, customer)
        }
    }

    val isBorrow = transaction?.transactionType.equals("BORROW", ignoreCase = true)
    val isPayment = transaction?.transactionType.equals("PAYMENT", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("credit_receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Receipt",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when {
                                    isBorrow -> "Utang / Borrow Receipt"
                                    isPayment -> "Payment Receipt"
                                    else -> "Statement Reminder"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recipient: ${customer.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt Paper Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("receipt_text_preview")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (customer.phoneNumber.isNotBlank()) "SIM: ${customer.phoneNumber}" else "No phone saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customer.phoneNumber.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (customer.messengerContact.isNotBlank()) "Messenger: ${customer.messengerContact}" else "No messenger saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customer.messengerContact.isNotBlank()) MessengerBlue else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: SMS & Facebook Messenger
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Native SIM SMS
                    Button(
                        onClick = {
                            CreditReceiptHelper.sendNativeSms(
                                context = context,
                                phoneNumber = customer.phoneNumber,
                                messageText = receiptText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_native_sms_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SmsGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "SMS",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send via Native SIM SMS",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Button 2: Facebook Messenger
                    Button(
                        onClick = {
                            CreditReceiptHelper.openMessengerReceipt(
                                context = context,
                                messengerContact = customer.messengerContact,
                                messageText = receiptText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_messenger_receipt_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MessengerBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Messenger",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send via Facebook Messenger",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Secondary action buttons: Share & Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                CreditReceiptHelper.shareReceipt(
                                    context = context,
                                    subject = "Credit Receipt - ${customer.name}",
                                    messageText = receiptText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_receipt_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Other Apps", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                CreditReceiptHelper.copyToClipboard(
                                    context = context,
                                    label = "Utang Receipt",
                                    text = receiptText
                                )
                                android.widget.Toast.makeText(context, "Receipt copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("copy_receipt_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
