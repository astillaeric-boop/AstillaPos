package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY currentBalance DESC, name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR messengerContact LIKE '%' || :query || '%' ORDER BY currentBalance DESC, name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>): List<Long>

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET currentBalance = :newBalance, lastUpdated = :timestamp WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: Long, newBalance: Double, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransaction(transaction: CreditTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransactions(transactions: List<CreditTransactionEntity>): List<Long>

    @Query("SELECT * FROM credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions ORDER BY timestamp DESC")
    fun getAllCreditTransactions(): Flow<List<CreditTransactionEntity>>

    @Query("DELETE FROM credit_transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("DELETE FROM credit_transactions")
    suspend fun deleteAllCreditTransactions()

    @Query("SELECT SUM(currentBalance) FROM customers")
    fun getTotalOutstandingCredit(): Flow<Double?>
}
