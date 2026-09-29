package com.example.ui.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BillOccurrenceItem
import com.example.data.model.BudgetAlert
import com.example.data.model.CategoryBudgetStatus
import com.example.data.model.IncomeOccurrenceItem
import com.example.data.model.IncomeStatus
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.PaymentCategory
import com.example.data.model.PaymentStatus
import com.example.domain.AppDate
import com.example.domain.YearMonth
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.IncomeStatusBadge
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatRelativeDue
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.getIncomeCategoryIcon
import com.example.ui.viewmodel.FinancialPlannerViewModel

enum class PlannerFilterTab(val title: String) {
    ALL("All"),
    BUDGET("Budgets"),
    UPCOMING("Upcoming"),
    UNPAID("Unpaid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    INCOME("Income")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyFinancialPlannerScreen(
    viewModel: FinancialPlannerViewModel,
    onNavigateToAddBill: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToEditBill: (Long) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val budgetOverview by viewModel.budgetOverview.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilter by remember { mutableStateOf(PlannerFilterTab.ALL) }

    // Detail BottomSheet state
    var selectedBillForDetail by remember { mutableStateOf<BillOccurrenceItem?>(null) }
    var selectedIncomeForDetail by remember { mutableStateOf<IncomeOccurrenceItem?>(null) }

    // Mark Paid Dialog State
    var billToMarkPaid by remember { mutableStateOf<BillOccurrenceItem?>(null) }
    var incomeToMarkReceived by remember { mutableStateOf<IncomeOccurrenceItem?>(null) }
    var variableAmountToEdit by remember { mutableStateOf<BillOccurrenceItem?>(null) }

    // Budget Cap Dialog State
    var isAddBudgetDialogOpen by remember { mutableStateOf(false) }
    var budgetCapToEdit by remember { mutableStateOf<CategoryBudgetStatus?>(null) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBill,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("planner_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill or Income")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Month Navigation Header
            MonthNavigationHeader(
                currentMonth = selectedMonth,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() },
                onToday = { viewModel.jumpToToday() }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly Summary Card
                item {
                    MonthlySummaryCard(summary = summary)
                }

                // Overdue Alert Banner if any
                if (summary.overdueCount > 0) {
                    item {
                        OverdueAlertBanner(
                            count = summary.overdueCount,
                            totalAmount = summary.overduePaymentsAmount,
                            onClick = { selectedFilter = PlannerFilterTab.OVERDUE }
                        )
                    }
                }

                // Budget Alert Banner if any
                if (budgetOverview.alerts.isNotEmpty()) {
                    item {
                        BudgetAlertBanner(
                            alerts = budgetOverview.alerts,
                            onManageBudgets = { selectedFilter = PlannerFilterTab.BUDGET }
                        )
                    }
                }

                // Filter Chips
                item {
                    FilterTabsRow(
                        selectedTab = selectedFilter,
                        onSelectTab = { selectedFilter = it },
                        overdueCount = summary.overdueCount,
                        budgetAlertCount = budgetOverview.alerts.size
                    )
                }

                // Category Budget Caps Section (Shown when ALL or BUDGET selected)
                if (selectedFilter == PlannerFilterTab.ALL || selectedFilter == PlannerFilterTab.BUDGET) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Category Budget Caps",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (budgetOverview.statuses.isNotEmpty()) {
                                        "${formatCurrency(budgetOverview.totalSpentInBudgetedCategories)} of ${formatCurrency(budgetOverview.totalBudgetCap)} spent (${budgetOverview.overallPercentageSpent.toInt()}%)"
                                    } else {
                                        "Set monthly spending limits per category for proactive alerts"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { isAddBudgetDialogOpen = true },
                                modifier = Modifier.testTag("btn_add_budget_cap")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Cap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (budgetOverview.statuses.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No monthly budget caps set. Set category limits to receive alerts when spending approaches these limits.",
                                buttonText = "Set First Budget Cap",
                                onAction = { isAddBudgetDialogOpen = true }
                            )
                        }
                    } else {
                        items(budgetOverview.statuses, key = { "budget_${it.category.name}" }) { status ->
                            CategoryBudgetCard(
                                status = status,
                                onEdit = { budgetCapToEdit = status }
                            )
                        }
                    }
                }

                // Income Section (Shown when ALL or INCOME selected)
                if (selectedFilter == PlannerFilterTab.ALL || selectedFilter == PlannerFilterTab.INCOME) {
                    item {
                        SectionHeader(
                            title = "Expected Income",
                            subtitle = "${formatCurrency(summary.receivedIncome)} of ${formatCurrency(summary.expectedIncome)} received"
                        )
                    }

                    if (summary.incomes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No income scheduled for this month",
                                buttonText = "Add Income Source",
                                onAction = onNavigateToAddIncome
                            )
                        }
                    } else {
                        items(summary.incomes, key = { "inc_${it.incomeSourceId}_${it.occurrenceDate.isoString}" }) { incomeItem ->
                            IncomeOccurrenceCard(
                                item = incomeItem,
                                onMarkReceived = { incomeToMarkReceived = incomeItem },
                                onClick = { selectedIncomeForDetail = incomeItem }
                            )
                        }
                    }
                }

                // Bills Section
                if (selectedFilter != PlannerFilterTab.INCOME && selectedFilter != PlannerFilterTab.BUDGET) {
                    val filteredBills = when (selectedFilter) {
                        PlannerFilterTab.ALL -> summary.bills
                        PlannerFilterTab.UPCOMING -> summary.bills.filter { it.status == PaymentStatus.UNPAID && it.dueDate >= AppDate.today() }
                        PlannerFilterTab.UNPAID -> summary.bills.filter { it.status == PaymentStatus.UNPAID || it.status == PaymentStatus.OVERDUE }
                        PlannerFilterTab.PAID -> summary.bills.filter { it.status == PaymentStatus.PAID }
                        PlannerFilterTab.OVERDUE -> summary.bills.filter { it.status == PaymentStatus.OVERDUE }
                        PlannerFilterTab.INCOME, PlannerFilterTab.BUDGET -> emptyList()
                    }

                    item {
                        SectionHeader(
                            title = when (selectedFilter) {
                                PlannerFilterTab.PAID -> "Paid Bills"
                                PlannerFilterTab.UNPAID -> "Unpaid & Overdue Bills"
                                PlannerFilterTab.OVERDUE -> "Overdue Bills"
                                PlannerFilterTab.UPCOMING -> "Upcoming Bills"
                                else -> "Planned Bills & Payments"
                            },
                            subtitle = "${formatCurrency(summary.totalPaidPayments)} of ${formatCurrency(summary.totalPlannedPayments)} paid"
                        )
                    }

                    if (filteredBills.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = when (selectedFilter) {
                                    PlannerFilterTab.OVERDUE -> "No overdue payments! You're all caught up."
                                    PlannerFilterTab.UNPAID -> "All bills for this month have been paid!"
                                    PlannerFilterTab.PAID -> "No bills paid yet this month."
                                    else -> "No recurring payments scheduled for this month."
                                },
                                buttonText = if (selectedFilter == PlannerFilterTab.ALL) "Add Recurring Bill" else null,
                                onAction = onNavigateToAddBill
                            )
                        }
                    } else {
                        items(filteredBills, key = { "bill_${it.recurringPaymentId}_${it.dueDate.isoString}" }) { billItem ->
                            BillOccurrenceCard(
                                item = billItem,
                                onMarkPaid = { billToMarkPaid = billItem },
                                onEditVariable = { variableAmountToEdit = billItem },
                                onClick = { selectedBillForDetail = billItem }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // --- Detail BottomSheet for Bill ---
    selectedBillForDetail?.let { billItem ->
        ModalBottomSheet(
            onDismissRequest = { selectedBillForDetail = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            BillDetailSheet(
                item = billItem,
                onMarkPaid = {
                    selectedBillForDetail = null
                    billToMarkPaid = billItem
                },
                onMarkUnpaid = {
                    viewModel.markPaymentAsUnpaid(billItem)
                    selectedBillForDetail = null
                },
                onSkip = {
                    viewModel.skipPaymentOccurrence(billItem)
                    selectedBillForDetail = null
                },
                onEditSchedule = {
                    selectedBillForDetail = null
                    onNavigateToEditBill(billItem.recurringPaymentId)
                },
                onEditVariableAmount = {
                    selectedBillForDetail = null
                    variableAmountToEdit = billItem
                },
                onDismiss = { selectedBillForDetail = null }
            )
        }
    }

    // --- Detail BottomSheet for Income ---
    selectedIncomeForDetail?.let { incItem ->
        ModalBottomSheet(
            onDismissRequest = { selectedIncomeForDetail = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            IncomeDetailSheet(
                item = incItem,
                onMarkReceived = {
                    selectedIncomeForDetail = null
                    incomeToMarkReceived = incItem
                },
                onRevertExpected = {
                    viewModel.markIncomeAsExpected(incItem)
                    selectedIncomeForDetail = null
                },
                onSkip = {
                    viewModel.skipIncomeOccurrence(incItem)
                    selectedIncomeForDetail = null
                },
                onDismiss = { selectedIncomeForDetail = null }
            )
        }
    }

    // --- Mark Bill Paid Dialog ---
    billToMarkPaid?.let { bill ->
        MarkBillPaidDialog(
            item = bill,
            onConfirm = { amount, method ->
                viewModel.markPaymentAsPaid(bill, amount = amount, paymentMethod = method)
                billToMarkPaid = null
            },
            onDismiss = { billToMarkPaid = null }
        )
    }

    // --- Mark Income Received Dialog ---
    incomeToMarkReceived?.let { income ->
        MarkIncomeReceivedDialog(
            item = income,
            onConfirm = { amount, method ->
                viewModel.markIncomeAsReceived(income, amount = amount, paymentMethod = method)
                incomeToMarkReceived = null
            },
            onDismiss = { incomeToMarkReceived = null }
        )
    }

    // --- Edit Variable Bill Amount Dialog ---
    variableAmountToEdit?.let { bill ->
        EditVariableAmountDialog(
            item = bill,
            onConfirm = { newAmount ->
                viewModel.updateVariableOccurrenceAmount(bill, newAmount)
                variableAmountToEdit = null
            },
            onDismiss = { variableAmountToEdit = null }
        )
    }

    // --- Set / Add Budget Cap Dialog ---
    if (isAddBudgetDialogOpen) {
        SetBudgetCapDialog(
            initialCategory = null,
            existingCap = null,
            configuredCategories = budgetOverview.statuses.map { it.category },
            onSave = { category, limit, threshold ->
                viewModel.saveBudgetCap(category, limit, threshold)
                isAddBudgetDialogOpen = false
            },
            onDismiss = { isAddBudgetDialogOpen = false }
        )
    }

    // --- Edit Budget Cap Dialog ---
    budgetCapToEdit?.let { capStatus ->
        SetBudgetCapDialog(
            initialCategory = capStatus.category,
            existingCap = capStatus,
            configuredCategories = budgetOverview.statuses.map { it.category },
            onSave = { category, limit, threshold ->
                viewModel.saveBudgetCap(category, limit, threshold)
                budgetCapToEdit = null
            },
            onDelete = {
                viewModel.deleteBudgetCap(capStatus.id, capStatus.category.displayName)
                budgetCapToEdit = null
            },
            onDismiss = { budgetCapToEdit = null }
        )
    }
}

@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier.testTag("planner_prev_month")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToday() }
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentMonth.displayTitle(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("planner_next_month")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(summary: MonthlyFinancialSummary) {
    val isPositive = summary.expectedRemainingBalance >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "EXPECTED REMAINING BALANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatCurrency(summary.expectedRemainingBalance),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = (if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer).copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 2-column breakdown
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expected Income",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(summary.expectedIncome),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Received: ${formatCurrency(summary.receivedIncome)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Planned Payments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(summary.totalPlannedPayments),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Remaining Unpaid: ${formatCurrency(summary.remainingUnpaidPayments)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun OverdueAlertBanner(count: Int, totalAmount: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("overdue_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count Overdue Payment${if (count > 1) "s" else ""}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                )
                Text(
                    text = "Total ${formatCurrency(totalAmount)} due past date",
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = "View",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun FilterTabsRow(
    selectedTab: PlannerFilterTab,
    onSelectTab: (PlannerFilterTab) -> Unit,
    overdueCount: Int,
    budgetAlertCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlannerFilterTab.entries.forEach { tab ->
            val label = when {
                tab == PlannerFilterTab.OVERDUE && overdueCount > 0 -> "Overdue ($overdueCount)"
                tab == PlannerFilterTab.BUDGET && budgetAlertCount > 0 -> "Budgets ($budgetAlertCount ⚠️)"
                else -> tab.title
            }
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (tab == PlannerFilterTab.BUDGET && budgetAlertCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                    selectedLabelColor = if (tab == PlannerFilterTab.BUDGET && budgetAlertCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun IncomeOccurrenceCard(
    item: IncomeOccurrenceItem,
    onMarkReceived: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("income_item_${item.incomeSourceId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBox(
                icon = getIncomeCategoryIcon(item.category),
                contentDescription = item.category.displayName,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.category.displayName} • ${item.occurrenceDate.day} ${item.occurrenceDate.yearMonth}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(item.receivedAmount ?: item.expectedAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (item.status == IncomeStatus.RECEIVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (item.status == IncomeStatus.EXPECTED) {
                    OutlinedButton(
                        onClick = onMarkReceived,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("mark_received_btn_${item.incomeSourceId}")
                    ) {
                        Text("Receive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IncomeStatusBadge(status = item.status)
                }
            }
        }
    }
}

@Composable
fun BillOccurrenceCard(
    item: BillOccurrenceItem,
    onMarkPaid: () -> Unit,
    onEditVariable: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("bill_item_${item.recurringPaymentId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBox(
                icon = getCategoryIcon(item.category),
                contentDescription = item.category.displayName
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isVariableAmount) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Var", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    text = formatRelativeDue(item.dueDate),
                    fontSize = 12.sp,
                    color = if (item.status == PaymentStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (item.status == PaymentStatus.OVERDUE) FontWeight.Bold else FontWeight.Normal
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(item.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (item.status == PaymentStatus.UNPAID || item.status == PaymentStatus.OVERDUE) {
                    Button(
                        onClick = onMarkPaid,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.status == PaymentStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("mark_paid_btn_${item.recurringPaymentId}")
                    ) {
                        Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    PaymentStatusBadge(status = item.status)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String, buttonText: String? = null, onAction: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            if (buttonText != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAction) {
                    Text(buttonText, fontSize = 12.sp)
                }
            }
        }
    }
}

// --- Detail & Action Sheets ---

@Composable
fun BillDetailSheet(
    item: BillOccurrenceItem,
    onMarkPaid: () -> Unit,
    onMarkUnpaid: () -> Unit,
    onSkip: () -> Unit,
    onEditSchedule: () -> Unit,
    onEditVariableAmount: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${item.category.displayName} • ${item.frequency.displayName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PaymentStatusBadge(status = item.status)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Amount Due", formatCurrency(item.amount))
                DetailRow("Due Date", item.dueDate.isoString)
                if (item.paidDate != null) {
                    DetailRow("Paid Date", item.paidDate.isoString)
                }
                DetailRow("Auto-Renewal", if (item.isAutoRenewal) "Yes" else "No")
                if (item.isVariableAmount) {
                    DetailRow("Amount Type", "Variable Amount")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        if (item.status == PaymentStatus.UNPAID || item.status == PaymentStatus.OVERDUE) {
            Button(
                onClick = onMarkPaid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_mark_paid_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Paid")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Skip This Occurrence")
            }
        } else if (item.status == PaymentStatus.PAID) {
            OutlinedButton(
                onClick = onMarkUnpaid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Revert to Unpaid")
            }
        }

        if (item.isVariableAmount && item.status != PaymentStatus.PAID) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEditVariableAmount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Variable Bill Amount")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onEditSchedule,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Recurring Bill Schedule")
        }
    }
}

@Composable
fun IncomeDetailSheet(
    item: IncomeOccurrenceItem,
    onMarkReceived: () -> Unit,
    onRevertExpected: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${item.category.displayName} • ${item.frequency.displayName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IncomeStatusBadge(status = item.status)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Expected Amount", formatCurrency(item.expectedAmount))
                if (item.receivedAmount != null) {
                    DetailRow("Received Amount", formatCurrency(item.receivedAmount))
                }
                DetailRow("Expected Date", item.occurrenceDate.isoString)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (item.status == IncomeStatus.EXPECTED) {
            Button(
                onClick = onMarkReceived,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Received")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip Income for This Month")
            }
        } else if (item.status == IncomeStatus.RECEIVED) {
            OutlinedButton(
                onClick = onRevertExpected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Revert to Expected")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --- Quick Dialogs ---

@Composable
fun MarkBillPaidDialog(
    item: BillOccurrenceItem,
    onConfirm: (amount: Double, paymentMethod: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toInt().toString()) }
    var paymentMethod by remember { mutableStateOf("Bank Account") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Paid: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record transaction and update bill status for ${item.dueDate.isoString}.")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount Paid (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Payment Method") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: item.amount
                    onConfirm(amt, paymentMethod)
                },
                modifier = Modifier.testTag("dialog_confirm_paid_btn")
            ) {
                Text("Confirm Paid")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MarkIncomeReceivedDialog(
    item: IncomeOccurrenceItem,
    onConfirm: (amount: Double, paymentMethod: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.expectedAmount.toInt().toString()) }
    var paymentMethod by remember { mutableStateOf("Bank Account") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Income: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record received income transaction for ${item.occurrenceDate.isoString}.")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount Received (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Payment Method / Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: item.expectedAmount
                    onConfirm(amt, paymentMethod)
                }
            ) {
                Text("Confirm Received")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditVariableAmountDialog(
    item: BillOccurrenceItem,
    onConfirm: (newAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Variable Bill Amount", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set the exact bill amount for ${item.name} for this month (${item.dueDate.isoString}).")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: item.amount
                onConfirm(amt)
            }) {
                Text("Save Amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BudgetAlertBanner(
    alerts: List<BudgetAlert>,
    onManageBudgets: () -> Unit
) {
    val hasExceeded = alerts.any { it.isExceeded }
    val exceededCount = alerts.count { it.isExceeded }
    val approachingCount = alerts.count { !it.isExceeded }

    val containerColor = if (hasExceeded) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF3CD)
    val contentColor = if (hasExceeded) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF856404)
    val iconColor = if (hasExceeded) MaterialTheme.colorScheme.error else Color(0xFFD97706)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManageBudgets() }
            .testTag("budget_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(contentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasExceeded) Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = "Budget Alert",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasExceeded) {
                        if (exceededCount == 1) "1 Category Exceeded Budget Cap" else "$exceededCount Categories Exceeded Budget Cap"
                    } else {
                        if (approachingCount == 1) "1 Category Approaching Budget Limit" else "$approachingCount Categories Approaching Budget Limit"
                    },
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 14.sp
                )
                val summaryText = alerts.joinToString(separator = " • ") { alert ->
                    if (alert.isExceeded) {
                        "${alert.category.displayName} (${alert.percentageSpent.toInt()}%)"
                    } else {
                        "${alert.category.displayName} (${alert.percentageSpent.toInt()}% of cap)"
                    }
                }
                Text(
                    text = summaryText,
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = contentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Manage",
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    status: CategoryBudgetStatus,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_cap_card_${status.category.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBox(
                    icon = getCategoryIcon(status.category),
                    contentDescription = status.category.displayName
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    when {
                        status.isExceeded -> {
                            val overBy = status.spentAmount - status.monthlyLimit
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EXCEEDED by ${formatCurrency(overBy)} (${status.percentageSpent.toInt()}%)",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        status.isApproaching -> {
                            Surface(
                                color = Color(0xFFFFF3CD),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "APPROACHING LIMIT (${status.percentageSpent.toInt()}%)",
                                        color = Color(0xFF856404),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ON TRACK (${status.percentageSpent.toInt()}%)",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("btn_edit_budget_${status.category.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit Budget Cap",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            val progressFraction = (status.percentageSpent / 100.0).coerceIn(0.0, 1.0).toFloat()
            val progressColor = when {
                status.isExceeded -> MaterialTheme.colorScheme.error
                status.isApproaching -> Color(0xFFD97706)
                else -> MaterialTheme.colorScheme.primary
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent this month",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(status.spentAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = progressColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Monthly Cap",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(status.monthlyLimit),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (status.isExceeded) "Over by" else "Remaining",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (status.isExceeded) {
                            "+${formatCurrency(status.spentAmount - status.monthlyLimit)}"
                        } else {
                            formatCurrency(status.remainingAllowance)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Alert threshold: ${status.alertThresholdPercent.toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Planned bills: ${formatCurrency(status.plannedAmount)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetCapDialog(
    initialCategory: PaymentCategory?,
    existingCap: CategoryBudgetStatus?,
    configuredCategories: List<PaymentCategory>,
    onSave: (category: PaymentCategory, limit: Double, alertThresholdPercent: Double) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEditing = existingCap != null
    val availableCategories = remember {
        if (isEditing) {
            listOf(initialCategory ?: PaymentCategory.OTHER)
        } else {
            val unconfigured = PaymentCategory.entries.filter { it !in configuredCategories }
            if (unconfigured.isNotEmpty()) unconfigured else PaymentCategory.entries
        }
    }

    var selectedCategory by remember {
        mutableStateOf(initialCategory ?: availableCategories.firstOrNull() ?: PaymentCategory.OTHER)
    }
    var limitText by remember {
        mutableStateOf(if (isEditing) existingCap?.monthlyLimit?.toLong()?.toString().orEmpty() else "")
    }
    var alertThreshold by remember {
        mutableStateOf(existingCap?.alertThresholdPercent?.toFloat() ?: 80f)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Budget Cap") },
            text = { Text("Are you sure you want to remove the monthly budget cap for ${selectedCategory.displayName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Budget Cap" else "Set Monthly Budget Cap",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isEditing && availableCategories.size > 1) {
                    Text("Select Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 12.sp) }
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBox(
                            icon = getCategoryIcon(selectedCategory),
                            contentDescription = selectedCategory.displayName
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCategory.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Budget Limit (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    placeholder = { Text("e.g. 5000") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_limit")
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert Threshold",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Surface(
                            color = Color(0xFFFFF3CD),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${alertThreshold.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Slider(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        valueRange = 50f..95f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Alerts appear as soon as your ${selectedCategory.displayName} expenses reach ${alertThreshold.toInt()}% of ₹${limitText.ifEmpty { "0" }}.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (limit > 0.0) {
                        onSave(selectedCategory, limit, alertThreshold.toDouble())
                    }
                },
                enabled = (limitText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("btn_save_budget_cap")
            ) {
                Text(if (isEditing) "Update Cap" else "Set Cap")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

