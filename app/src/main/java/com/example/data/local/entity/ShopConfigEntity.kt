package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_config")
data class ShopConfigEntity(
    @PrimaryKey val shopCode: String,
    val shopName: String,
    val isAutoCleanupEnabled: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val isActive: Boolean = true
)
