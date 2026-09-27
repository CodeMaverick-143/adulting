package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_payments")
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val isVariableAmount: Boolean = false,
    val category: String, // PaymentCategory.name
    val frequency: String, // RecurrenceFrequency.name
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String? = null, // "YYYY-MM-DD"
    val dueDay: Int = 1, // 1..31
    val isAutoRenewal: Boolean = true,
    val notes: String = "",
    val status: String = "ACTIVE", // ScheduleStatus.name (ACTIVE, PAUSED, COMPLETED)
    val effectiveAmountChangeDate: String? = null,
    val newAmount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
