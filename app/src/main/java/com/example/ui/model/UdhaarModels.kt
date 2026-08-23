package com.example.ui.model

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ShopConfigEntity
import com.example.data.local.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalUdhaar: Double,
    val totalPaid: Double,
    val remainingPending: Double,
    val lastTransactionDate: Long,
    val lastTransactionDescription: String,
    val transactionCount: Int
) {
    val isFullySettled: Boolean get() = remainingPending <= 0.0
}

data class DashboardStats(
    val totalPendingUdhaar: Double = 0.0,
    val totalCustomers: Int = 0,
    val totalPaidAmount: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val customersWithPendingCount: Int = 0,
    val settledCustomersCount: Int = 0,
    val recentPendingTransactions: List<TransactionEntity> = emptyList()
)

enum class CustomerFilter {
    ALL, PENDING, PAID
}

enum class CustomerSort(val label: String) {
    HIGHEST_PENDING("Highest Udhaar"),
    LOWEST_PENDING("Lowest Udhaar"),
    NEWEST("Newest Added"),
    OLDEST("Oldest Added"),
    NAME_AZ("Name (A to Z)")
}

enum class SyncState {
    SYNCED, SYNCING, OFFLINE, IDLE
}

object UdhaarFormatter {
    private val pkrFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatPKR(amount: Double): String {
        val formatted = pkrFormat.format(amount.toLong())
        return "Rs. $formatted"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days == 1L -> "Yesterday"
            days < 30 -> "$days days ago"
            else -> formatDate(timestamp)
        }
    }
}
