package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE shopCode = :shopCode ORDER BY dateMillis DESC")
    fun getAllTransactionsByShop(shopCode: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE shopCode = :shopCode AND customerId = :customerId ORDER BY dateMillis DESC")
    fun getTransactionsByCustomer(shopCode: String, customerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions WHERE shopCode = :shopCode AND status = 'PAID' AND paidDateMillis IS NOT NULL AND paidDateMillis < :cutoffTimestamp")
    suspend fun deleteOldPaidTransactions(shopCode: String, cutoffTimestamp: Long): Int

    @Query("DELETE FROM transactions WHERE shopCode = :shopCode")
    suspend fun deleteAllForShop(shopCode: String)
}
