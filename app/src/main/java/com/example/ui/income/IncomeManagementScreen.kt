package com.example.ui.income

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.local.IncomeSourceEntity
import com.example.data.model.IncomeCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.domain.AppDate
import com.example.domain.RecurrenceCalculator
import com.example.domain.YearMonth
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.formatCurrency
import com.example.ui.components.getIncomeCategoryIcon
import com.example.ui.planner.EmptyStateCard
import com.example.ui.viewmodel.FinancialPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeManagementScreen(
    viewModel: FinancialPlannerViewModel,
    openAddDialogTrigger: Boolean = false,
    onAddDialogClosed: () -> Unit = {}
) {
    val incomeSources by viewModel.incomeSources.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sources, 1 = Timeline

    var showAddEditSheet by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<IncomeSourceEntity?>(null) }
    var incomeToDelete by remember { mutableStateOf<IncomeSourceEntity?>(null) }
    var incomeToChangeFutureAmount by remember { mutableStateOf<IncomeSourceEntity?>(null) }

    if (openAddDialogTrigger) {
        showAddEditSheet = true
        incomeToEdit = null
        onAddDialogClosed()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    incomeToEdit = null
                    showAddEditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_income_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Income Source")
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
                    text = "Income Management",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Manage your salary, freelancing, and revenue timelines",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Income Sources (${incomeSources.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Income Timeline") }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (incomeSources.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No income sources configured yet.",
                                buttonText = "Add Your Salary or Income",
                                onAction = {
                                    incomeToEdit = null
                                    showAddEditSheet = true
                                }
                            )
                        }
                    } else {
                        items(incomeSources, key = { it.id }) { source ->
                            IncomeSourceCard(
                                source = source,
                                onEdit = {
                                    incomeToEdit = source
                                    showAddEditSheet = true
                                },
                                onToggleStatus = { viewModel.toggleIncomeStatus(source) },
                                onChangeFutureAmount = { incomeToChangeFutureAmount = source },
                                onDelete = { incomeToDelete = source }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            } else {
                // Income Timeline View
                IncomeTimelineView(incomeSources = incomeSources)
            }
        }
    }

    // Add / Edit Income BottomSheet
    if (showAddEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddEditIncomeSheet(
                existing = incomeToEdit,
                onSave = { name, amount, cat, freq, start, end, day, notes ->
                    viewModel.saveIncomeSource(
                        id = incomeToEdit?.id ?: 0L,
                        name = name,
                        amount = amount,
                        category = cat,
                        frequency = freq,
                        startDate = start,
                        endDate = end,
                        expectedPaymentDay = day,
                        notes = notes
                    )
                    showAddEditSheet = false
                },
                onDismiss = { showAddEditSheet = false }
            )
        }
    }

    // Delete Confirmation
    incomeToDelete?.let { source ->
        ConfirmationDialog(
            title = "Delete Income Source",
            message = "Are you sure you want to delete ${source.name}? All historical projected occurrences will be removed.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteIncomeSource(source)
                incomeToDelete = null
            },
            onDismiss = { incomeToDelete = null }
        )
    }

    // Change Future Amount Dialog
    incomeToChangeFutureAmount?.let { source ->
        ChangeFutureAmountDialog(
            title = "Change Future Income Amount",
            currentAmount = source.amount,
            onConfirm = { effectiveDate, newAmt ->
                viewModel.changeFutureIncomeAmount(source.id, effectiveDate, newAmt)
                incomeToChangeFutureAmount = null
            },
            onDismiss = { incomeToChangeFutureAmount = null }
        )
    }
}

@Composable
fun IncomeSourceCard(
    source: IncomeSourceEntity,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onChangeFutureAmount: () -> Unit,
    onDelete: () -> Unit
) {
    val category = runCatching { IncomeCategory.valueOf(source.category) }.getOrDefault(IncomeCategory.OTHER)
    val freq = runCatching { RecurrenceFrequency.valueOf(source.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
    val isActive = source.status == ScheduleStatus.ACTIVE.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("income_source_card_${source.id}"),
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
                        icon = getIncomeCategoryIcon(category),
                        contentDescription = category.displayName,
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = source.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${category.displayName} • ${freq.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(source.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
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

            // Dates & details
            Text(
                text = "Started: ${source.startDate}" + (if (source.endDate != null) " • Ends: ${source.endDate}" else " • Ongoing"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (source.newAmount != null && source.effectiveAmountChangeDate != null) {
                Text(
                    text = "Changes to ${formatCurrency(source.newAmount)} from ${source.effectiveAmountChangeDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

@Composable
fun IncomeTimelineView(incomeSources: List<IncomeSourceEntity>) {
    val currentYM = YearMonth.current()
    val months = (0..11).map { currentYM.plusMonths(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(months, key = { it.formatted }) { ym ->
            var monthTotal = 0.0
            val breakdown = mutableListOf<String>()

            for (src in incomeSources) {
                val freq = runCatching { RecurrenceFrequency.valueOf(src.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
                val status = runCatching { ScheduleStatus.valueOf(src.status) }.getOrDefault(ScheduleStatus.ACTIVE)
                if (status != ScheduleStatus.ACTIVE) continue

                val startDate = runCatching { AppDate.parse(src.startDate) }.getOrDefault(AppDate.today())
                val endDate = src.endDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }
                val effectiveDate = src.effectiveAmountChangeDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }

                val dates = RecurrenceCalculator.generateOccurrencesForMonth(
                    frequency = freq,
                    startDate = startDate,
                    endDate = endDate,
                    preferredDayOfMonth = src.expectedPaymentDay,
                    targetMonth = ym
                )

                for (d in dates) {
                    val amt = RecurrenceCalculator.resolveEffectiveAmount(
                        baseAmount = src.amount,
                        occurrenceDate = d,
                        effectiveChangeDate = effectiveDate,
                        newAmount = src.newAmount
                    )
                    monthTotal += amt
                    breakdown.add("${src.name}: ${formatCurrency(amt)}")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ym.displayTitle(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (monthTotal > 0) formatCurrency(monthTotal) else "No expected salary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (monthTotal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (breakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = breakdown.joinToString(" • "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeSheet(
    existing: IncomeSourceEntity?,
    onSave: (name: String, amount: Double, category: IncomeCategory, freq: RecurrenceFrequency, start: AppDate, end: AppDate?, day: Int, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toInt()?.toString() ?: "") }
    var selectedCategory by remember {
        mutableStateOf(existing?.category?.let { runCatching { IncomeCategory.valueOf(it) }.getOrNull() } ?: IncomeCategory.SALARY)
    }
    var selectedFrequency by remember {
        mutableStateOf(existing?.frequency?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() } ?: RecurrenceFrequency.MONTHLY)
    }
    var startDateText by remember { mutableStateOf(existing?.startDate ?: AppDate.today().isoString) }
    var endDateText by remember { mutableStateOf(existing?.endDate ?: "") }
    var dayOfMonthText by remember { mutableStateOf(existing?.expectedPaymentDay?.toString() ?: "1") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var freqDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (existing == null) "Add Income Source" else "Edit Income Source",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Income Name (e.g. Monthly Salary, Freelance)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_income_name")
            )
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Amount (₹)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_income_amount")
            )
        }

        // Category Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    IncomeCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Frequency Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = freqDropdownExpanded,
                onExpandedChange = { freqDropdownExpanded = !freqDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = freqDropdownExpanded,
                    onDismissRequest = { freqDropdownExpanded = false }
                ) {
                    RecurrenceFrequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.displayName) },
                            onClick = {
                                selectedFrequency = freq
                                freqDropdownExpanded = false
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
                    value = dayOfMonthText,
                    onValueChange = { dayOfMonthText = it.filter { c -> c.isDigit() } },
                    label = { Text("Day of Month (1-31)") },
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
                        errorMessage = "Please enter income name"
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

                    val day = dayOfMonthText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                    onSave(name, amt, selectedCategory, selectedFrequency, start, end, day, notes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_income_btn")
            ) {
                Text("Save Income Source")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChangeFutureAmountDialog(
    title: String,
    currentAmount: Double,
    onConfirm: (effectiveDate: AppDate, newAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var newAmountText by remember { mutableStateOf("") }
    var effectiveDateText by remember { mutableStateOf(AppDate.today().isoString) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Amount: ${formatCurrency(currentAmount)}")
                Text("Specify a new amount effective from a future date. Historical records before this date will NOT be modified.")
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = { newAmountText = it.filter { c -> c.isDigit() } },
                    label = { Text("New Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = effectiveDateText,
                    onValueChange = { effectiveDateText = it },
                    label = { Text("Effective Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = newAmountText.toDoubleOrNull()
                if (amt == null || amt <= 0) {
                    error = "Enter valid amount"
                    return@Button
                }
                val date = runCatching { AppDate.parse(effectiveDateText) }.getOrNull()
                if (date == null) {
                    error = "Invalid date (YYYY-MM-DD)"
                    return@Button
                }
                onConfirm(date, amt)
            }) {
                Text("Update Effective Amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
