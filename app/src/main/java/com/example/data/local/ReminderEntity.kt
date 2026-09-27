package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = RecurringPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringPaymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recurringPaymentId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringPaymentId: Long,
    val daysBefore: Int, // 0 = on due date, 1 = 1 day before, 3 = 3 days before, etc.
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isEnabled: Boolean = true
)
