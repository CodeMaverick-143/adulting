package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE recurringPaymentId = :paymentId")
    fun getRemindersForPayment(paymentId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE recurringPaymentId = :paymentId")
    suspend fun getRemindersForPaymentOnce(paymentId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabledReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(entity: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(entities: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(entity: ReminderEntity)

    @Delete
    suspend fun deleteReminder(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE recurringPaymentId = :paymentId")
    suspend fun deleteRemindersForPayment(paymentId: Long)
}
