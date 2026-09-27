package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Query("SELECT * FROM recurring_payments ORDER BY createdAt DESC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE status = 'ACTIVE' ORDER BY dueDay ASC")
    fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    fun getRecurringPaymentById(id: Long): Flow<RecurringPaymentEntity?>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    suspend fun getRecurringPaymentByIdOnce(id: Long): RecurringPaymentEntity?

    @Query("SELECT * FROM recurring_payments")
    suspend fun getAllRecurringPaymentsOnce(): List<RecurringPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayment(entity: RecurringPaymentEntity): Long

    @Update
    suspend fun updateRecurringPayment(entity: RecurringPaymentEntity)

    @Delete
    suspend fun deleteRecurringPayment(entity: RecurringPaymentEntity)

    @Query("UPDATE recurring_payments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE recurring_payments SET effectiveAmountChangeDate = :effectiveDate, newAmount = :newAmount WHERE id = :id")
    suspend fun updateEffectiveAmount(id: Long, effectiveDate: String, newAmount: Double)
}
