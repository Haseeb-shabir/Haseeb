package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val shopCode: String,
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val type: String = "UDHAAR", // "UDHAAR" or "PAYMENT"
    val description: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "PARTIAL", "PAID"
    val paidAmount: Double = 0.0,
    val paidDateMillis: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = if (type == "UDHAAR") (amount - paidAmount).coerceAtLeast(0.0) else 0.0

    val isFullyPaid: Boolean
        get() = status == "PAID" || (type == "UDHAAR" && remainingAmount <= 0.0)
}
