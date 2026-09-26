package com.example.data.model

import com.example.domain.AppDate
import com.example.domain.YearMonth

data class IncomeOccurrenceItem(
    val id: Long, // 0 if projected/unsaved
    val incomeSourceId: Long,
    val name: String,
    val category: IncomeCategory,
    val frequency: RecurrenceFrequency,
    val occurrenceDate: AppDate,
    val expectedAmount: Double,
    val receivedAmount: Double?,
    val status: IncomeStatus,
    val isProjected: Boolean,
    val transactionId: Long? = null,
    val notes: String = ""
)

data class BillOccurrenceItem(
    val id: Long, // 0 if projected/unsaved
    val recurringPaymentId: Long,
    val name: String,
    val category: PaymentCategory,
    val frequency: RecurrenceFrequency,
    val dueDate: AppDate,
    val amount: Double,
    val isVariableAmount: Boolean,
    val status: PaymentStatus,
    val paidDate: AppDate? = null,
    val isProjected: Boolean,
    val transactionId: Long? = null,
    val isAutoRenewal: Boolean = true,
    val notes: String = ""
)

data class MonthlyFinancialSummary(
    val targetMonth: YearMonth,
    val expectedIncome: Double,
    val receivedIncome: Double,
    val remainingExpectedIncome: Double,
    val totalPlannedPayments: Double,
    val totalPaidPayments: Double,
    val remainingUnpaidPayments: Double,
    val overduePaymentsAmount: Double,
    val overdueCount: Int,
    val expectedRemainingBalance: Double,
    val incomes: List<IncomeOccurrenceItem>,
    val bills: List<BillOccurrenceItem>
)

data class MonthForecast(
    val targetMonth: YearMonth,
    val expectedIncome: Double,
    val scheduledPayments: Double,
    val projectedNet: Double,
    val cumulativeBalance: Double,
    val isNegative: Boolean,
    val nonMonthlyCommitments: List<String>
)

data class CategoryBudgetStatus(
    val id: Long = 0,
    val category: PaymentCategory,
    val monthlyLimit: Double,
    val alertThresholdPercent: Double = 80.0,
    val spentAmount: Double, // Actual paid in this month
    val plannedAmount: Double, // Total planned/committed in this month
    val remainingAllowance: Double, // monthlyLimit - spentAmount (clamped to min 0 or negative if exceeded)
    val percentageSpent: Double, // (spentAmount / monthlyLimit) * 100
    val isApproaching: Boolean, // percentageSpent >= alertThresholdPercent && percentageSpent <= 100.0
    val isExceeded: Boolean, // percentageSpent > 100.0
    val isConfigured: Boolean = true
)

data class BudgetAlert(
    val category: PaymentCategory,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val percentageSpent: Double,
    val alertThresholdPercent: Double,
    val isExceeded: Boolean,
    val isApproaching: Boolean = !isExceeded
)

data class MonthlyBudgetOverview(
    val targetMonth: YearMonth,
    val totalBudgetCap: Double,
    val totalSpentInBudgetedCategories: Double,
    val totalPlannedInBudgetedCategories: Double,
    val overallPercentageSpent: Double,
    val statuses: List<CategoryBudgetStatus>,
    val alerts: List<BudgetAlert>
)
