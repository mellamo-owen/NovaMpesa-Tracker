package com.novanest.mpesatracker.data.dao

import androidx.room.*
import com.novanest.mpesatracker.data.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isLoan = 1")
    fun getAllLoans(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp <= :to ORDER BY timestamp DESC")
    fun getBetween(from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND timestamp >= :from AND timestamp <= :to")
    fun getByCategoryBetween(categoryId: Long, from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :from AND timestamp <= :to")
    suspend fun totalBetween(from: Long, to: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND timestamp >= :from AND timestamp <= :to")
    suspend fun categoryTotalBetween(categoryId: Long, from: Long, to: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE amount < 200 AND timestamp >= :from AND timestamp <= :to")
    suspend fun leakageTotal(from: Long, to: Long): Double? // For Idea 6

    @Query("SELECT * FROM transactions WHERE txnCode = :code LIMIT 1")
    suspend fun findByTxnCode(code: String): Transaction?

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :txnId")
    suspend fun updateCategory(txnId: Long, categoryId: Long)

    @Query("UPDATE transactions SET isLoan = :isLoan WHERE id = :txnId")
    suspend fun updateLoanStatus(txnId: Long, isLoan: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: Transaction): Long

    @Update
    suspend fun update(txn: Transaction)

    @Delete
    suspend fun delete(txn: Transaction)
}
