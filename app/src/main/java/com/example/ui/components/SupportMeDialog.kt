package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.AppSettingsEntity
import com.example.util.ImageStorageHelper
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MayaGreen = Color(0xFF00C261)
private val MayaDarkGreen = Color(0xFF059669)
private val GCashBlue = Color(0xFF005CEE)
private val GCashDarkBlue = Color(0xFF003CB3)

@Composable
fun SupportMeDialog(
    settings: AppSettingsEntity,
    onDismiss: () -> Unit,
    onUpdateMayaImage: (String?) -> Unit,
    onUpdateGcashImage: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Maya, 1 = GCash

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveImageFromUri(context, uri)
            if (savedPath != null) {
                if (selectedTab == 0) {
                    onUpdateMayaImage(savedPath)
                } else {
                    onUpdateGcashImage(savedPath)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Support the Developer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scan QR or copy details to donate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Payment Method Selector Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 14.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MayaGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("m", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Maya",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) MayaDarkGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(GCashBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GCash",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) GCashBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // MAYA DONATION CARD
                    MayaDonationCard(
                        settings = settings,
                        onCopy = { label, text ->
                            copyToClipboard(context, label, text)
                        },
                        onPickCustomImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onResetImage = { onUpdateMayaImage(null) },
                        onOpenApp = { openAppOrBrowser(context, "maya", "https://www.maya.ph") }
                    )
                } else {
                    // GCASH DONATION CARD
                    GCashDonationCard(
                        settings = settings,
                        onCopy = { label, text ->
                            copyToClipboard(context, label, text)
                        },
                        onPickCustomImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onResetImage = { onUpdateGcashImage(null) },
                        onOpenApp = { openAppOrBrowser(context, "gcash", "https://www.gcash.com") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thank you note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❤️ Big thanks for your support!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your donation fuels ongoing updates, offline enhancements, and keeps this POS tool running smoothly.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun MayaDonationCard(
    settings: AppSettingsEntity,
    onCopy: (String, String) -> Unit,
    onPickCustomImage: () -> Unit,
    onResetImage: () -> Unit,
    onOpenApp: () -> Unit
) {
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(settings.mayaAccountName, settings.mayaHandle, settings.mayaPhoneNumber) {
        withContext(Dispatchers.Default) {
            val payload = QrCodeGenerator.buildMayaQrPayload(
                settings.mayaAccountName,
                settings.mayaHandle,
                settings.mayaPhoneNumber
            )
            generatedBitmap = QrCodeGenerator.generateQrBitmap(payload, size = 640, showInstaPayBadge = true)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Maya Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MayaGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "m",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = settings.mayaAccountName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                letterSpacing = 0.5.sp
            )

            Text(
                text = settings.mayaHandle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )

            Text(
                text = settings.mayaPhoneNumber,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // QR Code Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(230.dp)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!settings.mayaQrImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(settings.mayaQrImagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Maya QR Code",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(210.dp)
                        )
                    } else if (generatedBitmap != null) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Maya InstaPay QR Code",
                            modifier = Modifier.size(210.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MayaGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transfer fees may apply pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Text(
                    text = "Transfer fees may apply",
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Brand
            Text(
                text = "maya",
                color = MayaGreen,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onCopy("Maya Username", settings.mayaHandle) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy @", fontSize = 11.sp)
                }

                Button(
                    onClick = onOpenApp,
                    colors = ButtonDefaults.buttonColors(containerColor = MayaGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Maya App", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onPickCustomImage) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Saved QR Photo", fontSize = 11.sp)
                }

                if (!settings.mayaQrImagePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onResetImage) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Reset", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}

@Composable
fun GCashDonationCard(
    settings: AppSettingsEntity,
    onCopy: (String, String) -> Unit,
    onPickCustomImage: () -> Unit,
    onResetImage: () -> Unit,
    onOpenApp: () -> Unit
) {
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(settings.gcashAccountName, settings.gcashPhoneNumber, settings.gcashUserId) {
        withContext(Dispatchers.Default) {
            val payload = QrCodeGenerator.buildGcashQrPayload(
                settings.gcashAccountName,
                settings.gcashPhoneNumber,
                settings.gcashUserId
            )
            generatedBitmap = QrCodeGenerator.generateQrBitmap(payload, size = 640, showInstaPayBadge = true)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GCashBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // GCash Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = GCashBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GCash",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // White Inner Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // QR Box
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!settings.gcashQrImagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(settings.gcashQrImagePath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "GCash QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(190.dp)
                            )
                        } else if (generatedBitmap != null) {
                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "GCash InstaPay QR Code",
                                modifier = Modifier.size(190.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = GCashBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Transfer fees may apply.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = settings.gcashAccountName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GCashBlue,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Mobile No.: ${settings.gcashPhoneNumber}",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "User ID: ${settings.gcashUserId}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onCopy("GCash Number", settings.gcashPhoneNumber) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy No.", fontSize = 11.sp)
                }

                Button(
                    onClick = onOpenApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GCashBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GCash App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onPickCustomImage,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Saved QR Photo", fontSize = 11.sp)
                }

                if (!settings.gcashQrImagePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onResetImage,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFCDD2))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun openAppOrBrowser(context: Context, appScheme: String, fallbackUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$appScheme://"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open $appScheme app", Toast.LENGTH_SHORT).show()
        }
    }
}
