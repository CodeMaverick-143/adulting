package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_sources")
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val category: String, // IncomeCategory.name
    val frequency: String, // RecurrenceFrequency.name
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String? = null, // "YYYY-MM-DD"
    val expectedPaymentDay: Int = 1, // 1..31
    val notes: String = "",
    val status: String = "ACTIVE", // ScheduleStatus.name
    val effectiveAmountChangeDate: String? = null,
    val newAmount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
