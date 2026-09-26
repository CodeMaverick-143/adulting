package com.example.data.model

enum class RecurrenceFrequency(val displayName: String) {
    ONE_TIME("One-time"),
    WEEKLY("Weekly"),
    BIWEEKLY("Biweekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    HALF_YEARLY("Half-yearly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

enum class IncomeCategory(val displayName: String) {
    SALARY("Salary"),
    FREELANCING("Freelancing"),
    BUSINESS("Business income"),
    ALLOWANCE("Allowance"),
    INVESTMENT("Investment"),
    OTHER("Other income")
}

enum class PaymentCategory(val displayName: String) {
    HOUSING("Housing / Rent"),
    UTILITIES("Utilities / Bills"),
    SUBSCRIPTIONS("Subscriptions"),
    RECHARGE("Mobile & Internet"),
    INSURANCE("Insurance"),
    LOANS("Loans & EMIs"),
    EDUCATION("Education"),
    HEALTH("Health & Medical"),
    OTHER("Other")
}

enum class ScheduleStatus(val displayName: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

enum class IncomeStatus(val displayName: String) {
    EXPECTED("Expected"),
    RECEIVED("Received"),
    SKIPPED("Skipped")
}

enum class PaymentStatus(val displayName: String) {
    UNPAID("Unpaid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    SKIPPED("Skipped")
}

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}
