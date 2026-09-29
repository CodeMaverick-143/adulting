package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.BudgetCapEntity
import com.example.data.model.BillOccurrenceItem
import com.example.data.model.BudgetAlert
import com.example.data.model.CategoryBudgetStatus
import com.example.data.model.IncomeCategory
import com.example.data.model.IncomeOccurrenceItem
import com.example.data.model.MonthForecast
import com.example.data.model.MonthlyBudgetOverview
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.PaymentCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.data.model.TransactionType
import com.example.data.repository.FinancialPlannerRepository
import com.example.domain.AppDate
import com.example.domain.YearMonth
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialPlannerRepository
    val selectedMonth = MutableStateFlow(YearMonth.current())

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _forecast = MutableStateFlow<List<MonthForecast>>(emptyList())
    val forecast: StateFlow<List<MonthForecast>> = _forecast.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = FinancialPlannerRepository(db)
        refreshForecast()
    }

    val monthlySummary: StateFlow<MonthlyFinancialSummary> = selectedMonth
        .flatMapLatest { ym ->
            repository.getMonthlyFinancialSummary(ym)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyFinancialSummary(
                targetMonth = YearMonth.current(),
                expectedIncome = 0.0,
                receivedIncome = 0.0,
                remainingExpectedIncome = 0.0,
                totalPlannedPayments = 0.0,
                totalPaidPayments = 0.0,
                remainingUnpaidPayments = 0.0,
                overduePaymentsAmount = 0.0,
                overdueCount = 0,
                expectedRemainingBalance = 0.0,
                incomes = emptyList(),
                bills = emptyList()
            )
        )

    val incomeSources: StateFlow<List<IncomeSourceEntity>> = repository.getAllIncomeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringPayments: StateFlow<List<RecurringPaymentEntity>> = repository.getAllRecurringPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetOverview: StateFlow<MonthlyBudgetOverview> = selectedMonth
        .flatMapLatest { ym ->
            repository.getMonthlyBudgetOverview(ym)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyBudgetOverview(
                targetMonth = YearMonth.current(),
                totalBudgetCap = 0.0,
                totalSpentInBudgetedCategories = 0.0,
                totalPlannedInBudgetedCategories = 0.0,
                overallPercentageSpent = 0.0,
                statuses = emptyList(),
                alerts = emptyList()
            )
        )

    val budgetCaps: StateFlow<List<BudgetCapEntity>> = repository.getAllBudgetCaps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.nextMonth()
        refreshForecast()
    }

    fun prevMonth() {
        selectedMonth.value = selectedMonth.value.prevMonth()
        refreshForecast()
    }

    fun selectMonth(yearMonth: YearMonth) {
        selectedMonth.value = yearMonth
        refreshForecast()
    }

    fun jumpToToday() {
        selectedMonth.value = YearMonth.current()
        refreshForecast()
    }

    fun refreshForecast() {
        viewModelScope.launch {
            try {
                val f = repository.calculateMultiMonthForecast(selectedMonth.value, 12)
                _forecast.value = f
            } catch (e: Exception) {
                _forecast.value = emptyList()
            }
        }
    }

    // --- Income Actions ---

    fun saveIncomeSource(
        id: Long = 0,
        name: String,
        amount: Double,
        category: IncomeCategory,
        frequency: RecurrenceFrequency,
        startDate: AppDate,
        endDate: AppDate?,
        expectedPaymentDay: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val entity = IncomeSourceEntity(
                id = id,
                name = name.trim(),
                amount = amount,
                category = category.name,
                frequency = frequency.name,
                startDate = startDate.isoString,
                endDate = endDate?.isoString,
                expectedPaymentDay = expectedPaymentDay,
                notes = notes.trim(),
                status = ScheduleStatus.ACTIVE.name
            )
            repository.saveIncomeSource(entity)
            showMessage("Saved income source: ${entity.name}")
            refreshForecast()
        }
    }

    fun deleteIncomeSource(source: IncomeSourceEntity) {
        viewModelScope.launch {
            repository.deleteIncomeSource(source)
            showMessage("Deleted ${source.name}")
            refreshForecast()
        }
    }

    fun toggleIncomeStatus(source: IncomeSourceEntity) {
        viewModelScope.launch {
            val nextStatus = if (source.status == ScheduleStatus.ACTIVE.name) {
                ScheduleStatus.PAUSED
            } else {
                ScheduleStatus.ACTIVE
            }
            repository.setIncomeSourceStatus(source.id, nextStatus)
            showMessage("${source.name} marked as ${nextStatus.displayName}")
            refreshForecast()
        }
    }

    fun changeFutureIncomeAmount(sourceId: Long, effectiveDate: AppDate, newAmount: Double) {
        viewModelScope.launch {
            repository.changeFutureIncomeAmount(sourceId, effectiveDate.isoString, newAmount)
            showMessage("Future income amount set to ₹${newAmount.toLong()} from ${effectiveDate.isoString}")
            refreshForecast()
        }
    }

    fun markIncomeAsReceived(
        item: IncomeOccurrenceItem,
        amount: Double = item.expectedAmount,
        receivedDate: AppDate = AppDate.today(),
        paymentMethod: String = "Bank Account"
    ) {
        viewModelScope.launch {
            repository.markIncomeAsReceived(
                occurrenceId = item.id,
                incomeSourceId = item.incomeSourceId,
                occurrenceDate = item.occurrenceDate,
                amount = amount,
                receivedDate = receivedDate,
                paymentMethod = paymentMethod
            )
            showMessage("Received ₹${amount.toLong()} from ${item.name}")
            refreshForecast()
        }
    }

    fun markIncomeAsExpected(item: IncomeOccurrenceItem) {
        viewModelScope.launch {
            repository.markIncomeAsExpected(item.id)
            showMessage("Reverted ${item.name} to expected")
            refreshForecast()
        }
    }

    fun skipIncomeOccurrence(item: IncomeOccurrenceItem) {
        viewModelScope.launch {
            repository.skipIncomeOccurrence(item.id, item.incomeSourceId, item.occurrenceDate, item.expectedAmount)
            showMessage("Skipped income ${item.name} for ${item.occurrenceDate.isoString}")
            refreshForecast()
        }
    }

    // --- Recurring Payments Actions ---

    fun saveRecurringPayment(
        id: Long = 0,
        name: String,
        amount: Double,
        isVariableAmount: Boolean = false,
        category: PaymentCategory,
        frequency: RecurrenceFrequency,
        startDate: AppDate,
        endDate: AppDate?,
        dueDay: Int,
        isAutoRenewal: Boolean = true,
        notes: String = "",
        remindersDaysBefore: List<Int> = listOf(0, 1)
    ) {
        viewModelScope.launch {
            val entity = RecurringPaymentEntity(
                id = id,
                name = name.trim(),
                amount = amount,
                isVariableAmount = isVariableAmount,
                category = category.name,
                frequency = frequency.name,
                startDate = startDate.isoString,
                endDate = endDate?.isoString,
                dueDay = dueDay,
                isAutoRenewal = isAutoRenewal,
                notes = notes.trim(),
                status = ScheduleStatus.ACTIVE.name
            )
            val paymentId = repository.saveRecurringPayment(entity, remindersDaysBefore)
            // Schedule alarms for active bill
            val reminders = repository.getRemindersForPaymentOnce(paymentId)
            ReminderScheduler.scheduleRemindersForBill(
                getApplication(),
                entity.copy(id = paymentId),
                reminders
            )
            showMessage("Saved bill: ${entity.name}")
            refreshForecast()
        }
    }

    fun deleteRecurringPayment(payment: RecurringPaymentEntity) {
        viewModelScope.launch {
            val reminders = repository.getRemindersForPaymentOnce(payment.id)
            ReminderScheduler.cancelRemindersForBill(getApplication(), payment.id, reminders)
            repository.deleteRecurringPayment(payment)
            showMessage("Deleted ${payment.name}")
            refreshForecast()
        }
    }

    fun toggleRecurringPaymentStatus(payment: RecurringPaymentEntity) {
        viewModelScope.launch {
            val nextStatus = if (payment.status == ScheduleStatus.ACTIVE.name) {
                ScheduleStatus.PAUSED
            } else {
                ScheduleStatus.ACTIVE
            }
            repository.setRecurringPaymentStatus(payment.id, nextStatus)
            val updated = payment.copy(status = nextStatus.name)
            val reminders = repository.getRemindersForPaymentOnce(payment.id)
            if (nextStatus == ScheduleStatus.PAUSED) {
                ReminderScheduler.cancelRemindersForBill(getApplication(), payment.id, reminders)
            } else {
                ReminderScheduler.scheduleRemindersForBill(getApplication(), updated, reminders)
            }
            showMessage("${payment.name} marked as ${nextStatus.displayName}")
            refreshForecast()
        }
    }

    fun changeFuturePaymentAmount(paymentId: Long, effectiveDate: AppDate, newAmount: Double) {
        viewModelScope.launch {
            repository.changeFuturePaymentAmount(paymentId, effectiveDate.isoString, newAmount)
            showMessage("Future payment amount set to ₹${newAmount.toLong()} from ${effectiveDate.isoString}")
            refreshForecast()
        }
    }

    fun markPaymentAsPaid(
        item: BillOccurrenceItem,
        amount: Double = item.amount,
        paidDate: AppDate = AppDate.today(),
        paymentMethod: String = "Bank Account"
    ) {
        viewModelScope.launch {
            repository.markPaymentAsPaid(
                occurrenceId = item.id,
                recurringPaymentId = item.recurringPaymentId,
                dueDate = item.dueDate,
                amount = amount,
                paidDate = paidDate,
                paymentMethod = paymentMethod
            )
            val alert = repository.checkBudgetAlertOnPayment(item.category, selectedMonth.value)
            if (alert != null) {
                if (alert.isExceeded) {
                    showMessage("🚨 Paid ${item.name} (₹${amount.toLong()}) - ${item.category.displayName} is OVER BUDGET (₹${alert.spentAmount.toLong()} of ₹${alert.monthlyLimit.toLong()})!")
                } else if (alert.isApproaching) {
                    showMessage("⚠️ Paid ${item.name} (₹${amount.toLong()}) - ${item.category.displayName} is at ${alert.percentageSpent.toInt()}% of monthly budget cap!")
                } else {
                    showMessage("Marked ${item.name} as Paid (₹${amount.toLong()})")
                }
            } else {
                showMessage("Marked ${item.name} as Paid (₹${amount.toLong()})")
            }
            refreshForecast()
        }
    }

    fun markPaymentAsUnpaid(item: BillOccurrenceItem) {
        viewModelScope.launch {
            repository.markPaymentAsUnpaid(
                occurrenceId = item.id,
                recurringPaymentId = item.recurringPaymentId,
                dueDate = item.dueDate,
                amount = item.amount
            )
            showMessage("Reverted ${item.name} to unpaid")
            refreshForecast()
        }
    }

    fun skipPaymentOccurrence(item: BillOccurrenceItem) {
        viewModelScope.launch {
            repository.skipPaymentOccurrence(
                occurrenceId = item.id,
                recurringPaymentId = item.recurringPaymentId,
                dueDate = item.dueDate,
                amount = item.amount
            )
            showMessage("Skipped ${item.name} for ${item.dueDate.isoString}")
            refreshForecast()
        }
    }

    fun updateVariableOccurrenceAmount(item: BillOccurrenceItem, newAmount: Double) {
        viewModelScope.launch {
            repository.updateOccurrenceAmount(
                occurrenceId = item.id,
                recurringPaymentId = item.recurringPaymentId,
                dueDate = item.dueDate,
                newAmount = newAmount
            )
            showMessage("Updated amount for ${item.name} to ₹${newAmount.toLong()}")
            refreshForecast()
        }
    }

    fun getRemindersForPayment(paymentId: Long): Flow<List<ReminderEntity>> {
        return repository.getRemindersForPayment(paymentId)
    }

    fun updateRemindersForPayment(payment: RecurringPaymentEntity, daysBeforeList: List<Int>) {
        viewModelScope.launch {
            repository.saveRemindersForPayment(payment.id, daysBeforeList)
            val updatedReminders = repository.getRemindersForPaymentOnce(payment.id)
            ReminderScheduler.scheduleRemindersForBill(getApplication(), payment, updatedReminders)
            showMessage("Reminders updated for ${payment.name}")
        }
    }

    // --- Transactions Actions ---

    fun addManualTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: AppDate,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type.name,
                category = category.trim(),
                date = date.isoString,
                paymentMethod = paymentMethod.trim(),
                notes = notes.trim()
            )
            repository.insertTransaction(tx)
            showMessage("Logged transaction: ${tx.title}")
            refreshForecast()
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            showMessage("Deleted transaction")
            refreshForecast()
        }
    }

    // --- Budget Cap Actions ---

    fun saveBudgetCap(
        category: PaymentCategory,
        monthlyLimit: Double,
        alertThresholdPercent: Double = 80.0
    ) {
        viewModelScope.launch {
            repository.saveBudgetCap(category, monthlyLimit, alertThresholdPercent)
            showMessage("Set monthly budget cap for ${category.displayName}: ₹${monthlyLimit.toLong()} (Alert at ${alertThresholdPercent.toInt()}%)")
        }
    }

    fun deleteBudgetCap(id: Long, categoryName: String) {
        viewModelScope.launch {
            repository.deleteBudgetCap(id)
            showMessage("Removed budget cap for $categoryName")
        }
    }
}
