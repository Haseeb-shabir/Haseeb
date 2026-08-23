package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ShopConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopConfigDao {
    @Query("SELECT * FROM shop_config WHERE isActive = 1 LIMIT 1")
    fun getActiveShop(): Flow<ShopConfigEntity?>

    @Query("SELECT * FROM shop_config WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveShopSync(): ShopConfigEntity?

    @Query("SELECT * FROM shop_config WHERE shopCode = :shopCode LIMIT 1")
    suspend fun getShopByCode(shopCode: String): ShopConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopConfigEntity)

    @Update
    suspend fun updateShop(shop: ShopConfigEntity)

    @Query("UPDATE shop_config SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE shop_config SET isAutoCleanupEnabled = :enabled WHERE shopCode = :shopCode")
    suspend fun setAutoCleanupEnabled(shopCode: String, enabled: Boolean)

    @Query("UPDATE shop_config SET lastSyncedAt = :timestamp WHERE shopCode = :shopCode")
    suspend fun updateLastSynced(shopCode: String, timestamp: Long)

    @Query("DELETE FROM shop_config")
    suspend fun clearAll()
}
