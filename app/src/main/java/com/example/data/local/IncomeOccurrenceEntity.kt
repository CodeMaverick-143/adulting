package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "income_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = IncomeSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["incomeSourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("incomeSourceId"),
        Index(value = ["incomeSourceId", "occurrenceDate"], unique = true),
        Index("transactionId")
    ]
)
data class IncomeOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incomeSourceId: Long,
    val occurrenceDate: String, // "YYYY-MM-DD"
    val expectedAmount: Double,
    val receivedAmount: Double? = null,
    val status: String, // IncomeStatus.name (EXPECTED, RECEIVED, SKIPPED)
    val receivedDate: String? = null,
    val transactionId: Long? = null,
    val notes: String = ""
)
