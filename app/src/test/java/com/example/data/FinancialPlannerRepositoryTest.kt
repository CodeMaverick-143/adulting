package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetCapEntity
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.IncomeCategory
import com.example.data.model.PaymentCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.data.model.TransactionType
import com.example.data.repository.FinancialPlannerRepository
import com.example.domain.AppDate
import com.example.domain.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialPlannerRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinancialPlannerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinancialPlannerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `monthly summary calculation matches expected example`() = runBlocking {
        // Income: Salary ₹50,000 monthly
        val salaryId = repository.saveIncomeSource(
            IncomeSourceEntity(
                name = "Tech Salary",
                amount = 50000.0,
                category = IncomeCategory.SALARY.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                expectedPaymentDay = 1,
                status = ScheduleStatus.ACTIVE.name
            )
        )
        assertTrue(salaryId > 0)

        // Rent: ₹15,000
        repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Rent",
                amount = 15000.0,
                isVariableAmount = false,
                category = PaymentCategory.HOUSING.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 5
            )
        )

        // Netflix: ₹649
        repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Netflix",
                amount = 649.0,
                isVariableAmount = false,
                category = PaymentCategory.SUBSCRIPTIONS.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 10
            )
        )

        // Electricity: ₹1,500 (variable)
        repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Electricity",
                amount = 1500.0,
                isVariableAmount = true,
                category = PaymentCategory.UTILITIES.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 15
            )
        )

        val targetMonth = YearMonth(2027, 1)
        val summary = repository.getMonthlyFinancialSummary(targetMonth).first()

        assertEquals(50000.0, summary.expectedIncome, 0.001)
        assertEquals(17149.0, summary.totalPlannedPayments, 0.001)
        assertEquals(32851.0, summary.expectedRemainingBalance, 0.001)
        assertEquals(3, summary.bills.size)
        assertEquals(1, summary.incomes.size)
    }

    @Test
    fun `marking payment as paid creates transaction and prevents duplicates`() = runBlocking {
        val paymentId = repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Rent",
                amount = 15000.0,
                category = PaymentCategory.HOUSING.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 5
            )
        )

        val dueDate = AppDate(2027, 1, 5)

        // Mark paid
        repository.markPaymentAsPaid(
            occurrenceId = 0L,
            recurringPaymentId = paymentId,
            dueDate = dueDate,
            amount = 15000.0,
            paidDate = AppDate(2027, 1, 5),
            paymentMethod = "Bank Transfer"
        )

        // Verify transaction exists
        val txs = repository.getAllTransactions().first()
        assertEquals(1, txs.size)
        assertEquals("Rent", txs.first().title)
        assertEquals(15000.0, txs.first().amount, 0.001)
        assertEquals(TransactionType.EXPENSE.name, txs.first().type)

        // Verify occurrence status in summary
        val summary1 = repository.getMonthlyFinancialSummary(YearMonth(2027, 1)).first()
        assertEquals(15000.0, summary1.totalPaidPayments, 0.001)
        assertEquals(0.0, summary1.remainingUnpaidPayments, 0.001)
        assertEquals(PaymentStatus.PAID, summary1.bills.first().status)

        // Attempting to mark paid again must NOT create a duplicate transaction
        repository.markPaymentAsPaid(
            occurrenceId = summary1.bills.first().id,
            recurringPaymentId = paymentId,
            dueDate = dueDate,
            amount = 15000.0,
            paidDate = AppDate(2027, 1, 5)
        )

        val txsAfter = repository.getAllTransactions().first()
        assertEquals(1, txsAfter.size) // Still exactly 1 transaction!
    }

    @Test
    fun `reverting paid payment deletes linked transaction`() = runBlocking {
        val paymentId = repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Gym",
                amount = 2000.0,
                category = PaymentCategory.HEALTH.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 1
            )
        )

        repository.markPaymentAsPaid(
            occurrenceId = 0L,
            recurringPaymentId = paymentId,
            dueDate = AppDate(2027, 1, 1),
            amount = 2000.0
        )
        assertEquals(1, repository.getAllTransactions().first().size)

        val summary = repository.getMonthlyFinancialSummary(YearMonth(2027, 1)).first()
        val occId = summary.bills.first().id
        assertTrue(occId > 0)

        // Mark unpaid
        repository.markPaymentAsUnpaid(
            occurrenceId = occId,
            recurringPaymentId = paymentId,
            dueDate = AppDate(2027, 1, 1),
            amount = 2000.0
        )

        assertEquals(0, repository.getAllTransactions().first().size)
    }

    @Test
    fun `variable bill amount update updates that specific occurrence amount`() = runBlocking {
        val billId = repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Electricity",
                amount = 1200.0,
                isVariableAmount = true,
                category = PaymentCategory.UTILITIES.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 20
            )
        )

        val targetMonth = YearMonth(2027, 1)
        val initialSummary = repository.getMonthlyFinancialSummary(targetMonth).first()
        assertEquals(1200.0, initialSummary.totalPlannedPayments, 0.001)

        // Update electricity for Jan 2027 to ₹1,850
        repository.updateOccurrenceAmount(
            occurrenceId = 0L,
            recurringPaymentId = billId,
            dueDate = AppDate(2027, 1, 20),
            newAmount = 1850.0
        )

        val updatedSummary = repository.getMonthlyFinancialSummary(targetMonth).first()
        assertEquals(1850.0, updatedSummary.totalPlannedPayments, 0.001)
        assertEquals(1850.0, updatedSummary.bills.first().amount, 0.001)
    }

    @Test
    fun `multi-month forecast detects negative balances accurately`() = runBlocking {
        // Income ₹10,000 monthly
        repository.saveIncomeSource(
            IncomeSourceEntity(
                name = "Stipend",
                amount = 10000.0,
                category = IncomeCategory.ALLOWANCE.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                expectedPaymentDay = 1
            )
        )

        // Monthly rent ₹8,000
        repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Rent",
                amount = 8000.0,
                category = PaymentCategory.HOUSING.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 5
            )
        )

        // Annual mobile recharge ₹3,999 in March
        repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Annual Mobile Recharge",
                amount = 3999.0,
                category = PaymentCategory.RECHARGE.name,
                frequency = RecurrenceFrequency.YEARLY.name,
                startDate = "2027-03-10",
                dueDay = 10
            )
        )

        val forecast = repository.calculateMultiMonthForecast(YearMonth(2027, 1), 6)
        assertEquals(6, forecast.size)

        // Jan: Net = 10000 - 8000 = +2000
        assertEquals(2000.0, forecast[0].projectedNet, 0.001)
        assertEquals(false, forecast[0].isNegative)

        // Feb: Net = 10000 - 8000 = +2000
        assertEquals(2000.0, forecast[1].projectedNet, 0.001)
        assertEquals(false, forecast[1].isNegative)

        // Mar: Net = 10000 - (8000 + 3999) = -1999 (NEGATIVE!)
        assertEquals(-1999.0, forecast[2].projectedNet, 0.001)
        assertEquals(true, forecast[2].isNegative)
        assertTrue(forecast[2].nonMonthlyCommitments.any { it.contains("Annual Mobile Recharge") })
    }

    @Test
    fun `budget cap alerts when spending approaches and exceeds limits`() = runBlocking {
        // Set budget cap for SUBSCRIPTIONS: ₹1,000 monthly, alert threshold 80%
        val capId = repository.saveBudgetCap(
            category = PaymentCategory.SUBSCRIPTIONS,
            monthlyLimit = 1000.0,
            alertThresholdPercent = 80.0
        )
        assertTrue(capId > 0)

        val targetMonth = YearMonth(2027, 1)

        // Initial overview with 0 spending: no alert
        var overview = repository.getMonthlyBudgetOverview(targetMonth).first()
        assertEquals(1, overview.statuses.size)
        assertEquals(0.0, overview.statuses[0].spentAmount, 0.001)
        assertEquals(false, overview.statuses[0].isApproaching)
        assertEquals(false, overview.statuses[0].isExceeded)
        assertTrue(overview.alerts.isEmpty())

        // Save a subscription bill of ₹850 (85% of limit, approaching threshold of 80%)
        val billId = repository.saveRecurringPayment(
            RecurringPaymentEntity(
                name = "Streaming Bundle",
                amount = 850.0,
                category = PaymentCategory.SUBSCRIPTIONS.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = "2027-01-01",
                dueDay = 15
            )
        )

        // Mark it as paid
        repository.markPaymentAsPaid(
            occurrenceId = 0,
            recurringPaymentId = billId,
            dueDate = AppDate(2027, 1, 15),
            amount = 850.0
        )

        // Check overview: should show approaching alert
        overview = repository.getMonthlyBudgetOverview(targetMonth).first()
        val status = overview.statuses.first { it.category == PaymentCategory.SUBSCRIPTIONS }
        assertEquals(850.0, status.spentAmount, 0.001)
        assertEquals(85.0, status.percentageSpent, 0.001)
        assertEquals(true, status.isApproaching)
        assertEquals(false, status.isExceeded)
        assertEquals(1, overview.alerts.size)
        assertEquals(PaymentCategory.SUBSCRIPTIONS, overview.alerts[0].category)
        assertEquals(true, overview.alerts[0].isApproaching)
        assertEquals(false, overview.alerts[0].isExceeded)

        // Now check alert directly via checkBudgetAlertOnPayment
        val alert = repository.checkBudgetAlertOnPayment(PaymentCategory.SUBSCRIPTIONS, targetMonth)
        assertNotNull(alert)
        assertEquals(true, alert?.isApproaching)
        assertEquals(false, alert?.isExceeded)

        // Add additional manual transaction of ₹200 to exceed limit (850 + 200 = 1050 > 1000)
        repository.insertTransaction(
            TransactionEntity(
                title = "Extra channel add-on",
                amount = 200.0,
                type = TransactionType.EXPENSE.name,
                category = PaymentCategory.SUBSCRIPTIONS.name,
                date = AppDate(2027, 1, 20).isoString,
                paymentMethod = "UPI"
            )
        )

        overview = repository.getMonthlyBudgetOverview(targetMonth).first()
        val updatedStatus = overview.statuses.first { it.category == PaymentCategory.SUBSCRIPTIONS }
        assertEquals(1050.0, updatedStatus.spentAmount, 0.001)
        assertEquals(105.0, updatedStatus.percentageSpent, 0.001)
        assertEquals(false, updatedStatus.isApproaching)
        assertEquals(true, updatedStatus.isExceeded)
        assertEquals(1, overview.alerts.size)
        assertEquals(true, overview.alerts[0].isExceeded)
    }
}
