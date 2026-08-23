package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE shopCode = :shopCode ORDER BY name ASC")
    fun getCustomersByShop(shopCode: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE shopCode = :shopCode AND (LOWER(name) = LOWER(:name) OR (phone != '' AND phone = :phone)) LIMIT 1")
    suspend fun findExistingCustomer(shopCode: String, name: String, phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: String)

    @Query("DELETE FROM customers WHERE shopCode = :shopCode")
    suspend fun deleteAllForShop(shopCode: String)
}
