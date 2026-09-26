package com.example.domain

import com.example.data.model.PaymentStatus
import com.example.data.model.RecurrenceFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceCalculationTest {

    @Test
    fun `leap year detection is correct`() {
        assertTrue(AppDate.isLeapYear(2028))
        assertTrue(AppDate.isLeapYear(2024))
        assertTrue(AppDate.isLeapYear(2000))
        assertFalse(AppDate.isLeapYear(2027))
        assertFalse(AppDate.isLeapYear(1900))
        assertFalse(AppDate.isLeapYear(2026))
    }

    @Test
    fun `month end day clamping in leap and non-leap years`() {
        assertEquals(31, AppDate.getDaysInMonth(2027, 1))
        assertEquals(28, AppDate.getDaysInMonth(2027, 2)) // non-leap
        assertEquals(29, AppDate.getDaysInMonth(2028, 2)) // leap year!
        assertEquals(30, AppDate.getDaysInMonth(2027, 4))
    }

    @Test
    fun `monthly recurrence on 31st clamps to 28 in Feb and 30 in Apr`() {
        val start = AppDate(2027, 1, 31)

        val janOccurrences = RecurrenceCalculator.generateOccurrencesForMonth(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = start,
            endDate = null,
            preferredDayOfMonth = 31,
            targetMonth = YearMonth(2027, 1)
        )
        assertEquals(1, janOccurrences.size)
        assertEquals(AppDate(2027, 1, 31), janOccurrences.first())

        val febOccurrences = RecurrenceCalculator.generateOccurrencesForMonth(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = start,
            endDate = null,
            preferredDayOfMonth = 31,
            targetMonth = YearMonth(2027, 2)
        )
        assertEquals(1, febOccurrences.size)
        assertEquals(AppDate(2027, 2, 28), febOccurrences.first()) // Clamped to 28

        val aprOccurrences = RecurrenceCalculator.generateOccurrencesForMonth(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = start,
            endDate = null,
            preferredDayOfMonth = 31,
            targetMonth = YearMonth(2027, 4)
        )
        assertEquals(1, aprOccurrences.size)
        assertEquals(AppDate(2027, 4, 30), aprOccurrences.first()) // Clamped to 30
    }

    @Test
    fun `quarterly recurrence occurs strictly every 3 months`() {
        val start = AppDate(2027, 1, 15)

        val jan = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.QUARTERLY, start, null, 15, YearMonth(2027, 1)
        )
        assertEquals(1, jan.size)

        val feb = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.QUARTERLY, start, null, 15, YearMonth(2027, 2)
        )
        assertEquals(0, feb.size)

        val apr = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.QUARTERLY, start, null, 15, YearMonth(2027, 4)
        )
        assertEquals(1, apr.size)
        assertEquals(AppDate(2027, 4, 15), apr.first())
    }

    @Test
    fun `yearly recurrence occurs only in anniversary month`() {
        val start = AppDate(2027, 3, 20) // Mobile recharge in March

        val feb2028 = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.YEARLY, start, null, 20, YearMonth(2028, 2)
        )
        assertEquals(0, feb2028.size)

        val mar2028 = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.YEARLY, start, null, 20, YearMonth(2028, 3)
        )
        assertEquals(1, mar2028.size)
        assertEquals(AppDate(2028, 3, 20), mar2028.first())
    }

    @Test
    fun `start and end dates strictly respected`() {
        // Starts Feb 10 2027, ends Apr 15 2027
        val start = AppDate(2027, 2, 10)
        val end = AppDate(2027, 4, 15)

        // Jan 2027 before start
        val jan = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.MONTHLY, start, end, 1, YearMonth(2027, 1)
        )
        assertEquals(0, jan.size)

        // May 2027 after end
        val may = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.MONTHLY, start, end, 1, YearMonth(2027, 5)
        )
        assertEquals(0, may.size)

        // Mar 2027 within range
        val mar = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.MONTHLY, start, end, 1, YearMonth(2027, 3)
        )
        assertEquals(1, mar.size)
    }

    @Test
    fun `effective amount change preserves historical amounts`() {
        val baseAmount = 50000.0
        val effectiveDate = AppDate(2027, 4, 1)
        val newAmount = 60000.0

        // Jan 2027 (before effective date) -> base amount
        val janAmt = RecurrenceCalculator.resolveEffectiveAmount(
            baseAmount,
            AppDate(2027, 1, 1),
            effectiveDate,
            newAmount
        )
        assertEquals(50000.0, janAmt, 0.001)

        // Mar 2027 (before effective date) -> base amount
        val marAmt = RecurrenceCalculator.resolveEffectiveAmount(
            baseAmount,
            AppDate(2027, 3, 31),
            effectiveDate,
            newAmount
        )
        assertEquals(50000.0, marAmt, 0.001)

        // Apr 2027 (on effective date) -> new amount
        val aprAmt = RecurrenceCalculator.resolveEffectiveAmount(
            baseAmount,
            AppDate(2027, 4, 1),
            effectiveDate,
            newAmount
        )
        assertEquals(60000.0, aprAmt, 0.001)

        // May 2027 (after effective date) -> new amount
        val mayAmt = RecurrenceCalculator.resolveEffectiveAmount(
            baseAmount,
            AppDate(2027, 5, 1),
            effectiveDate,
            newAmount
        )
        assertEquals(60000.0, mayAmt, 0.001)
    }

    @Test
    fun `weekly recurrence generates multiple dates in month`() {
        val start = AppDate(2027, 1, 1) // Friday
        val janOccurrences = RecurrenceCalculator.generateOccurrencesForMonth(
            RecurrenceFrequency.WEEKLY,
            start,
            null,
            1,
            YearMonth(2027, 1)
        )
        // In Jan 2027: 1, 8, 15, 22, 29 (5 occurrences)
        assertEquals(5, janOccurrences.size)
    }
}
