package com.example.domain

import com.example.data.model.PaymentStatus
import com.example.data.model.RecurrenceFrequency
import java.util.Locale

/**
 * Lightweight, robust date representation (YYYY-MM-DD) that works reliably across all Android API versions.
 */
data class AppDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<AppDate> {

    init {
        require(month in 1..12) { "Month must be between 1 and 12, got $month" }
        val maxDays = getDaysInMonth(year, month)
        require(day in 1..maxDays) { "Day $day is invalid for $year-$month (max $maxDays)" }
    }

    val yearMonth: String
        get() = String.format(Locale.US, "%04d-%02d", year, month)

    val isoString: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    override fun compareTo(other: AppDate): Int {
        if (this.year != other.year) return this.year.compareTo(other.year)
        if (this.month != other.month) return this.month.compareTo(other.month)
        return this.day.compareTo(other.day)
    }

    fun plusDays(days: Int): AppDate {
        if (days == 0) return this
        var y = year
        var m = month
        var d = day + days

        if (days > 0) {
            while (true) {
                val daysInM = getDaysInMonth(y, m)
                if (d <= daysInM) break
                d -= daysInM
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
            }
        } else {
            while (d < 1) {
                m--
                if (m < 1) {
                    m = 12
                    y--
                }
                val daysInPrevM = getDaysInMonth(y, m)
                d += daysInPrevM
            }
        }
        return AppDate(y, m, d)
    }

    fun plusMonths(months: Int): AppDate {
        val totalMonths = (year * 12 + (month - 1)) + months
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        val maxDays = getDaysInMonth(newYear, newMonth)
        val newDay = minOf(day, maxDays)
        return AppDate(newYear, newMonth, newDay)
    }

    fun daysBetween(other: AppDate): Long {
        return this.toEpochDay() - other.toEpochDay()
    }

    fun toEpochDay(): Long {
        // Compute astronomical days since 1970-01-01
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) {
            y--
            m += 12
        }
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    companion object {
        fun parse(iso: String): AppDate {
            val parts = iso.trim().split("-")
            require(parts.size == 3) { "Invalid ISO date: $iso" }
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            val safeD = minOf(d, getDaysInMonth(y, m))
            return AppDate(y, m, safeD)
        }

        fun fromYearMonth(yearMonth: String, day: Int): AppDate {
            val parts = yearMonth.trim().split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val safeDay = minOf(day, getDaysInMonth(y, m))
            return AppDate(y, m, safeDay)
        }

        fun today(): AppDate {
            val cal = java.util.Calendar.getInstance()
            return AppDate(
                year = cal.get(java.util.Calendar.YEAR),
                month = cal.get(java.util.Calendar.MONTH) + 1,
                day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

        fun isLeapYear(year: Int): Boolean {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        }

        fun getDaysInMonth(year: Int, month: Int): Int {
            return when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(year)) 29 else 28
                else -> 30
            }
        }
    }
}

data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    init {
        require(month in 1..12) { "Month must be 1..12" }
    }

    val formatted: String
        get() = String.format(Locale.US, "%04d-%02d", year, month)

    fun monthName(): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }
    }

    fun shortMonthName(): String {
        return monthName().take(3)
    }

    fun displayTitle(): String = "${monthName()} $year"

    fun nextMonth(): YearMonth {
        return if (month == 12) YearMonth(year + 1, 1) else YearMonth(year, month + 1)
    }

    fun prevMonth(): YearMonth {
        return if (month == 1) YearMonth(year - 1, 12) else YearMonth(year, month - 1)
    }

    fun plusMonths(count: Int): YearMonth {
        val total = (year * 12 + (month - 1)) + count
        val newYear = total / 12
        val newMonth = (total % 12) + 1
        return YearMonth(newYear, newMonth)
    }

    override fun compareTo(other: YearMonth): Int {
        if (this.year != other.year) return this.year.compareTo(other.year)
        return this.month.compareTo(other.month)
    }

    companion object {
        fun current(): YearMonth {
            val t = AppDate.today()
            return YearMonth(t.year, t.month)
        }

        fun parse(ym: String): YearMonth {
            val parts = ym.trim().split("-")
            return YearMonth(parts[0].toInt(), parts[1].toInt())
        }
    }
}

object RecurrenceCalculator {

    /**
     * Resolves the effective amount for a schedule on a specific occurrence date.
     * Guarantees that historical occurrences before effectiveAmountChangeDate retain the original amount.
     */
    fun resolveEffectiveAmount(
        baseAmount: Double,
        occurrenceDate: AppDate,
        effectiveChangeDate: AppDate?,
        newAmount: Double?
    ): Double {
        if (effectiveChangeDate != null && newAmount != null && occurrenceDate >= effectiveChangeDate) {
            return newAmount
        }
        return baseAmount
    }

    /**
     * Computes the payment status for an unpaid occurrence given its due date and today's date.
     */
    fun computeUnpaidStatus(dueDate: AppDate, today: AppDate = AppDate.today()): PaymentStatus {
        return if (dueDate < today) PaymentStatus.OVERDUE else PaymentStatus.UNPAID
    }

    /**
     * Generates all occurrence dates for a schedule within a specified target month.
     * Supports: ONE_TIME, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY.
     * Accurately handles leap years, month-end clamping, and start/end date constraints.
     */
    fun generateOccurrencesForMonth(
        frequency: RecurrenceFrequency,
        startDate: AppDate,
        endDate: AppDate?,
        preferredDayOfMonth: Int,
        targetMonth: YearMonth
    ): List<AppDate> {
        val firstDayOfTarget = AppDate(targetMonth.year, targetMonth.month, 1)
        val maxDaysInTarget = AppDate.getDaysInMonth(targetMonth.year, targetMonth.month)
        val lastDayOfTarget = AppDate(targetMonth.year, targetMonth.month, maxDaysInTarget)

        // Check overall boundaries
        if (endDate != null && endDate < firstDayOfTarget) return emptyList()
        if (startDate > lastDayOfTarget) return emptyList()

        val occurrences = mutableListOf<AppDate>()

        when (frequency) {
            RecurrenceFrequency.ONE_TIME -> {
                if (startDate.year == targetMonth.year && startDate.month == targetMonth.month) {
                    occurrences.add(startDate)
                }
            }

            RecurrenceFrequency.MONTHLY -> {
                val clampedDay = minOf(preferredDayOfMonth, maxDaysInTarget)
                val candidate = AppDate(targetMonth.year, targetMonth.month, clampedDay)
                if (candidate >= startDate && (endDate == null || candidate <= endDate)) {
                    occurrences.add(candidate)
                }
            }

            RecurrenceFrequency.QUARTERLY -> {
                val monthsDiff = (targetMonth.year - startDate.year) * 12 + (targetMonth.month - startDate.month)
                if (monthsDiff >= 0 && monthsDiff % 3 == 0) {
                    val clampedDay = minOf(preferredDayOfMonth, maxDaysInTarget)
                    val candidate = AppDate(targetMonth.year, targetMonth.month, clampedDay)
                    if (candidate >= startDate && (endDate == null || candidate <= endDate)) {
                        occurrences.add(candidate)
                    }
                }
            }

            RecurrenceFrequency.HALF_YEARLY -> {
                val monthsDiff = (targetMonth.year - startDate.year) * 12 + (targetMonth.month - startDate.month)
                if (monthsDiff >= 0 && monthsDiff % 6 == 0) {
                    val clampedDay = minOf(preferredDayOfMonth, maxDaysInTarget)
                    val candidate = AppDate(targetMonth.year, targetMonth.month, clampedDay)
                    if (candidate >= startDate && (endDate == null || candidate <= endDate)) {
                        occurrences.add(candidate)
                    }
                }
            }

            RecurrenceFrequency.YEARLY -> {
                if (targetMonth.month == startDate.month && targetMonth.year >= startDate.year) {
                    val clampedDay = minOf(preferredDayOfMonth, maxDaysInTarget)
                    val candidate = AppDate(targetMonth.year, targetMonth.month, clampedDay)
                    if (candidate >= startDate && (endDate == null || candidate <= endDate)) {
                        occurrences.add(candidate)
                    }
                }
            }

            RecurrenceFrequency.WEEKLY, RecurrenceFrequency.BIWEEKLY -> {
                val stepDays = if (frequency == RecurrenceFrequency.WEEKLY) 7 else 14
                var curr = startDate
                // Advance forward if starting far in the past to avoid looping thousands of times
                val daysBeforeTarget = firstDayOfTarget.daysBetween(curr)
                if (daysBeforeTarget > 0) {
                    val stepsToSkip = (daysBeforeTarget / stepDays).toInt()
                    if (stepsToSkip > 0) {
                        curr = curr.plusDays(stepsToSkip * stepDays)
                    }
                }

                while (curr < firstDayOfTarget) {
                    curr = curr.plusDays(stepDays)
                }

                while (curr <= lastDayOfTarget) {
                    if (curr >= startDate && (endDate == null || curr <= endDate)) {
                        occurrences.add(curr)
                    }
                    curr = curr.plusDays(stepDays)
                }
            }

            RecurrenceFrequency.CUSTOM -> {
                // Fallback custom default to monthly
                val clampedDay = minOf(preferredDayOfMonth, maxDaysInTarget)
                val candidate = AppDate(targetMonth.year, targetMonth.month, clampedDay)
                if (candidate >= startDate && (endDate == null || candidate <= endDate)) {
                    occurrences.add(candidate)
                }
            }
        }

        return occurrences
    }
}
