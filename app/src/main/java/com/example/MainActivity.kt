package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CreditScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AstillaPosTheme
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.CreditViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SettingsViewModel

enum class PosTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    POS("POS", Icons.Default.ShoppingCart),
    INVENTORY("Inventory", Icons.Default.Inventory),
    UTANG("Utang", Icons.Default.AccountBalanceWallet),
    ANALYTICS("Analytics", Icons.Default.Assessment),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val posViewModel: PosViewModel by viewModels()
    private val inventoryViewModel: InventoryViewModel by viewModels()
    private val creditViewModel: CreditViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Automatically sync with Google Sheet every time the app is launched
        settingsViewModel.triggerAutoSyncOnStart()
        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            AstillaPosTheme(themeIndex = settings.themeColor) {
                var currentTab by remember { mutableStateOf(PosTab.POS) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            PosTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        PosTab.POS -> PosScreen(
                            viewModel = posViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        PosTab.INVENTORY -> InventoryScreen(
                            viewModel = inventoryViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        PosTab.UTANG -> CreditScreen(
                            viewModel = creditViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        PosTab.ANALYTICS -> AnalyticsScreen(
                            viewModel = analyticsViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        PosTab.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
