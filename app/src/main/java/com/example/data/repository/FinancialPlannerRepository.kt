package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.IncomeOccurrenceEntity
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.PaymentOccurrenceEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.BudgetCapEntity
import com.example.data.model.BillOccurrenceItem
import com.example.data.model.BudgetAlert
import com.example.data.model.CategoryBudgetStatus
import com.example.data.model.IncomeCategory
import com.example.data.model.IncomeOccurrenceItem
import com.example.data.model.IncomeStatus
import com.example.data.model.MonthForecast
import com.example.data.model.MonthlyBudgetOverview
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.PaymentCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.data.model.TransactionType
import com.example.domain.AppDate
import com.example.domain.RecurrenceCalculator
import com.example.domain.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class FinancialPlannerRepository(private val database: AppDatabase) {

    private val incomeDao = database.incomeDao()
    private val recurringPaymentDao = database.recurringPaymentDao()
    private val occurrenceDao = database.occurrenceDao()
    private val transactionDao = database.transactionDao()
    private val reminderDao = database.reminderDao()
    private val budgetCapDao = database.budgetCapDao()

    // --- Income Sources ---

    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>> = incomeDao.getAllIncomeSources()

    fun getActiveIncomeSources(): Flow<List<IncomeSourceEntity>> = incomeDao.getActiveIncomeSources()

    fun getIncomeSourceById(id: Long): Flow<IncomeSourceEntity?> = incomeDao.getIncomeSourceById(id)

    suspend fun saveIncomeSource(entity: IncomeSourceEntity): Long = withContext(Dispatchers.IO) {
        incomeDao.insertIncomeSource(entity)
    }

    suspend fun updateIncomeSource(entity: IncomeSourceEntity) = withContext(Dispatchers.IO) {
        incomeDao.updateIncomeSource(entity)
    }

    suspend fun deleteIncomeSource(entity: IncomeSourceEntity) = withContext(Dispatchers.IO) {
        incomeDao.deleteIncomeSource(entity)
    }

    suspend fun setIncomeSourceStatus(id: Long, status: ScheduleStatus) = withContext(Dispatchers.IO) {
        incomeDao.updateStatus(id, status.name)
    }

    suspend fun changeFutureIncomeAmount(id: Long, effectiveDate: String, newAmount: Double) = withContext(Dispatchers.IO) {
        incomeDao.updateEffectiveAmount(id, effectiveDate, newAmount)
    }

    // --- Recurring Payments ---

    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>> = recurringPaymentDao.getAllRecurringPayments()

    fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>> = recurringPaymentDao.getActiveRecurringPayments()

    fun getRecurringPaymentById(id: Long): Flow<RecurringPaymentEntity?> = recurringPaymentDao.getRecurringPaymentById(id)

    suspend fun saveRecurringPayment(
        entity: RecurringPaymentEntity,
        remindersDaysBefore: List<Int> = listOf(0, 1)
    ): Long = withContext(Dispatchers.IO) {
        val paymentId = recurringPaymentDao.insertRecurringPayment(entity)
        reminderDao.deleteRemindersForPayment(paymentId)
        val reminderEntities = remindersDaysBefore.distinct().map { days ->
            ReminderEntity(
                recurringPaymentId = paymentId,
                daysBefore = days,
                reminderHour = 9,
                reminderMinute = 0,
                isEnabled = true
            )
        }
        reminderDao.insertReminders(reminderEntities)
        paymentId
    }

    suspend fun updateRecurringPayment(entity: RecurringPaymentEntity) = withContext(Dispatchers.IO) {
        recurringPaymentDao.updateRecurringPayment(entity)
    }

    suspend fun deleteRecurringPayment(entity: RecurringPaymentEntity) = withContext(Dispatchers.IO) {
        recurringPaymentDao.deleteRecurringPayment(entity)
    }

    suspend fun setRecurringPaymentStatus(id: Long, status: ScheduleStatus) = withContext(Dispatchers.IO) {
        recurringPaymentDao.updateStatus(id, status.name)
    }

    suspend fun changeFuturePaymentAmount(id: Long, effectiveDate: String, newAmount: Double) = withContext(Dispatchers.IO) {
        recurringPaymentDao.updateEffectiveAmount(id, effectiveDate, newAmount)
    }

    // --- Monthly Summary Calculation ---

    fun getMonthlyFinancialSummary(yearMonth: YearMonth): Flow<MonthlyFinancialSummary> {
        val prefix = yearMonth.formatted
        return combine(
            incomeDao.getAllIncomeSources(),
            occurrenceDao.getIncomeOccurrencesForMonth(prefix),
            recurringPaymentDao.getAllRecurringPayments(),
            occurrenceDao.getPaymentOccurrencesForMonth(prefix)
        ) { incomeSources, incomeOccurrences, recurringPayments, paymentOccurrences ->
            calculateSummary(yearMonth, incomeSources, incomeOccurrences, recurringPayments, paymentOccurrences)
        }
    }

    fun calculateSummary(
        yearMonth: YearMonth,
        incomeSources: List<IncomeSourceEntity>,
        storedIncomeOccurrences: List<IncomeOccurrenceEntity>,
        recurringPayments: List<RecurringPaymentEntity>,
        storedPaymentOccurrences: List<PaymentOccurrenceEntity>
    ): MonthlyFinancialSummary {
        val today = AppDate.today()
        val storedIncomeMap = storedIncomeOccurrences.associateBy { "${it.incomeSourceId}_${it.occurrenceDate}" }
        val storedPaymentMap = storedPaymentOccurrences.associateBy { "${it.recurringPaymentId}_${it.dueDate}" }

        // Process Incomes
        val incomeItems = mutableListOf<IncomeOccurrenceItem>()
        for (source in incomeSources) {
            val freq = runCatching { RecurrenceFrequency.valueOf(source.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
            val cat = runCatching { IncomeCategory.valueOf(source.category) }.getOrDefault(IncomeCategory.OTHER)
            val scheduleStatus = runCatching { ScheduleStatus.valueOf(source.status) }.getOrDefault(ScheduleStatus.ACTIVE)

            if (scheduleStatus == ScheduleStatus.PAUSED && storedIncomeOccurrences.none { it.incomeSourceId == source.id }) {
                continue
            }

            val startDate = runCatching { AppDate.parse(source.startDate) }.getOrDefault(AppDate.today())
            val endDate = source.endDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }
            val effectiveChangeDate = source.effectiveAmountChangeDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }

            val dates = RecurrenceCalculator.generateOccurrencesForMonth(
                frequency = freq,
                startDate = startDate,
                endDate = endDate,
                preferredDayOfMonth = source.expectedPaymentDay,
                targetMonth = yearMonth
            )

            for (date in dates) {
                val key = "${source.id}_${date.isoString}"
                val stored = storedIncomeMap[key]

                val expectedAmt = RecurrenceCalculator.resolveEffectiveAmount(
                    baseAmount = source.amount,
                    occurrenceDate = date,
                    effectiveChangeDate = effectiveChangeDate,
                    newAmount = source.newAmount
                )

                if (stored != null) {
                    val status = runCatching { IncomeStatus.valueOf(stored.status) }.getOrDefault(IncomeStatus.EXPECTED)
                    incomeItems.add(
                        IncomeOccurrenceItem(
                            id = stored.id,
                            incomeSourceId = source.id,
                            name = source.name,
                            category = cat,
                            frequency = freq,
                            occurrenceDate = date,
                            expectedAmount = stored.expectedAmount,
                            receivedAmount = stored.receivedAmount,
                            status = status,
                            isProjected = false,
                            transactionId = stored.transactionId,
                            notes = stored.notes
                        )
                    )
                } else if (scheduleStatus == ScheduleStatus.ACTIVE) {
                    incomeItems.add(
                        IncomeOccurrenceItem(
                            id = 0,
                            incomeSourceId = source.id,
                            name = source.name,
                            category = cat,
                            frequency = freq,
                            occurrenceDate = date,
                            expectedAmount = expectedAmt,
                            receivedAmount = null,
                            status = IncomeStatus.EXPECTED,
                            isProjected = true
                        )
                    )
                }
            }
        }
        incomeItems.sortBy { it.occurrenceDate }

        // Process Bills
        val billItems = mutableListOf<BillOccurrenceItem>()
        for (bill in recurringPayments) {
            val freq = runCatching { RecurrenceFrequency.valueOf(bill.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
            val cat = runCatching { PaymentCategory.valueOf(bill.category) }.getOrDefault(PaymentCategory.OTHER)
            val scheduleStatus = runCatching { ScheduleStatus.valueOf(bill.status) }.getOrDefault(ScheduleStatus.ACTIVE)

            if (scheduleStatus == ScheduleStatus.PAUSED && storedPaymentOccurrences.none { it.recurringPaymentId == bill.id }) {
                continue
            }

            val startDate = runCatching { AppDate.parse(bill.startDate) }.getOrDefault(AppDate.today())
            val endDate = bill.endDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }
            val effectiveChangeDate = bill.effectiveAmountChangeDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }

            val dates = RecurrenceCalculator.generateOccurrencesForMonth(
                frequency = freq,
                startDate = startDate,
                endDate = endDate,
                preferredDayOfMonth = bill.dueDay,
                targetMonth = yearMonth
            )

            for (date in dates) {
                val key = "${bill.id}_${date.isoString}"
                val stored = storedPaymentMap[key]

                val resolvedAmt = RecurrenceCalculator.resolveEffectiveAmount(
                    baseAmount = bill.amount,
                    occurrenceDate = date,
                    effectiveChangeDate = effectiveChangeDate,
                    newAmount = bill.newAmount
                )

                if (stored != null) {
                    var status = runCatching { PaymentStatus.valueOf(stored.status) }.getOrDefault(PaymentStatus.UNPAID)
                    if (status == PaymentStatus.UNPAID && date < today) {
                        status = PaymentStatus.OVERDUE
                    }
                    val paidDate = stored.paidDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }

                    billItems.add(
                        BillOccurrenceItem(
                            id = stored.id,
                            recurringPaymentId = bill.id,
                            name = bill.name,
                            category = cat,
                            frequency = freq,
                            dueDate = date,
                            amount = stored.amount,
                            isVariableAmount = bill.isVariableAmount,
                            status = status,
                            paidDate = paidDate,
                            isProjected = false,
                            transactionId = stored.transactionId,
                            isAutoRenewal = bill.isAutoRenewal,
                            notes = stored.notes
                        )
                    )
                } else if (scheduleStatus == ScheduleStatus.ACTIVE) {
                    val status = RecurrenceCalculator.computeUnpaidStatus(date, today)
                    billItems.add(
                        BillOccurrenceItem(
                            id = 0,
                            recurringPaymentId = bill.id,
                            name = bill.name,
                            category = cat,
                            frequency = freq,
                            dueDate = date,
                            amount = resolvedAmt,
                            isVariableAmount = bill.isVariableAmount,
                            status = status,
                            paidDate = null,
                            isProjected = true,
                            isAutoRenewal = bill.isAutoRenewal
                        )
                    )
                }
            }
        }
        billItems.sortBy { it.dueDate }

        // Calculations
        val totalExpectedIncome = incomeItems.filter { it.status != IncomeStatus.SKIPPED }.sumOf { it.expectedAmount }
        val totalReceivedIncome = incomeItems.filter { it.status == IncomeStatus.RECEIVED }.sumOf { it.receivedAmount ?: it.expectedAmount }
        val remainingExpectedIncome = incomeItems.filter { it.status == IncomeStatus.EXPECTED }.sumOf { it.expectedAmount }

        val activeBills = billItems.filter { it.status != PaymentStatus.SKIPPED }
        val totalPlannedPayments = activeBills.sumOf { it.amount }
        val totalPaidPayments = activeBills.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
        val unpaidBills = activeBills.filter { it.status == PaymentStatus.UNPAID || it.status == PaymentStatus.OVERDUE }
        val remainingUnpaidPayments = unpaidBills.sumOf { it.amount }
        val overdueBills = activeBills.filter { it.status == PaymentStatus.OVERDUE }
        val overduePaymentsAmount = overdueBills.sumOf { it.amount }

        val expectedRemainingBalance = (totalReceivedIncome + remainingExpectedIncome) - totalPlannedPayments

        return MonthlyFinancialSummary(
            targetMonth = yearMonth,
            expectedIncome = totalExpectedIncome,
            receivedIncome = totalReceivedIncome,
            remainingExpectedIncome = remainingExpectedIncome,
            totalPlannedPayments = totalPlannedPayments,
            totalPaidPayments = totalPaidPayments,
            remainingUnpaidPayments = remainingUnpaidPayments,
            overduePaymentsAmount = overduePaymentsAmount,
            overdueCount = overdueBills.size,
            expectedRemainingBalance = expectedRemainingBalance,
            incomes = incomeItems,
            bills = billItems
        )
    }

    // --- Payment Status & Transaction Integration ---

    suspend fun markPaymentAsPaid(
        occurrenceId: Long,
        recurringPaymentId: Long,
        dueDate: AppDate,
        amount: Double,
        paidDate: AppDate = AppDate.today(),
        paymentMethod: String = "Bank Account"
    ) = withContext(Dispatchers.IO) {
        val payment = recurringPaymentDao.getRecurringPaymentByIdOnce(recurringPaymentId)
        val paymentName = payment?.name ?: "Recurring Payment"
        val category = payment?.category ?: PaymentCategory.OTHER.name

        var targetOccurrenceId = occurrenceId
        if (targetOccurrenceId == 0L) {
            val existing = occurrenceDao.getPaymentOccurrence(recurringPaymentId, dueDate.isoString)
            if (existing != null) {
                targetOccurrenceId = existing.id
            } else {
                targetOccurrenceId = occurrenceDao.insertPaymentOccurrence(
                    PaymentOccurrenceEntity(
                        recurringPaymentId = recurringPaymentId,
                        dueDate = dueDate.isoString,
                        amount = amount,
                        status = PaymentStatus.PAID.name,
                        paidDate = paidDate.isoString
                    )
                )
            }
        }

        // Prevent duplicate transaction: check if transaction already exists
        val existingTx = occurrenceDao.getPaymentOccurrenceById(targetOccurrenceId)?.transactionId?.let {
            transactionDao.getTransactionById(it)
        }

        val txId = existingTx?.id ?: transactionDao.insertTransaction(
            TransactionEntity(
                title = paymentName,
                amount = amount,
                type = TransactionType.EXPENSE.name,
                category = category,
                date = paidDate.isoString,
                paymentMethod = paymentMethod,
                recurringPaymentId = recurringPaymentId,
                paymentOccurrenceId = targetOccurrenceId
            )
        )

        occurrenceDao.updatePaymentOccurrence(
            PaymentOccurrenceEntity(
                id = targetOccurrenceId,
                recurringPaymentId = recurringPaymentId,
                dueDate = dueDate.isoString,
                amount = amount,
                status = PaymentStatus.PAID.name,
                paidDate = paidDate.isoString,
                transactionId = txId
            )
        )
    }

    suspend fun markPaymentAsUnpaid(
        occurrenceId: Long,
        recurringPaymentId: Long,
        dueDate: AppDate,
        amount: Double
    ) = withContext(Dispatchers.IO) {
        if (occurrenceId != 0L) {
            val occ = occurrenceDao.getPaymentOccurrenceById(occurrenceId)
            if (occ?.transactionId != null) {
                transactionDao.deleteTransactionById(occ.transactionId)
            }
            val today = AppDate.today()
            val newStatus = if (dueDate < today) PaymentStatus.OVERDUE.name else PaymentStatus.UNPAID.name
            occurrenceDao.updatePaymentOccurrence(
                occ!!.copy(
                    status = newStatus,
                    paidDate = null,
                    transactionId = null
                )
            )
        }
    }

    suspend fun skipPaymentOccurrence(
        occurrenceId: Long,
        recurringPaymentId: Long,
        dueDate: AppDate,
        amount: Double
    ) = withContext(Dispatchers.IO) {
        var targetId = occurrenceId
        if (targetId == 0L) {
            val existing = occurrenceDao.getPaymentOccurrence(recurringPaymentId, dueDate.isoString)
            if (existing != null) {
                targetId = existing.id
            } else {
                targetId = occurrenceDao.insertPaymentOccurrence(
                    PaymentOccurrenceEntity(
                        recurringPaymentId = recurringPaymentId,
                        dueDate = dueDate.isoString,
                        amount = amount,
                        status = PaymentStatus.SKIPPED.name
                    )
                )
            }
        }
        val occ = occurrenceDao.getPaymentOccurrenceById(targetId)
        if (occ?.transactionId != null) {
            transactionDao.deleteTransactionById(occ.transactionId)
        }
        if (occ != null) {
            occurrenceDao.updatePaymentOccurrence(
                occ.copy(
                    status = PaymentStatus.SKIPPED.name,
                    paidDate = null,
                    transactionId = null
                )
            )
        }
    }

    suspend fun updateOccurrenceAmount(
        occurrenceId: Long,
        recurringPaymentId: Long,
        dueDate: AppDate,
        newAmount: Double
    ) = withContext(Dispatchers.IO) {
        if (occurrenceId != 0L) {
            val occ = occurrenceDao.getPaymentOccurrenceById(occurrenceId)
            if (occ != null) {
                occurrenceDao.updatePaymentOccurrence(occ.copy(amount = newAmount))
                if (occ.transactionId != null) {
                    val tx = transactionDao.getTransactionById(occ.transactionId)
                    if (tx != null) {
                        transactionDao.updateTransaction(tx.copy(amount = newAmount))
                    }
                }
            }
        } else {
            occurrenceDao.insertPaymentOccurrence(
                PaymentOccurrenceEntity(
                    recurringPaymentId = recurringPaymentId,
                    dueDate = dueDate.isoString,
                    amount = newAmount,
                    status = PaymentStatus.UNPAID.name
                )
            )
        }
    }

    // --- Income Status & Transaction Integration ---

    suspend fun markIncomeAsReceived(
        occurrenceId: Long,
        incomeSourceId: Long,
        occurrenceDate: AppDate,
        amount: Double,
        receivedDate: AppDate = AppDate.today(),
        paymentMethod: String = "Bank Account"
    ) = withContext(Dispatchers.IO) {
        val incomeSource = incomeDao.getIncomeSourceByIdOnce(incomeSourceId)
        val name = incomeSource?.name ?: "Income"
        val category = incomeSource?.category ?: IncomeCategory.OTHER.name

        var targetId = occurrenceId
        if (targetId == 0L) {
            val existing = occurrenceDao.getIncomeOccurrence(incomeSourceId, occurrenceDate.isoString)
            if (existing != null) {
                targetId = existing.id
            } else {
                targetId = occurrenceDao.insertIncomeOccurrence(
                    IncomeOccurrenceEntity(
                        incomeSourceId = incomeSourceId,
                        occurrenceDate = occurrenceDate.isoString,
                        expectedAmount = amount,
                        receivedAmount = amount,
                        status = IncomeStatus.RECEIVED.name,
                        receivedDate = receivedDate.isoString
                    )
                )
            }
        }

        val existingTx = occurrenceDao.getIncomeOccurrenceById(targetId)?.transactionId?.let {
            transactionDao.getTransactionById(it)
        }

        val txId = existingTx?.id ?: transactionDao.insertTransaction(
            TransactionEntity(
                title = name,
                amount = amount,
                type = TransactionType.INCOME.name,
                category = category,
                date = receivedDate.isoString,
                paymentMethod = paymentMethod,
                incomeOccurrenceId = targetId
            )
        )

        val occ = occurrenceDao.getIncomeOccurrenceById(targetId)
        if (occ != null) {
            occurrenceDao.updateIncomeOccurrence(
                occ.copy(
                    receivedAmount = amount,
                    status = IncomeStatus.RECEIVED.name,
                    receivedDate = receivedDate.isoString,
                    transactionId = txId
                )
            )
        }
    }

    suspend fun markIncomeAsExpected(
        occurrenceId: Long
    ) = withContext(Dispatchers.IO) {
        if (occurrenceId != 0L) {
            val occ = occurrenceDao.getIncomeOccurrenceById(occurrenceId)
            if (occ?.transactionId != null) {
                transactionDao.deleteTransactionById(occ.transactionId)
            }
            if (occ != null) {
                occurrenceDao.updateIncomeOccurrence(
                    occ.copy(
                        receivedAmount = null,
                        status = IncomeStatus.EXPECTED.name,
                        receivedDate = null,
                        transactionId = null
                    )
                )
            }
        }
    }

    suspend fun skipIncomeOccurrence(
        occurrenceId: Long,
        incomeSourceId: Long,
        occurrenceDate: AppDate,
        expectedAmount: Double
    ) = withContext(Dispatchers.IO) {
        var targetId = occurrenceId
        if (targetId == 0L) {
            targetId = occurrenceDao.insertIncomeOccurrence(
                IncomeOccurrenceEntity(
                    incomeSourceId = incomeSourceId,
                    occurrenceDate = occurrenceDate.isoString,
                    expectedAmount = expectedAmount,
                    status = IncomeStatus.SKIPPED.name
                )
            )
        } else {
            val occ = occurrenceDao.getIncomeOccurrenceById(targetId)
            if (occ?.transactionId != null) {
                transactionDao.deleteTransactionById(occ.transactionId)
            }
            if (occ != null) {
                occurrenceDao.updateIncomeOccurrence(
                    occ.copy(
                        status = IncomeStatus.SKIPPED.name,
                        receivedAmount = null,
                        receivedDate = null,
                        transactionId = null
                    )
                )
            }
        }
    }

    // --- Multi-Month Forecast ---

    suspend fun calculateMultiMonthForecast(
        startMonth: YearMonth,
        numberOfMonths: Int = 12
    ): List<MonthForecast> = withContext(Dispatchers.IO) {
        val incomeSources = database.incomeDao().getAllIncomeSourcesOnce()
        val recurringPayments = database.recurringPaymentDao().getAllRecurringPaymentsOnce()

        val forecastList = mutableListOf<MonthForecast>()
        var runningCumulative = 0.0

        for (i in 0 until numberOfMonths) {
            val targetYM = startMonth.plusMonths(i)
            val storedIncomes = occurrenceDao.getIncomeOccurrencesForMonthOnce(targetYM.formatted)
            val storedPayments = occurrenceDao.getPaymentOccurrencesForMonthOnce(targetYM.formatted)

            val summary = calculateSummary(
                yearMonth = targetYM,
                incomeSources = incomeSources,
                storedIncomeOccurrences = storedIncomes,
                recurringPayments = recurringPayments,
                storedPaymentOccurrences = storedPayments
            )

            val projectedNet = summary.expectedIncome - summary.totalPlannedPayments
            runningCumulative += projectedNet

            val nonMonthlyCommitments = summary.bills.filter {
                it.frequency != RecurrenceFrequency.MONTHLY && it.status != PaymentStatus.SKIPPED
            }.map { "${it.name} (${it.frequency.displayName}): ₹${it.amount.toLong()}" }

            forecastList.add(
                MonthForecast(
                    targetMonth = targetYM,
                    expectedIncome = summary.expectedIncome,
                    scheduledPayments = summary.totalPlannedPayments,
                    projectedNet = projectedNet,
                    cumulativeBalance = runningCumulative,
                    isNegative = projectedNet < 0,
                    nonMonthlyCommitments = nonMonthlyCommitments
                )
            )
        }

        forecastList
    }

    // --- Reminders ---

    fun getRemindersForPayment(paymentId: Long): Flow<List<ReminderEntity>> = reminderDao.getRemindersForPayment(paymentId)

    suspend fun getRemindersForPaymentOnce(paymentId: Long): List<ReminderEntity> = withContext(Dispatchers.IO) {
        reminderDao.getRemindersForPaymentOnce(paymentId)
    }

    suspend fun saveRemindersForPayment(paymentId: Long, daysBeforeList: List<Int>) = withContext(Dispatchers.IO) {
        reminderDao.deleteRemindersForPayment(paymentId)
        val entities = daysBeforeList.distinct().map { days ->
            ReminderEntity(
                recurringPaymentId = paymentId,
                daysBefore = days,
                reminderHour = 9,
                reminderMinute = 0,
                isEnabled = true
            )
        }
        reminderDao.insertReminders(entities)
    }

    // --- Transactions ---

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsForMonth(yearMonth: YearMonth): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForMonth(yearMonth.formatted)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            transactionDao.deleteTransaction(tx)
            // If linked to payment occurrence, revert occurrence
            tx.paymentOccurrenceId?.let { occId ->
                val occ = occurrenceDao.getPaymentOccurrenceById(occId)
                if (occ != null) {
                    val dueDate = runCatching { AppDate.parse(occ.dueDate) }.getOrDefault(AppDate.today())
                    val newStatus = if (dueDate < AppDate.today()) PaymentStatus.OVERDUE.name else PaymentStatus.UNPAID.name
                    occurrenceDao.updatePaymentOccurrence(
                        occ.copy(status = newStatus, paidDate = null, transactionId = null)
                    )
                }
            }
            // If linked to income occurrence, revert occurrence
            tx.incomeOccurrenceId?.let { occId ->
                val occ = occurrenceDao.getIncomeOccurrenceById(occId)
                if (occ != null) {
                    occurrenceDao.updateIncomeOccurrence(
                        occ.copy(status = IncomeStatus.EXPECTED.name, receivedAmount = null, receivedDate = null, transactionId = null)
                    )
                }
            }
        }
    }

    // --- Budget Caps & Alerts ---

    fun getAllBudgetCaps(): Flow<List<BudgetCapEntity>> = budgetCapDao.getAllBudgetCaps()

    suspend fun getBudgetCapForCategory(category: PaymentCategory): BudgetCapEntity? = withContext(Dispatchers.IO) {
        budgetCapDao.getBudgetCapForCategory(category.name)
    }

    suspend fun saveBudgetCap(
        category: PaymentCategory,
        monthlyLimit: Double,
        alertThresholdPercent: Double = 80.0
    ): Long = withContext(Dispatchers.IO) {
        val existing = budgetCapDao.getBudgetCapForCategory(category.name)
        val entity = BudgetCapEntity(
            id = existing?.id ?: 0,
            category = category.name,
            monthlyLimit = monthlyLimit,
            alertThresholdPercent = alertThresholdPercent,
            isActive = true
        )
        budgetCapDao.upsertBudgetCap(entity)
    }

    suspend fun deleteBudgetCap(id: Long) = withContext(Dispatchers.IO) {
        budgetCapDao.deleteBudgetCapById(id)
    }

    suspend fun deleteBudgetCapByCategory(category: PaymentCategory) = withContext(Dispatchers.IO) {
        budgetCapDao.deleteBudgetCapByCategory(category.name)
    }

    fun getMonthlyBudgetOverview(yearMonth: YearMonth): Flow<MonthlyBudgetOverview> {
        return combine(
            budgetCapDao.getAllBudgetCaps(),
            getMonthlyFinancialSummary(yearMonth),
            transactionDao.getTransactionsForMonth(yearMonth.formatted)
        ) { caps, summary, transactions ->
            calculateBudgetOverview(yearMonth, caps, summary, transactions)
        }
    }

    fun calculateBudgetOverview(
        yearMonth: YearMonth,
        budgetCaps: List<BudgetCapEntity>,
        summary: MonthlyFinancialSummary,
        transactions: List<TransactionEntity>
    ): MonthlyBudgetOverview {
        val statuses = mutableListOf<CategoryBudgetStatus>()
        val alerts = mutableListOf<BudgetAlert>()

        val activeCaps = budgetCaps.filter { it.isActive }

        for (cap in activeCaps) {
            val category = runCatching { PaymentCategory.valueOf(cap.category) }.getOrNull() ?: continue
            val paidBills = summary.bills.filter { it.category == category && it.status == PaymentStatus.PAID }.sumOf { it.amount }
            val plannedBills = summary.bills.filter { it.category == category && it.status != PaymentStatus.SKIPPED }.sumOf { it.amount }
            val manualExpenses = transactions.filter {
                it.type == TransactionType.EXPENSE.name &&
                it.category == category.name &&
                it.recurringPaymentId == null
            }.sumOf { it.amount }

            val totalSpent = paidBills + manualExpenses
            val totalPlanned = maxOf(plannedBills + manualExpenses, totalSpent)
            val limit = cap.monthlyLimit
            val percent = if (limit > 0.0) (totalSpent / limit) * 100.0 else 0.0
            val threshold = cap.alertThresholdPercent
            val isExceeded = percent > 100.0
            val isApproaching = percent >= threshold && !isExceeded
            val remaining = limit - totalSpent

            val statusItem = CategoryBudgetStatus(
                id = cap.id,
                category = category,
                monthlyLimit = limit,
                alertThresholdPercent = threshold,
                spentAmount = totalSpent,
                plannedAmount = totalPlanned,
                remainingAllowance = remaining,
                percentageSpent = percent,
                isApproaching = isApproaching,
                isExceeded = isExceeded,
                isConfigured = true
            )
            statuses.add(statusItem)

            if (isApproaching || isExceeded) {
                alerts.add(
                    BudgetAlert(
                        category = category,
                        monthlyLimit = limit,
                        spentAmount = totalSpent,
                        percentageSpent = percent,
                        alertThresholdPercent = threshold,
                        isExceeded = isExceeded
                    )
                )
            }
        }

        // Sort: exceeded first, then approaching, then by highest percentage spent
        statuses.sortWith(compareByDescending<CategoryBudgetStatus> { it.isExceeded }
            .thenByDescending { it.isApproaching }
            .thenByDescending { it.percentageSpent })

        val totalCap = statuses.sumOf { it.monthlyLimit }
        val totalSpentBudgeted = statuses.sumOf { it.spentAmount }
        val totalPlannedBudgeted = statuses.sumOf { it.plannedAmount }
        val overallPercent = if (totalCap > 0) (totalSpentBudgeted / totalCap) * 100.0 else 0.0

        return MonthlyBudgetOverview(
            targetMonth = yearMonth,
            totalBudgetCap = totalCap,
            totalSpentInBudgetedCategories = totalSpentBudgeted,
            totalPlannedInBudgetedCategories = totalPlannedBudgeted,
            overallPercentageSpent = overallPercent,
            statuses = statuses,
            alerts = alerts
        )
    }

    suspend fun checkBudgetAlertOnPayment(
        category: PaymentCategory,
        yearMonth: YearMonth
    ): BudgetAlert? = withContext(Dispatchers.IO) {
        val cap = budgetCapDao.getBudgetCapForCategory(category.name) ?: return@withContext null
        if (!cap.isActive) return@withContext null

        val summary = getMonthlyFinancialSummaryOnce(yearMonth)
        val transactions = transactionDao.getTransactionsForMonthOnce(yearMonth.formatted)
        val overview = calculateBudgetOverview(yearMonth, listOf(cap), summary, transactions)
        overview.alerts.firstOrNull { it.category == category }
    }

    private suspend fun getMonthlyFinancialSummaryOnce(yearMonth: YearMonth): MonthlyFinancialSummary {
        val prefix = yearMonth.formatted
        val incomeSources = incomeDao.getAllIncomeSourcesOnce()
        val incomeOccurrences = occurrenceDao.getIncomeOccurrencesForMonthOnce(prefix)
        val recurringPayments = recurringPaymentDao.getAllRecurringPaymentsOnce()
        val paymentOccurrences = occurrenceDao.getPaymentOccurrencesForMonthOnce(prefix)
        return calculateSummary(yearMonth, incomeSources, incomeOccurrences, recurringPayments, paymentOccurrences)
    }
}


