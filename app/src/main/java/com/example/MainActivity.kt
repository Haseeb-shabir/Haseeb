package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppBottomNav
import com.example.ui.screens.add_udhaar.AddUdhaarScreen
import com.example.ui.screens.auth.ShopLoginScreen
import com.example.ui.screens.customer_detail.CustomerDetailScreen
import com.example.ui.screens.customers.CustomersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UdhaarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: UdhaarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
                var initialAddName by rememberSaveable { mutableStateOf("") }
                var initialAddPhone by rememberSaveable { mutableStateOf("") }

                // User messages snackbar
                LaunchedEffect(uiState.userMessage) {
                    uiState.userMessage?.let { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearUserMessage()
                        }
                    }
                }

                if (!uiState.isInitialized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.activeShop == null) {
                    ShopLoginScreen(
                        isLoading = uiState.isLoading,
                        onStartNewShop = { name ->
                            viewModel.startNewShop(name) {
                                selectedTab = 0
                            }
                        },
                        onJoinExistingShop = { code ->
                            viewModel.joinShop(code) { success ->
                                if (success) {
                                    selectedTab = 0
                                }
                            }
                        },
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                } else {
                    val activeShop = uiState.activeShop!!

                    // If a customer detail screen is active
                    if (selectedCustomerId != null) {
                        val customerWithBalance = uiState.customers.find { it.customer.id == selectedCustomerId }
                        val customerTransactions by viewModel.getCustomerTransactionsFlow(selectedCustomerId!!)
                            .collectAsStateWithLifecycle(initialValue = emptyList())

                        BackHandler {
                            selectedCustomerId = null
                        }

                        if (customerWithBalance != null) {
                            CustomerDetailScreen(
                                customerWithBalance = customerWithBalance,
                                transactions = customerTransactions,
                                onBackClick = { selectedCustomerId = null },
                                onMarkAsPaid = { txId ->
                                    viewModel.markTransactionAsPaid(txId)
                                },
                                onRecordPartialPayment = { txId, amount, note ->
                                    viewModel.recordPartialPayment(txId, amount, note)
                                },
                                onRecordGeneralPayment = { custId, amount, note ->
                                    viewModel.recordGeneralPayment(custId, amount, note)
                                },
                                onAddUdhaarForCustomer = { name, phone ->
                                    initialAddName = name
                                    initialAddPhone = phone
                                    selectedCustomerId = null
                                    selectedTab = 2 // Switch to Add Udhaar tab
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        } else {
                            selectedCustomerId = null
                        }
                    } else {
                        // Main Bottom Tabbed Navigation
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                AppBottomNav(
                                    currentTab = selectedTab,
                                    onTabSelected = { tab ->
                                        if (tab != 2) {
                                            initialAddName = ""
                                            initialAddPhone = ""
                                        }
                                        selectedTab = tab
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) { innerPadding ->
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "TabNavigation",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) { tab ->
                                when (tab) {
                                    0 -> DashboardScreen(
                                        shop = activeShop,
                                        stats = uiState.stats,
                                        syncState = uiState.syncState,
                                        onSyncClick = { viewModel.triggerSync(isBackground = false) },
                                        onNavigateToAddUdhaar = {
                                            initialAddName = ""
                                            initialAddPhone = ""
                                            selectedTab = 2
                                        },
                                        onNavigateToCustomers = { selectedTab = 1 },
                                        onNavigateToCustomerDetail = { custId ->
                                            selectedCustomerId = custId
                                        },
                                        onMarkTransactionAsPaid = { txId ->
                                            viewModel.markTransactionAsPaid(txId)
                                        },
                                        onRecordPartialPayment = { txId, amount, note ->
                                            viewModel.recordPartialPayment(txId, amount, note)
                                        }
                                    )

                                    1 -> CustomersScreen(
                                        customers = uiState.filteredCustomers,
                                        searchQuery = uiState.searchQuery,
                                        filter = uiState.filter,
                                        sort = uiState.sort,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        onFilterChange = { viewModel.setFilter(it) },
                                        onSortChange = { viewModel.setSort(it) },
                                        onCustomerClick = { custId ->
                                            selectedCustomerId = custId
                                        },
                                        onAddCustomerClick = {
                                            initialAddName = ""
                                            initialAddPhone = ""
                                            selectedTab = 2
                                        }
                                    )

                                    2 -> AddUdhaarScreen(
                                        existingCustomers = uiState.customers,
                                        initialCustomerName = initialAddName,
                                        initialPhone = initialAddPhone,
                                        isLoading = uiState.isLoading,
                                        onSaveUdhaar = { name, phone, amount, desc, dateMillis, isPaid ->
                                            viewModel.addUdhaar(
                                                customerName = name,
                                                phone = phone,
                                                amount = amount,
                                                description = desc,
                                                dateMillis = dateMillis,
                                                isPaid = isPaid
                                            ) { custId ->
                                                initialAddName = ""
                                                initialAddPhone = ""
                                                selectedTab = 0 // Go to dashboard to see updated balance
                                            }
                                        }
                                    )

                                    3 -> HistoryScreen(
                                        historyTransactions = uiState.historyTransactions,
                                        isAutoCleanupEnabled = activeShop.isAutoCleanupEnabled,
                                        onCustomerClick = { custId ->
                                            selectedCustomerId = custId
                                        }
                                    )

                                    4 -> SettingsScreen(
                                        shop = activeShop,
                                        syncState = uiState.syncState,
                                        syncMessage = uiState.syncMessage,
                                        onSyncClick = { viewModel.triggerSync(isBackground = false) },
                                        onToggleAutoCleanup = { enabled ->
                                            viewModel.setAutoCleanup(enabled)
                                        },
                                        onLogoutClick = {
                                            viewModel.logoutShop()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
