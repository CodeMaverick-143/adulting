package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringPaymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recurringPaymentId"),
        Index(value = ["recurringPaymentId", "dueDate"], unique = true),
        Index("transactionId")
    ]
)
data class PaymentOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringPaymentId: Long,
    val dueDate: String, // "YYYY-MM-DD"
    val amount: Double,
    val status: String, // PaymentStatus.name (UNPAID, PAID, OVERDUE, SKIPPED)
    val paidDate: String? = null,
    val transactionId: Long? = null,
    val notes: String = ""
)
