package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date LIKE :yearMonthPrefix || '%' ORDER BY date DESC, createdAt DESC")
    fun getTransactionsForMonth(yearMonthPrefix: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date LIKE :yearMonthPrefix || '%' ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactionsForMonthOnce(yearMonthPrefix: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE paymentOccurrenceId = :occurrenceId LIMIT 1")
    suspend fun getTransactionByPaymentOccurrenceId(occurrenceId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE incomeOccurrenceId = :occurrenceId LIMIT 1")
    suspend fun getTransactionByIncomeOccurrenceId(occurrenceId: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(entity: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE paymentOccurrenceId = :occurrenceId")
    suspend fun deleteTransactionByPaymentOccurrenceId(occurrenceId: Long)

    @Query("DELETE FROM transactions WHERE incomeOccurrenceId = :occurrenceId")
    suspend fun deleteTransactionByIncomeOccurrenceId(occurrenceId: Long)
}
