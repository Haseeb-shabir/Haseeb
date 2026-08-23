package com.example.data.remote

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudShopData(
    val shopCode: String,
    val shopName: String,
    val customers: List<CloudCustomer> = emptyList(),
    val transactions: List<CloudTransaction> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class CloudCustomer(
    val id: String,
    val shopCode: String,
    val name: String,
    val phone: String = "",
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CloudTransaction(
    val id: String,
    val shopCode: String,
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val type: String = "UDHAAR",
    val description: String = "",
    val dateMillis: Long = 0L,
    val status: String = "PENDING",
    val paidAmount: Double = 0.0,
    val paidDateMillis: Long? = null,
    val lastUpdated: Long = 0L
)

fun CustomerEntity.toCloud(): CloudCustomer = CloudCustomer(
    id = id,
    shopCode = shopCode,
    name = name,
    phone = phone,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

fun CloudCustomer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    shopCode = shopCode,
    name = name,
    phone = phone,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

fun TransactionEntity.toCloud(): CloudTransaction = CloudTransaction(
    id = id,
    shopCode = shopCode,
    customerId = customerId,
    customerName = customerName,
    amount = amount,
    type = type,
    description = description,
    dateMillis = dateMillis,
    status = status,
    paidAmount = paidAmount,
    paidDateMillis = paidDateMillis,
    lastUpdated = lastUpdated
)

fun CloudTransaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    shopCode = shopCode,
    customerId = customerId,
    customerName = customerName,
    amount = amount,
    type = type,
    description = description,
    dateMillis = dateMillis,
    status = status,
    paidAmount = paidAmount,
    paidDateMillis = paidDateMillis,
    lastUpdated = lastUpdated
)
