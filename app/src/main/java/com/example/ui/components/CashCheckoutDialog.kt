package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil

@Composable
fun CashCheckoutDialog(
    totalAmount: Double,
    currency: String,
    onConfirm: (cashTendered: Double, changeDue: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var cashInput by remember { mutableStateOf("") }

    val tenderedAmount by remember(cashInput) {
        derivedStateOf {
            cashInput.toDoubleOrNull() ?: 0.0
        }
    }

    val changeDue by remember(tenderedAmount, totalAmount) {
        derivedStateOf {
            tenderedAmount - totalAmount
        }
    }

    val isSufficient = tenderedAmount >= totalAmount && totalAmount > 0

    // Quick tender suggestions: exact, next rounded bills, and standard denominations
    val quickOptions by remember(totalAmount) {
        derivedStateOf {
            val list = mutableListOf<Double>()
            // Exact amount
            if (totalAmount > 0) list.add(totalAmount)

            // Common Philippine bills / steps
            val bills = listOf(20.0, 50.0, 100.0, 200.0, 500.0, 1000.0)
            for (bill in bills) {
                if (bill > totalAmount && !list.contains(bill)) {
                    list.add(bill)
                }
            }
            // Next round 50 or 100 if totalAmount is unusual
            val next50 = (ceil(totalAmount / 50.0) * 50.0)
            if (next50 > totalAmount && !list.contains(next50)) {
                list.add(next50)
            }
            val next100 = (ceil(totalAmount / 100.0) * 100.0)
            if (next100 > totalAmount && !list.contains(next100)) {
                list.add(next100)
            }
            list.sorted()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cash Payment & Change",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Total Payable Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL PAYABLE AMOUNT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currency${String.format(Locale.US, "%,.2f", totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("cash_total_payable")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cash Received / Tendered Input Field
                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { input ->
                        // Allow only digits and decimal point
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            cashInput = input
                        }
                    },
                    label = { Text("Cash Received / Tendered *") },
                    placeholder = { Text("0.00") },
                    prefix = { Text(currency, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = cashInput.isNotEmpty() && !isSufficient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_tendered_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick-Tender Suggestion Chips
                Text(
                    text = "Quick Cash Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.forEach { amount ->
                        val isExact = (amount == totalAmount)
                        val isSelected = (tenderedAmount == amount)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                cashInput = if (amount % 1.0 == 0.0) {
                                    amount.toLong().toString()
                                } else {
                                    String.format(Locale.US, "%.2f", amount)
                                }
                            },
                            label = {
                                Text(
                                    text = if (isExact) "Exact ($currency${String.format(Locale.US, "%.0f", amount)})" else "$currency${String.format(Locale.US, "%.0f", amount)}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || isExact) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("quick_cash_chip_${amount.toLong()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Change Calculation Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        cashInput.isBlank() -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        isSufficient -> Color(0xFFDCFCE7) // Emerald light background
                        else -> Color(0xFFFEE2E2) // Red light background
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            cashInput.isBlank() -> {
                                Text(
                                    text = "Enter cash received above",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            isSufficient -> {
                                Text(
                                    text = "CHANGE DUE TO CUSTOMER",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currency${String.format(Locale.US, "%,.2f", changeDue)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.testTag("cash_change_due_text")
                                )
                            }
                            else -> {
                                val remaining = totalAmount - tenderedAmount
                                Text(
                                    text = "INSUFFICIENT CASH",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Still needs $currency${String.format(Locale.US, "%,.2f", remaining)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                    modifier = Modifier.testTag("cash_insufficient_text")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSufficient) {
                        onConfirm(tenderedAmount, changeDue)
                    }
                },
                enabled = isSufficient,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("complete_cash_sale_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Complete Sale", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_cash_sale_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
