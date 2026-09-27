package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("paymentOccurrenceId"),
        Index("incomeOccurrenceId"),
        Index("recurringPaymentId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // TransactionType.name (INCOME, EXPENSE)
    val category: String,
    val date: String, // "YYYY-MM-DD"
    val paymentMethod: String = "Bank Account",
    val notes: String = "",
    val recurringPaymentId: Long? = null,
    val paymentOccurrenceId: Long? = null,
    val incomeOccurrenceId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
