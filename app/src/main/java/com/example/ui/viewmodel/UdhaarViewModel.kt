package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ShopConfigEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.SyncResult
import com.example.data.repository.UdhaarRepository
import com.example.ui.model.CustomerFilter
import com.example.ui.model.CustomerSort
import com.example.ui.model.CustomerWithBalance
import com.example.ui.model.DashboardStats
import com.example.ui.model.SyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UdhaarUiState(
    val activeShop: ShopConfigEntity? = null,
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val syncMessage: String = "Ready",
    val searchQuery: String = "",
    val filter: CustomerFilter = CustomerFilter.ALL,
    val sort: CustomerSort = CustomerSort.HIGHEST_PENDING,
    val stats: DashboardStats = DashboardStats(),
    val customers: List<CustomerWithBalance> = emptyList(),
    val filteredCustomers: List<CustomerWithBalance> = emptyList(),
    val historyTransactions: List<TransactionEntity> = emptyList(),
    val userMessage: String? = null,
    val selectedCustomerId: String? = null
)

class UdhaarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UdhaarRepository
    private val _uiState = MutableStateFlow(UdhaarUiState())
    val uiState: StateFlow<UdhaarUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null
    private var activeShopCode: String? = null

    init {
        val db = AppDatabase.getInstance(application)
        repository = UdhaarRepository(db)
        observeActiveShop()
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            repository.getActiveShop().collect { shop ->
                _uiState.update { it.copy(activeShop = shop, isInitialized = true) }
                if (shop != null) {
                    if (activeShopCode != shop.shopCode) {
                        activeShopCode = shop.shopCode
                        startObservingShopData(shop.shopCode)
                        startPeriodicSync(shop.shopCode)
                    }
                } else {
                    activeShopCode = null
                    syncJob?.cancel()
                }
            }
        }
    }

    private fun startObservingShopData(shopCode: String) {
        viewModelScope.launch {
            combine(
                repository.getCustomers(shopCode),
                repository.getAllTransactions(shopCode),
                _uiState
            ) { customerEntities, transactionEntities, state ->
                // Compute balances per customer
                val customerMap = customerEntities.associateBy { it.id }
                val txByCustomer = transactionEntities.groupBy { it.customerId }

                val calculatedCustomers = customerEntities.map { customer ->
                    val txs = txByCustomer[customer.id] ?: emptyList()
                    val totalUdhaar = txs.filter { it.type == "UDHAAR" }.sumOf { it.amount }
                    val totalPaid = txs.sumOf { tx ->
                        if (tx.type == "PAYMENT") tx.amount else tx.paidAmount
                    }
                    val remainingPending = txs.filter { it.type == "UDHAAR" && it.status != "PAID" }
                        .sumOf { it.remainingAmount }

                    val latestTx = txs.maxByOrNull { it.dateMillis }

                    CustomerWithBalance(
                        customer = customer,
                        totalUdhaar = totalUdhaar,
                        totalPaid = totalPaid,
                        remainingPending = remainingPending,
                        lastTransactionDate = latestTx?.dateMillis ?: customer.createdAt,
                        lastTransactionDescription = latestTx?.description?.ifBlank { "Udhaar entry" } ?: "New account",
                        transactionCount = txs.size
                    )
                }

                // Compute Dashboard Stats Live
                val totalPendingUdhaar = transactionEntities
                    .filter { it.type == "UDHAAR" && it.status != "PAID" }
                    .sumOf { it.remainingAmount }

                val totalPaidAmount = transactionEntities.sumOf { tx ->
                    if (tx.type == "PAYMENT") tx.amount else tx.paidAmount
                }

                val totalUdhaarGiven = transactionEntities
                    .filter { it.type == "UDHAAR" }
                    .sumOf { it.amount }

                val customersWithPending = calculatedCustomers.count { it.remainingPending > 0.0 }
                val settledCount = calculatedCustomers.count { it.isFullySettled && it.totalUdhaar > 0 }

                val recentPending = transactionEntities
                    .filter { it.type == "UDHAAR" && it.status != "PAID" && it.remainingAmount > 0.0 }
                    .sortedByDescending { it.dateMillis }
                    .take(8)

                val stats = DashboardStats(
                    totalPendingUdhaar = totalPendingUdhaar,
                    totalCustomers = calculatedCustomers.size,
                    totalPaidAmount = totalPaidAmount,
                    totalPendingAmount = totalPendingUdhaar,
                    customersWithPendingCount = customersWithPending,
                    settledCustomersCount = settledCount,
                    recentPendingTransactions = recentPending
                )

                // Filter & Sort
                val filtered = applyFilterAndSort(
                    customers = calculatedCustomers,
                    query = state.searchQuery,
                    filter = state.filter,
                    sort = state.sort
                )

                // History transactions: All paid udhaar + payments
                val history = transactionEntities
                    .filter { it.status == "PAID" || it.type == "PAYMENT" }
                    .sortedByDescending { it.paidDateMillis ?: it.dateMillis }

                state.copy(
                    stats = stats,
                    customers = calculatedCustomers,
                    filteredCustomers = filtered,
                    historyTransactions = history
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun applyFilterAndSort(
        customers: List<CustomerWithBalance>,
        query: String,
        filter: CustomerFilter,
        sort: CustomerSort
    ): List<CustomerWithBalance> {
        val q = query.trim().lowercase()

        // 1. Search Query
        val searched = if (q.isEmpty()) {
            customers
        } else {
            customers.filter {
                it.customer.name.lowercase().contains(q) ||
                it.customer.phone.contains(q)
            }
        }

        // 2. Filter
        val filtered = when (filter) {
            CustomerFilter.ALL -> searched
            CustomerFilter.PENDING -> searched.filter { it.remainingPending > 0.0 }
            CustomerFilter.PAID -> searched.filter { it.isFullySettled }
        }

        // 3. Sort
        return when (sort) {
            CustomerSort.HIGHEST_PENDING -> filtered.sortedByDescending { it.remainingPending }
            CustomerSort.LOWEST_PENDING -> filtered.sortedBy { it.remainingPending }
            CustomerSort.NEWEST -> filtered.sortedByDescending { it.customer.createdAt }
            CustomerSort.OLDEST -> filtered.sortedBy { it.customer.createdAt }
            CustomerSort.NAME_AZ -> filtered.sortedBy { it.customer.name.lowercase() }
        }
    }

    private fun startPeriodicSync(shopCode: String) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            // Initial sync immediately
            triggerSync(shopCode, isBackground = true)

            // Periodic sync loop every 5-6 seconds for real-time multi-device collaboration
            while (isActive) {
                delay(6000)
                triggerSync(shopCode, isBackground = true)
            }
        }
    }

    fun triggerSync(shopCode: String? = null, isBackground: Boolean = false) {
        val code = shopCode ?: _uiState.value.activeShop?.shopCode ?: return
        viewModelScope.launch {
            if (!isBackground) {
                _uiState.update { it.copy(syncState = SyncState.SYNCING, syncMessage = "Syncing with team...") }
            }
            when (val result = repository.performSync(code)) {
                is SyncResult.Success -> {
                    _uiState.update {
                        it.copy(
                            syncState = SyncState.SYNCED,
                            syncMessage = result.message
                        )
                    }
                }
                is SyncResult.Offline -> {
                    _uiState.update {
                        it.copy(
                            syncState = SyncState.OFFLINE,
                            syncMessage = result.message
                        )
                    }
                }
                is SyncResult.Error -> {
                    _uiState.update {
                        it.copy(
                            syncState = SyncState.OFFLINE,
                            syncMessage = result.message
                        )
                    }
                }
            }
        }
    }

    // Auth actions
    fun startNewShop(shopName: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val shop = repository.startNewShop(shopName)
            _uiState.update { it.copy(isLoading = false, activeShop = shop, userMessage = "Shop created! Code: ${shop.shopCode}") }
            onComplete()
        }
    }

    fun joinShop(shopCode: String, onComplete: (Boolean) -> Unit) {
        if (shopCode.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val shop = repository.joinExistingShop(shopCode)
                _uiState.update { it.copy(isLoading = false, activeShop = shop, userMessage = "Joined Shop ${shop.shopCode}") }
                triggerSync(shop.shopCode, isBackground = false)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Failed to join shop: ${e.message}") }
                onComplete(false)
            }
        }
    }

    fun logoutShop() {
        viewModelScope.launch {
            repository.logoutShop()
            _uiState.update {
                it.copy(
                    activeShop = null,
                    customers = emptyList(),
                    filteredCustomers = emptyList(),
                    historyTransactions = emptyList(),
                    stats = DashboardStats(),
                    userMessage = "Logged out of shop"
                )
            }
        }
    }

    // Add Customer or Udhaar Entry
    fun addUdhaar(
        customerName: String,
        phone: String,
        amount: Double,
        description: String,
        dateMillis: Long,
        isPaid: Boolean,
        onSuccess: (String) -> Unit
    ) {
        val shopCode = _uiState.value.activeShop?.shopCode ?: return
        if (customerName.isBlank()) {
            _uiState.update { it.copy(userMessage = "Customer name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val customerId = repository.addCustomerOrUdhaar(
                shopCode = shopCode,
                customerName = customerName,
                phone = phone,
                amount = amount,
                description = description,
                dateMillis = dateMillis,
                isPaid = isPaid
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = if (isPaid) "Paid entry saved successfully" else "Udhaar of Rs. ${amount.toLong()} recorded!"
                )
            }
            onSuccess(customerId)
        }
    }

    // Mark specific transaction as paid
    fun markTransactionAsPaid(transactionId: String) {
        viewModelScope.launch {
            val success = repository.markTransactionAsPaid(transactionId)
            if (success) {
                _uiState.update { it.copy(userMessage = "Marked as Paid!") }
            }
        }
    }

    // Record Partial payment against specific transaction
    fun recordPartialPayment(transactionId: String, amount: Double, note: String = "") {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val success = repository.recordPartialPayment(transactionId, amount, note)
            if (success) {
                _uiState.update { it.copy(userMessage = "Partial payment of Rs. ${amount.toLong()} recorded") }
            }
        }
    }

    // Record general payment for customer
    fun recordGeneralPayment(customerId: String, amount: Double, note: String = "") {
        val shopCode = _uiState.value.activeShop?.shopCode ?: return
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.recordGeneralCustomerPayment(
                shopCode = shopCode,
                customerId = customerId,
                paymentAmount = amount,
                note = note
            )
            _uiState.update { it.copy(userMessage = "Payment of Rs. ${amount.toLong()} received") }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = applyFilterAndSort(state.customers, query, state.filter, state.sort)
            state.copy(searchQuery = query, filteredCustomers = updated)
        }
    }

    fun setFilter(filter: CustomerFilter) {
        _uiState.update { state ->
            val updated = applyFilterAndSort(state.customers, state.searchQuery, filter, state.sort)
            state.copy(filter = filter, filteredCustomers = updated)
        }
    }

    fun setSort(sort: CustomerSort) {
        _uiState.update { state ->
            val updated = applyFilterAndSort(state.customers, state.searchQuery, state.filter, sort)
            state.copy(sort = sort, filteredCustomers = updated)
        }
    }

    fun setAutoCleanup(enabled: Boolean) {
        val shopCode = _uiState.value.activeShop?.shopCode ?: return
        viewModelScope.launch {
            repository.setAutoCleanup(shopCode, enabled)
            _uiState.update { state ->
                val currentShop = state.activeShop?.copy(isAutoCleanupEnabled = enabled)
                state.copy(
                    activeShop = currentShop,
                    userMessage = if (enabled) "2-month history cleanup enabled" else "History cleanup disabled"
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun getCustomerTransactionsFlow(customerId: String) =
        repository.getCustomerTransactions(_uiState.value.activeShop?.shopCode ?: "", customerId)
}
