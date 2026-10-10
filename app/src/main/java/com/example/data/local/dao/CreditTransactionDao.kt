package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CreditTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransaction(transaction: CreditTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransactions(transactions: List<CreditTransactionEntity>): List<Long>

    @Query("SELECT * FROM credit_transactions ORDER BY timestamp DESC")
    fun getAllCreditTransactions(): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenDirect(startDate: Long, endDate: Long): List<CreditTransactionEntity>

    /**
     * Sum of credit / utang payments received ("Bayad Utang") in the given timeframe.
     * Logged as CUSTOMER_PAYMENT or PAYMENT.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM credit_transactions WHERE (transactionType LIKE '%PAYMENT%') AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getPaymentsCollected(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM credit_transactions WHERE (transactionType LIKE '%PAYMENT%') AND timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun getPaymentsCollectedDirect(startDate: Long, endDate: Long): Double

    /**
     * Sum of credit / utang borrow transactions in the given timeframe.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM credit_transactions WHERE transactionType = 'BORROW' AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getBorrowAmountBetween(startDate: Long, endDate: Long): Flow<Double>

    /**
     * Real-time sum of all active customer balances (unpaid receivables).
     */
    @Query("SELECT COALESCE(SUM(currentBalance), 0.0) FROM customers")
    fun getOutstandingUtangBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(currentBalance), 0.0) FROM customers")
    suspend fun getOutstandingUtangBalanceDirect(): Double

    @Query("DELETE FROM credit_transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long)

    @Query("DELETE FROM credit_transactions")
    suspend fun deleteAllCreditTransactions()

    @Query("SELECT * FROM credit_transactions WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTransactions(): List<CreditTransactionEntity>

    @Query("UPDATE credit_transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markTransactionSynced(id: Long)

    @Query("UPDATE credit_transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<Long>)
}
