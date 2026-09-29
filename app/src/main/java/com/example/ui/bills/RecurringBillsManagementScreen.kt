package com.example.ui.bills

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.RecurringPaymentEntity
import com.example.data.model.PaymentCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.domain.AppDate
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.formatCurrency
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.UpcomingRecurringPaymentsList
import com.example.ui.income.ChangeFutureAmountDialog
import com.example.ui.planner.EmptyStateCard
import com.example.ui.viewmodel.FinancialPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsManagementScreen(
    viewModel: FinancialPlannerViewModel,
    openAddDialogTrigger: Boolean = false,
    editBillIdTrigger: Long? = null,
    onTriggersConsumed: () -> Unit = {}
) {
    val bills by viewModel.recurringPayments.collectAsStateWithLifecycle()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<RecurringPaymentEntity?>(null) }
    var billToDelete by remember { mutableStateOf<RecurringPaymentEntity?>(null) }
    var billToChangeFutureAmount by remember { mutableStateOf<RecurringPaymentEntity?>(null) }
    var billToManageReminders by remember { mutableStateOf<RecurringPaymentEntity?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }

    if (openAddDialogTrigger) {
        billToEdit = null
        showAddEditSheet = true
        onTriggersConsumed()
    }

    if (editBillIdTrigger != null) {
        val target = bills.find { it.id == editBillIdTrigger }
        if (target != null) {
            billToEdit = target
            showAddEditSheet = true
        }
        onTriggersConsumed()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    billToEdit = null
                    showAddEditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_bill_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Bill")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Recurring Bills & Payments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Manage rent, subscriptions, utilities, and upcoming due dates",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Upcoming Due Dates", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_upcoming_payments")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("All Schedules (${bills.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_bill_schedules")
                )
            }

            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    UpcomingRecurringPaymentsList(
                        viewModel = viewModel,
                        onBillClick = { bill ->
                            val target = bills.find { it.id == bill.recurringPaymentId }
                            if (target != null) {
                                billToEdit = target
                                showAddEditSheet = true
                            }
                        },
                        onAddBillClick = {
                            billToEdit = null
                            showAddEditSheet = true
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (bills.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No recurring bills or subscriptions added yet.",
                                buttonText = "Add Your First Bill",
                                onAction = {
                                    billToEdit = null
                                    showAddEditSheet = true
                                }
                            )
                        }
                    } else {
                        items(bills, key = { it.id }) { bill ->
                            RecurringBillCard(
                                bill = bill,
                                onEdit = {
                                    billToEdit = bill
                                    showAddEditSheet = true
                                },
                                onToggleStatus = { viewModel.toggleRecurringPaymentStatus(bill) },
                                onChangeFutureAmount = { billToChangeFutureAmount = bill },
                                onManageReminders = { billToManageReminders = bill },
                                onDelete = { billToDelete = bill }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add / Edit Bill Sheet
    if (showAddEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddEditBillSheet(
                existing = billToEdit,
                onSave = { name, amount, isVar, cat, freq, start, end, dueDay, autoRenew, notes, reminders ->
                    viewModel.saveRecurringPayment(
                        id = billToEdit?.id ?: 0L,
                        name = name,
                        amount = amount,
                        isVariableAmount = isVar,
                        category = cat,
                        frequency = freq,
                        startDate = start,
                        endDate = end,
                        dueDay = dueDay,
                        isAutoRenewal = autoRenew,
                        notes = notes,
                        remindersDaysBefore = reminders
                    )
                    showAddEditSheet = false
                },
                onDismiss = { showAddEditSheet = false }
            )
        }
    }

    // Delete Confirmation
    billToDelete?.let { bill ->
        ConfirmationDialog(
            title = "Delete Bill",
            message = "Are you sure you want to delete ${bill.name}? Reminders will be cancelled.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteRecurringPayment(bill)
                billToDelete = null
            },
            onDismiss = { billToDelete = null }
        )
    }

    // Change Future Bill Amount
    billToChangeFutureAmount?.let { bill ->
        ChangeFutureAmountDialog(
            title = "Change Future Bill Amount",
            currentAmount = bill.amount,
            onConfirm = { effectiveDate, newAmt ->
                viewModel.changeFuturePaymentAmount(bill.id, effectiveDate, newAmt)
                billToChangeFutureAmount = null
            },
            onDismiss = { billToChangeFutureAmount = null }
        )
    }

    // Reminders Management Sheet
    billToManageReminders?.let { bill ->
        ModalBottomSheet(
            onDismissRequest = { billToManageReminders = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            BillRemindersConfigSheet(
                bill = bill,
                onSaveReminders = { daysList ->
                    viewModel.updateRemindersForPayment(bill, daysList)
                    billToManageReminders = null
                },
                onDismiss = { billToManageReminders = null }
            )
        }
    }
}

@Composable
fun RecurringBillCard(
    bill: RecurringPaymentEntity,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onChangeFutureAmount: () -> Unit,
    onManageReminders: () -> Unit,
    onDelete: () -> Unit
) {
    val category = runCatching { PaymentCategory.valueOf(bill.category) }.getOrDefault(PaymentCategory.OTHER)
    val freq = runCatching { RecurrenceFrequency.valueOf(bill.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
    val isActive = bill.status == ScheduleStatus.ACTIVE.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_bill_card_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CategoryIconBox(
                        icon = getCategoryIcon(category),
                        contentDescription = category.displayName
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bill.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (bill.isVariableAmount) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Variable", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text(
                            text = "${category.displayName} • ${freq.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(bill.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isActive) "Active" else "Paused",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Due Day: ${bill.dueDay} of each period • Auto-renew: ${if (bill.isAutoRenewal) "Yes" else "No"}" +
                        (if (bill.endDate != null) " • Ends: ${bill.endDate}" else ""),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (bill.newAmount != null && bill.effectiveAmountChangeDate != null) {
                Text(
                    text = "Changes to ${formatCurrency(bill.newAmount)} from ${bill.effectiveAmountChangeDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onManageReminders) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onChangeFutureAmount) {
                    Text("Change Future ₹", fontSize = 11.sp)
                }
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "Pause" else "Resume",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillSheet(
    existing: RecurringPaymentEntity?,
    onSave: (name: String, amount: Double, isVariable: Boolean, category: PaymentCategory, freq: RecurrenceFrequency, start: AppDate, end: AppDate?, dueDay: Int, autoRenew: Boolean, notes: String, reminders: List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toInt()?.toString() ?: "") }
    var isVariable by remember { mutableStateOf(existing?.isVariableAmount ?: false) }
    var selectedCategory by remember {
        mutableStateOf(existing?.category?.let { runCatching { PaymentCategory.valueOf(it) }.getOrNull() } ?: PaymentCategory.HOUSING)
    }
    var selectedFrequency by remember {
        mutableStateOf(existing?.frequency?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() } ?: RecurrenceFrequency.MONTHLY)
    }
    var startDateText by remember { mutableStateOf(existing?.startDate ?: AppDate.today().isoString) }
    var endDateText by remember { mutableStateOf(existing?.endDate ?: "") }
    var dueDayText by remember { mutableStateOf(existing?.dueDay?.toString() ?: "1") }
    var isAutoRenew by remember { mutableStateOf(existing?.isAutoRenewal ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    // Reminders selection: 0 = on due date, 1 = 1 day before, 3 = 3 days before
    var reminderOnDue by remember { mutableStateOf(true) }
    var reminder1DayBefore by remember { mutableStateOf(true) }
    var reminder3DaysBefore by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var freqExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (existing == null) "Add Recurring Bill" else "Edit Recurring Bill",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Bill Name (e.g. Rent, Netflix, Electricity)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bill_name")
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isVariable) "Approx Amount (₹)" else "Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_bill_amount")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isVariable, onCheckedChange = { isVariable = it })
                    Text("Variable", fontSize = 12.sp)
                }
            }
        }

        // Category Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    PaymentCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Frequency Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = freqExpanded,
                onExpandedChange = { freqExpanded = !freqExpanded }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = freqExpanded,
                    onDismissRequest = { freqExpanded = false }
                ) {
                    RecurrenceFrequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.displayName) },
                            onClick = {
                                selectedFrequency = freq
                                freqExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it.filter { c -> c.isDigit() } },
                    label = { Text("Due Day (1-31)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = endDateText,
                onValueChange = { endDateText = it },
                label = { Text("End Date (Optional YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Auto-Renewal Switch
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-Renewal Subscription", fontSize = 13.sp)
                Switch(checked = isAutoRenew, onCheckedChange = { isAutoRenew = it })
            }
        }

        // Reminders checkboxes
        item {
            Column {
                Text("Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminderOnDue, onCheckedChange = { reminderOnDue = it })
                    Text("On Due Date (9:00 AM)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder1DayBefore, onCheckedChange = { reminder1DayBefore = it })
                    Text("1 Day Before", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder3DaysBefore, onCheckedChange = { reminder3DaysBefore = it })
                    Text("3 Days Before", fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        errorMessage?.let { err ->
            item {
                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter bill name"
                        return@Button
                    }
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter valid amount"
                        return@Button
                    }
                    val start = runCatching { AppDate.parse(startDateText) }.getOrNull()
                    if (start == null) {
                        errorMessage = "Invalid start date format (YYYY-MM-DD)"
                        return@Button
                    }
                    val end = if (endDateText.isNotBlank()) {
                        val parsed = runCatching { AppDate.parse(endDateText) }.getOrNull()
                        if (parsed == null) {
                            errorMessage = "Invalid end date format (YYYY-MM-DD)"
                            return@Button
                        }
                        parsed
                    } else null

                    val day = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1

                    val reminders = mutableListOf<Int>()
                    if (reminderOnDue) reminders.add(0)
                    if (reminder1DayBefore) reminders.add(1)
                    if (reminder3DaysBefore) reminders.add(3)

                    onSave(name, amt, isVariable, selectedCategory, selectedFrequency, start, end, day, isAutoRenew, notes, reminders)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_bill_btn")
            ) {
                Text("Save Recurring Bill")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BillRemindersConfigSheet(
    bill: RecurringPaymentEntity,
    onSaveReminders: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var onDue by remember { mutableStateOf(true) }
    var oneDayBefore by remember { mutableStateOf(true) }
    var twoDaysBefore by remember { mutableStateOf(false) }
    var threeDaysBefore by remember { mutableStateOf(false) }
    var oneWeekBefore by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("Configure Reminders", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Receive push notifications before due date for ${bill.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = onDue, onCheckedChange = { onDue = it })
            Text("On Due Date (9:00 AM)", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = oneDayBefore, onCheckedChange = { oneDayBefore = it })
            Text("1 Day Before", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = twoDaysBefore, onCheckedChange = { twoDaysBefore = it })
            Text("2 Days Before", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = threeDaysBefore, onCheckedChange = { threeDaysBefore = it })
            Text("3 Days Before", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = oneWeekBefore, onCheckedChange = { oneWeekBefore = it })
            Text("1 Week Before", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val list = mutableListOf<Int>()
                if (onDue) list.add(0)
                if (oneDayBefore) list.add(1)
                if (twoDaysBefore) list.add(2)
                if (threeDaysBefore) list.add(3)
                if (oneWeekBefore) list.add(7)
                onSaveReminders(list)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Reminders")
        }
    }
}
