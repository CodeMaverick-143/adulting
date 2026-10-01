package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_caps",
    indices = [Index(value = ["category"], unique = true)]
)
data class BudgetCapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // Matches PaymentCategory.name
    val monthlyLimit: Double,
    val alertThresholdPercent: Double = 80.0,
    val isActive: Boolean = true
)
