package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {

    // --- Payment Occurrences ---

    @Query("SELECT * FROM payment_occurrences WHERE dueDate LIKE :yearMonthPrefix || '%' ORDER BY dueDate ASC")
    fun getPaymentOccurrencesForMonth(yearMonthPrefix: String): Flow<List<PaymentOccurrenceEntity>>

    @Query("SELECT * FROM payment_occurrences WHERE dueDate LIKE :yearMonthPrefix || '%'")
    suspend fun getPaymentOccurrencesForMonthOnce(yearMonthPrefix: String): List<PaymentOccurrenceEntity>

    @Query("SELECT * FROM payment_occurrences WHERE recurringPaymentId = :paymentId AND dueDate = :dueDate LIMIT 1")
    suspend fun getPaymentOccurrence(paymentId: Long, dueDate: String): PaymentOccurrenceEntity?

    @Query("SELECT * FROM payment_occurrences WHERE id = :id LIMIT 1")
    suspend fun getPaymentOccurrenceById(id: Long): PaymentOccurrenceEntity?

    @Query("SELECT * FROM payment_occurrences WHERE recurringPaymentId = :paymentId ORDER BY dueDate DESC")
    fun getPaymentHistory(paymentId: Long): Flow<List<PaymentOccurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentOccurrence(entity: PaymentOccurrenceEntity): Long

    @Update
    suspend fun updatePaymentOccurrence(entity: PaymentOccurrenceEntity)

    @Delete
    suspend fun deletePaymentOccurrence(entity: PaymentOccurrenceEntity)

    @Query("DELETE FROM payment_occurrences WHERE id = :id")
    suspend fun deletePaymentOccurrenceById(id: Long)

    @Query("SELECT * FROM payment_occurrences WHERE status = 'OVERDUE' OR (status = 'UNPAID' AND dueDate < :today)")
    suspend fun getAllOverduePaymentOccurrences(today: String): List<PaymentOccurrenceEntity>


    // --- Income Occurrences ---

    @Query("SELECT * FROM income_occurrences WHERE occurrenceDate LIKE :yearMonthPrefix || '%' ORDER BY occurrenceDate ASC")
    fun getIncomeOccurrencesForMonth(yearMonthPrefix: String): Flow<List<IncomeOccurrenceEntity>>

    @Query("SELECT * FROM income_occurrences WHERE occurrenceDate LIKE :yearMonthPrefix || '%'")
    suspend fun getIncomeOccurrencesForMonthOnce(yearMonthPrefix: String): List<IncomeOccurrenceEntity>

    @Query("SELECT * FROM income_occurrences WHERE incomeSourceId = :incomeId AND occurrenceDate = :occurrenceDate LIMIT 1")
    suspend fun getIncomeOccurrence(incomeId: Long, occurrenceDate: String): IncomeOccurrenceEntity?

    @Query("SELECT * FROM income_occurrences WHERE id = :id LIMIT 1")
    suspend fun getIncomeOccurrenceById(id: Long): IncomeOccurrenceEntity?

    @Query("SELECT * FROM income_occurrences WHERE incomeSourceId = :incomeId ORDER BY occurrenceDate DESC")
    fun getIncomeHistory(incomeId: Long): Flow<List<IncomeOccurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeOccurrence(entity: IncomeOccurrenceEntity): Long

    @Update
    suspend fun updateIncomeOccurrence(entity: IncomeOccurrenceEntity)

    @Delete
    suspend fun deleteIncomeOccurrence(entity: IncomeOccurrenceEntity)
}
