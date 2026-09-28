package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.sync.GoogleAppsScriptTemplate
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SapphirePrimary
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.components.SupportMeDialog

import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val showScriptDialog by viewModel.showScriptDialog.collectAsStateWithLifecycle()
    val showResetDialog by viewModel.showResetDialog.collectAsStateWithLifecycle()
    val showSecondResetConfirm by viewModel.showSecondResetConfirm.collectAsStateWithLifecycle()
    val showSupportDialog by viewModel.showSupportDialog.collectAsStateWithLifecycle()

    var storeNameInput by remember(settings.storeName) { mutableStateOf(settings.storeName) }
    var businessAddressInput by remember(settings.businessAddress) { mutableStateOf(settings.businessAddress) }
    var zipCodeInput by remember(settings.zipCode) { mutableStateOf(settings.zipCode) }
    var phoneNumberInput by remember(settings.phoneNumber) { mutableStateOf(settings.phoneNumber) }
    var taxRateInput by remember(settings.taxRate) { mutableStateOf(settings.taxRate.toString()) }
    var sheetUrlInput by remember(settings.googleSheetLink) { mutableStateOf(settings.googleSheetLink) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Text(
            text = "Store Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Customize identity, financials, sync, and database",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Support Developer / Support Me Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Support the Developer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Donate via Maya or GCash to support updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.openSupportDialog() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48)
                    ),
                    modifier = Modifier.testTag("support_me_button")
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Support Me",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Store Customization Card (Includes Store Name, Address, Zip Code, Phone)
        SettingsSectionCard(title = "Store Identity & Details", icon = Icons.Default.Store) {
            OutlinedTextField(
                value = storeNameInput,
                onValueChange = {
                    storeNameInput = it
                    viewModel.updateStoreName(it)
                },
                label = { Text("Store Name (Printed on Receipt)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = businessAddressInput,
                onValueChange = {
                    businessAddressInput = it
                    viewModel.updateBusinessAddress(it)
                },
                label = { Text("Business Address") },
                placeholder = { Text("e.g. 123 Rizal Ave, Brgy. Poblacion") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_address_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = zipCodeInput,
                    onValueChange = {
                        zipCodeInput = it
                        viewModel.updateZipCode(it)
                    },
                    label = { Text("Zip Code") },
                    placeholder = { Text("1000") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("zip_code_input")
                )

                OutlinedTextField(
                    value = phoneNumberInput,
                    onValueChange = {
                        phoneNumberInput = it
                        viewModel.updatePhoneNumber(it)
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("0912 345 6789") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("phone_number_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Financials & Taxes Card
        SettingsSectionCard(title = "Financials & Tax", icon = Icons.Default.AttachMoney) {
            Text(
                text = "Currency Symbol",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val currencyOptions = listOf("₱", "$", "€", "£", "¥", "₹")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                currencyOptions.forEach { sym ->
                    val isSelected = settings.currencySymbol == sym
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateCurrencySymbol(sym) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = sym,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Apply Sales Tax", fontWeight = FontWeight.SemiBold)
                    Text("Automatically compute tax on checkout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.taxEnabled,
                    onCheckedChange = { viewModel.toggleTax(it) },
                    modifier = Modifier.testTag("tax_switch")
                )
            }

            if (settings.taxEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = taxRateInput,
                    onValueChange = {
                        taxRateInput = it
                        it.toFloatOrNull()?.let { rate -> viewModel.updateTaxRate(rate) }
                    },
                    label = { Text("Tax Rate (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Audio & Hardware Feedback
        SettingsSectionCard(title = "Audio Feedback", icon = Icons.Default.VolumeUp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Barcode Scanner Sound", fontWeight = FontWeight.SemiBold)
                    Text("Play upbeat audio beep on scan success", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.soundEnabled,
                    onCheckedChange = { viewModel.toggleSound(it) },
                    modifier = Modifier.testTag("sound_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Color Theme Manager
        SettingsSectionCard(title = "App Color Theme", icon = Icons.Default.ColorLens) {
            val themes = listOf(
                Pair("Emerald", EmeraldPrimary),
                Pair("Sapphire", SapphirePrimary),
                Pair("Amber", AmberPrimary),
                Pair("Crimson", CrimsonPrimary)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                themes.forEachIndexed { index, (name, color) ->
                    val isSelected = settings.themeColor == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateTheme(index) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Google Sheets Sync Engine (Offline-First, Auto-Sync & Pull)
        SettingsSectionCard(title = "Google Sheets Sync Engine", icon = Icons.Default.Sync) {
            Text(
                text = "100% Offline-First with Room DB. Automatically syncs sales, deliveries, and live inventory to your Google Sheet whenever you open the app, and lets you pull catalog data anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sheetUrlInput,
                onValueChange = {
                    sheetUrlInput = it
                    viewModel.updateGoogleSheetLink(it)
                },
                label = { Text("Google Apps Script Web App URL") },
                placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_sheet_url_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.openScriptDialog() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Script", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.pullFromGoogleSheet() },
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pull Data", fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.triggerSyncNow() },
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-sync runs automatically on app launch and every 15 min in background.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Database Maintenance & 6-Month Retention Info
        SettingsSectionCard(title = "Database & Receipt Retention", icon = Icons.Default.Warning) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📜 6-Month Receipt Retention Policy",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every transaction receipt is safely preserved in Room DB for 6 months for financial audit and customer support, and automatically purged after 6 months to keep your device lightweight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.seedDemoData() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Seed Demo", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.openResetDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_db_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset DB", fontSize = 12.sp)
                }
            }
        }
    }

    // Google Apps Script Modal Dialog
    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeScriptDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Apps Script Snippet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste this code in your Google Sheet (Extensions > Apps Script), then deploy as a Web App with access set to 'Anyone':",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = GoogleAppsScriptTemplate.SCRIPT_CODE,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Google Apps Script", GoogleAppsScriptTemplate.SCRIPT_CODE)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeScriptDialog() }) {
                    Text("Close")
                }
            }
        )
    }

    // Step 1 Reset Confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeResetDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Database?")
                }
            },
            text = {
                Text("This action will erase all sales history, delivery logs, products, and custom settings stored locally in Room DB.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.proceedToSecondConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Continue...")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeResetDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 2 Double Confirmation Alert
    if (showSecondResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.closeResetDialog() },
            title = {
                Text("⚠️ Final Warning", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you ABSOLUTELY sure? All offline data will be permanently wiped. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeDatabaseReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_wipe_button")
                ) {
                    Text("YES, WIPE ALL DATA")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeResetDialog() }) {
                    Text("Abort")
                }
            }
        )
    }

    // Support Me Dialog (Maya & GCash Donations)
    if (showSupportDialog) {
        SupportMeDialog(
            settings = settings,
            onDismiss = { viewModel.closeSupportDialog() },
            onUpdateMayaImage = { viewModel.updateMayaQrImage(it) },
            onUpdateGcashImage = { viewModel.updateGcashQrImage(it) }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
