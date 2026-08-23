package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ShopConfigEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.CloudCustomer
import com.example.data.remote.CloudShopData
import com.example.data.remote.CloudTransaction
import com.example.data.remote.ShopSyncClient
import com.example.data.remote.toCloud
import com.example.data.remote.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

class UdhaarRepository(
    private val database: AppDatabase,
    private val syncClient: ShopSyncClient = ShopSyncClient()
) {
    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val shopConfigDao = database.shopConfigDao()

    fun getActiveShop(): Flow<ShopConfigEntity?> = shopConfigDao.getActiveShop()

    fun getCustomers(shopCode: String): Flow<List<CustomerEntity>> =
        customerDao.getCustomersByShop(shopCode)

    fun getAllTransactions(shopCode: String): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsByShop(shopCode)

    fun getCustomerTransactions(shopCode: String, customerId: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCustomer(shopCode, customerId)

    suspend fun getCustomerById(customerId: String): CustomerEntity? =
        customerDao.getCustomerById(customerId)

    suspend fun getTransactionById(transactionId: String): TransactionEntity? =
        transactionDao.getTransactionById(transactionId)

    // Generate a clean, 6-character Shop Code e.g. "PK7824" or "KH8931"
    private fun generateShopCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val prefix = "PK"
        val digits = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return "$prefix$digits"
    }

    suspend fun startNewShop(shopName: String): ShopConfigEntity = withContext(Dispatchers.IO) {
        val code = generateShopCode()
        val effectiveName = if (shopName.isBlank()) "My Shop" else shopName.trim()
        val config = ShopConfigEntity(
            shopCode = code,
            shopName = effectiveName,
            isAutoCleanupEnabled = true,
            lastSyncedAt = System.currentTimeMillis(),
            isActive = true
        )
        shopConfigDao.deactivateAll()
        shopConfigDao.insertShop(config)

        // Initialize empty cloud payload for this shop code
        val cloudData = CloudShopData(
            shopCode = code,
            shopName = effectiveName,
            customers = emptyList(),
            transactions = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        syncClient.pushCloudData(cloudData)

        config
    }

    suspend fun joinExistingShop(enteredCode: String): ShopConfigEntity = withContext(Dispatchers.IO) {
        val cleanCode = enteredCode.trim().uppercase()
        shopConfigDao.deactivateAll()

        // Check if we already have it locally
        val existingLocal = shopConfigDao.getShopByCode(cleanCode)
        val cloudData = syncClient.fetchCloudData(cleanCode)

        val shopName = cloudData?.shopName
            ?: existingLocal?.shopName
            ?: "Shop $cleanCode"

        val config = ShopConfigEntity(
            shopCode = cleanCode,
            shopName = shopName,
            isAutoCleanupEnabled = existingLocal?.isAutoCleanupEnabled ?: true,
            lastSyncedAt = System.currentTimeMillis(),
            isActive = true
        )
        shopConfigDao.insertShop(config)

        // Merge cloud data if available
        if (cloudData != null) {
            val entitiesCustomers = cloudData.customers.map { it.toEntity() }
            val entitiesTransactions = cloudData.transactions.map { it.toEntity() }
            customerDao.insertCustomers(entitiesCustomers)
            transactionDao.insertTransactions(entitiesTransactions)
        }

        config
    }

    suspend fun addCustomerOrUdhaar(
        shopCode: String,
        customerName: String,
        phone: String,
        amount: Double,
        description: String,
        dateMillis: Long,
        isPaid: Boolean
    ): String = withContext(Dispatchers.IO) {
        val trimmedName = customerName.trim()
        val trimmedPhone = phone.trim()

        // Check if customer already exists by name or phone
        var customer = customerDao.findExistingCustomer(shopCode, trimmedName, trimmedPhone)
        if (customer == null) {
            val newCustomerId = UUID.randomUUID().toString()
            customer = CustomerEntity(
                id = newCustomerId,
                shopCode = shopCode,
                name = trimmedName,
                phone = trimmedPhone,
                createdAt = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )
            customerDao.insertCustomer(customer)
        } else if (trimmedPhone.isNotBlank() && customer.phone.isBlank()) {
            // Update phone if previously blank
            val updated = customer.copy(phone = trimmedPhone, lastUpdated = System.currentTimeMillis())
            customerDao.updateCustomer(updated)
            customer = updated
        }

        if (amount > 0.0) {
            val status = if (isPaid) "PAID" else "PENDING"
            val paidAmount = if (isPaid) amount else 0.0
            val paidDateMillis = if (isPaid) dateMillis else null

            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                shopCode = shopCode,
                customerId = customer.id,
                customerName = customer.name,
                amount = amount,
                type = "UDHAAR",
                description = description.trim(),
                dateMillis = dateMillis,
                status = status,
                paidAmount = paidAmount,
                paidDateMillis = paidDateMillis,
                lastUpdated = System.currentTimeMillis()
            )
            transactionDao.insertTransaction(transaction)
        }

        // Trigger sync in background
        triggerSyncPush(shopCode)

        customer.id
    }

    suspend fun markTransactionAsPaid(transactionId: String): Boolean = withContext(Dispatchers.IO) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return@withContext false
        val now = System.currentTimeMillis()
        val updated = transaction.copy(
            status = "PAID",
            paidAmount = transaction.amount,
            paidDateMillis = now,
            lastUpdated = now
        )
        transactionDao.updateTransaction(updated)
        triggerSyncPush(transaction.shopCode)
        true
    }

    suspend fun recordPartialPayment(
        transactionId: String,
        paidNow: Double,
        note: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return@withContext false
        val now = System.currentTimeMillis()
        val newPaidAmount = (transaction.paidAmount + paidNow).coerceAtMost(transaction.amount)
        val isFullySettled = newPaidAmount >= transaction.amount
        val status = if (isFullySettled) "PAID" else "PARTIAL"
        val paidDate = if (isFullySettled) now else transaction.paidDateMillis

        val updatedDesc = if (note.isNotBlank()) {
            if (transaction.description.isNotBlank()) "${transaction.description} | Paid: $note" else "Paid: $note"
        } else {
            transaction.description
        }

        val updated = transaction.copy(
            status = status,
            paidAmount = newPaidAmount,
            paidDateMillis = paidDate,
            description = updatedDesc,
            lastUpdated = now
        )
        transactionDao.updateTransaction(updated)
        triggerSyncPush(transaction.shopCode)
        true
    }

    suspend fun recordGeneralCustomerPayment(
        shopCode: String,
        customerId: String,
        paymentAmount: Double,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ): Double = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId) ?: return@withContext 0.0
        // Get all pending/partial transactions for this customer ordered oldest first
        val transactions = transactionDao.getTransactionsByCustomer(shopCode, customerId).firstOrNull() ?: emptyList()
        val pendingList = transactions.filter { it.type == "UDHAAR" && it.status != "PAID" && it.remainingAmount > 0.0 }
            .sortedBy { it.dateMillis }

        var remainingPaymentToApply = paymentAmount
        val now = System.currentTimeMillis()

        for (tx in pendingList) {
            if (remainingPaymentToApply <= 0.0) break
            val oweOnThis = tx.remainingAmount
            if (remainingPaymentToApply >= oweOnThis) {
                // Fully pays this transaction
                val updated = tx.copy(
                    status = "PAID",
                    paidAmount = tx.amount,
                    paidDateMillis = dateMillis,
                    lastUpdated = now
                )
                transactionDao.updateTransaction(updated)
                remainingPaymentToApply -= oweOnThis
            } else {
                // Partially pays this transaction
                val updated = tx.copy(
                    status = "PARTIAL",
                    paidAmount = tx.paidAmount + remainingPaymentToApply,
                    lastUpdated = now
                )
                transactionDao.updateTransaction(updated)
                remainingPaymentToApply = 0.0
            }
        }

        // Also record a payment credit entry for complete ledger bookkeeping
        val paymentTx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            shopCode = shopCode,
            customerId = customerId,
            customerName = customer.name,
            amount = paymentAmount,
            type = "PAYMENT",
            description = if (note.isNotBlank()) "Payment received: $note" else "Cash payment received",
            dateMillis = dateMillis,
            status = "PAID",
            paidAmount = paymentAmount,
            paidDateMillis = dateMillis,
            lastUpdated = now
        )
        transactionDao.insertTransaction(paymentTx)

        triggerSyncPush(shopCode)
        paymentAmount
    }

    suspend fun performSync(shopCode: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localCustomers = customerDao.getCustomersByShop(shopCode).firstOrNull() ?: emptyList()
            val localTransactions = transactionDao.getAllTransactionsByShop(shopCode).firstOrNull() ?: emptyList()
            val activeShop = shopConfigDao.getShopByCode(shopCode)

            val cloudData = syncClient.fetchCloudData(shopCode)

            if (cloudData == null) {
                // If cloud has nothing yet, push local
                val pushPayload = CloudShopData(
                    shopCode = shopCode,
                    shopName = activeShop?.shopName ?: "Shop $shopCode",
                    customers = localCustomers.map { it.toCloud() },
                    transactions = localTransactions.map { it.toCloud() },
                    lastUpdated = System.currentTimeMillis()
                )
                val success = syncClient.pushCloudData(pushPayload)
                if (success) {
                    shopConfigDao.updateLastSynced(shopCode, System.currentTimeMillis())
                    return@withContext SyncResult.Success("Synced with cloud")
                } else {
                    return@withContext SyncResult.Offline("Offline: changes saved locally")
                }
            }

            // Merge customers: cloud + local by ID taking highest lastUpdated
            val mergedCustomersMap = mutableMapOf<String, CustomerEntity>()
            for (c in localCustomers) {
                mergedCustomersMap[c.id] = c
            }
            for (cc in cloudData.customers) {
                val entity = cc.toEntity()
                val existing = mergedCustomersMap[entity.id]
                if (existing == null || entity.lastUpdated >= existing.lastUpdated) {
                    mergedCustomersMap[entity.id] = entity
                }
            }

            // Merge transactions: cloud + local by ID taking highest lastUpdated
            val mergedTransactionsMap = mutableMapOf<String, TransactionEntity>()
            for (t in localTransactions) {
                mergedTransactionsMap[t.id] = t
            }
            for (ct in cloudData.transactions) {
                val entity = ct.toEntity()
                val existing = mergedTransactionsMap[entity.id]
                if (existing == null || entity.lastUpdated >= existing.lastUpdated) {
                    mergedTransactionsMap[entity.id] = entity
                }
            }

            val finalCustomers = mergedCustomersMap.values.toList()
            val finalTransactions = mergedTransactionsMap.values.toList()

            // Save merged list into Room
            customerDao.insertCustomers(finalCustomers)
            transactionDao.insertTransactions(finalTransactions)

            // Push merged back to cloud to keep all devices updated
            val mergedPayload = CloudShopData(
                shopCode = shopCode,
                shopName = cloudData.shopName.ifBlank { activeShop?.shopName ?: "Shop $shopCode" },
                customers = finalCustomers.map { it.toCloud() },
                transactions = finalTransactions.map { it.toCloud() },
                lastUpdated = System.currentTimeMillis()
            )
            syncClient.pushCloudData(mergedPayload)

            val now = System.currentTimeMillis()
            shopConfigDao.updateLastSynced(shopCode, now)

            // Check auto-cleanup policy
            if (activeShop?.isAutoCleanupEnabled == true) {
                cleanupOldPaidHistory(shopCode)
            }

            SyncResult.Success("Synced successfully across all devices")
        } catch (e: Exception) {
            Log.e("UdhaarRepository", "Sync exception: ${e.message}")
            SyncResult.Offline("Offline mode: Data saved safely on device")
        }
    }

    private suspend fun triggerSyncPush(shopCode: String) {
        try {
            val localCustomers = customerDao.getCustomersByShop(shopCode).firstOrNull() ?: emptyList()
            val localTransactions = transactionDao.getAllTransactionsByShop(shopCode).firstOrNull() ?: emptyList()
            val activeShop = shopConfigDao.getShopByCode(shopCode)

            val payload = CloudShopData(
                shopCode = shopCode,
                shopName = activeShop?.shopName ?: "Shop $shopCode",
                customers = localCustomers.map { it.toCloud() },
                transactions = localTransactions.map { it.toCloud() },
                lastUpdated = System.currentTimeMillis()
            )
            val pushed = syncClient.pushCloudData(payload)
            if (pushed) {
                shopConfigDao.updateLastSynced(shopCode, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.w("UdhaarRepository", "Background push deferred: ${e.message}")
        }
    }

    // Auto cleanup paid transactions older than 60 days (2 months)
    suspend fun cleanupOldPaidHistory(shopCode: String): Int = withContext(Dispatchers.IO) {
        val sixtyDaysMillis = 60L * 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - sixtyDaysMillis
        val deletedCount = transactionDao.deleteOldPaidTransactions(shopCode, cutoff)
        if (deletedCount > 0) {
            triggerSyncPush(shopCode)
        }
        deletedCount
    }

    suspend fun setAutoCleanup(shopCode: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        shopConfigDao.setAutoCleanupEnabled(shopCode, enabled)
        if (enabled) {
            cleanupOldPaidHistory(shopCode)
        }
    }

    suspend fun logoutShop(): Unit = withContext(Dispatchers.IO) {
        shopConfigDao.deactivateAll()
    }
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Offline(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
