package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.ui.FinanceViewModel
import com.example.ui.components.ConfirmationModal
import com.example.ui.components.ToastNotification
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val prefs = AppPreferences(applicationContext)
        
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return FinanceViewModel(database, prefs) as T
            }
        }
        val viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[FinanceViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()
                val activeScreen by viewModel.activeScreen.collectAsState()
                val toast by viewModel.toast.collectAsState()
                val confirmDeleteTx by viewModel.confirmDeleteTransaction.collectAsState()
                val confirmReset by viewModel.confirmReset.collectAsState()
                val editingTx by viewModel.editingTransaction.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                ) {
                    when (isSetupCompleted) {
                        null -> {
                            // Loading state
                            LoadingScreen()
                        }
                        false -> {
                            // First Setup
                            SetupScreen(viewModel = viewModel)
                        }
                        true -> {
                            // Main Application Flow (SPA-style)
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color(0xFF0F172A),
                                bottomBar = {
                                    FinanceBottomNavigation(
                                        activeScreen = activeScreen,
                                        onTabSelected = { screen ->
                                            viewModel.setScreen(screen)
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    // Primary screen content
                                    when (activeScreen) {
                                        "home" -> DashboardScreen(viewModel = viewModel)
                                        "add" -> TransactionFormScreen(viewModel = viewModel)
                                        "history" -> HistoryScreen(viewModel = viewModel)
                                        "settings" -> SettingsScreen(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }

                    // Dialog edits overlay
                    if (editingTx != null) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF0F172A)
                        ) {
                            TransactionFormScreen(viewModel = viewModel, editTx = editingTx)
                        }
                    }

                    // Global components
                    ToastNotification(toast = toast, onDismiss = { viewModel.dismissToast() })

                    // Custom confirm overlays
                    if (confirmDeleteTx != null) {
                        val tempTx = confirmDeleteTx!!
                        ConfirmationModal(
                            title = "Hapus transaksi?",
                            description = "Transaksi ini akan dihapus dan saldo akan dikembalikan sesuai efek transaksi tersebut.",
                            onConfirm = { viewModel.deleteTransactionConfirmed(tempTx) },
                            onCancel = { viewModel.dismissDeleteConfirmation() }
                        )
                    }

                    if (confirmReset) {
                        ConfirmationModal(
                            title = "Hapus semua data?",
                            description = "Semua data transaksi dan saldo akan dihapus permanen.",
                            confirmText = "Reset",
                            onConfirm = { viewModel.resetApplication() },
                            onCancel = { viewModel.dismissResetConfirmation() }
                        )
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val screen: String, val label: String, val icon: ImageVector)

@Composable
fun FinanceBottomNavigation(
    activeScreen: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("add", "Tambah", Icons.Default.Add),
        BottomNavItem("history", "Riwayat", Icons.Default.History),
        BottomNavItem("settings", "Setelan", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = Color(0xFF1E293B),
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEach { item ->
            val isSelected = activeScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF334155)
                )
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
