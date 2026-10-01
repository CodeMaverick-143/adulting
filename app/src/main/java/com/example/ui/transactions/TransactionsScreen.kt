package com.example.ui.transactions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.AppDate
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.formatCurrency
import com.example.ui.planner.EmptyStateCard
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.viewmodel.FinancialPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: FinancialPlannerViewModel) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredList = when (selectedFilter) {
        null -> transactions
        TransactionType.INCOME -> transactions.filter { it.type == TransactionType.INCOME.name }
        TransactionType.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE.name }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                    text = "Transactions History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Actual recorded income receipts and bill payments",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${transactions.size})") }
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.EXPENSE,
                    onClick = { selectedFilter = TransactionType.EXPENSE },
                    label = { Text("Expenses") }
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.INCOME,
                    onClick = { selectedFilter = TransactionType.INCOME },
                    label = { Text("Income") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "No actual transactions recorded yet. Transactions are automatically created when you mark bills as paid or receive income.",
                            buttonText = "Log Transaction Manually",
                            onAction = { showAddSheet = true }
                        )
                    }
                } else {
                    items(filteredList, key = { it.id }) { tx ->
                        TransactionItemCard(
                            tx = tx,
                            onDelete = { transactionToDelete = tx }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            AddTransactionSheet(
                onSave = { title, amt, type, cat, date, method, notes ->
                    viewModel.addManualTransaction(title, amt, type, cat, date, method, notes)
                    showAddSheet = false
                },
                onDismiss = { showAddSheet = false }
            )
        }
    }

    transactionToDelete?.let { tx ->
        ConfirmationDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete '${tx.title}' (${formatCurrency(tx.amount)})? If linked to a recurring payment, its status will be reverted to unpaid.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteTransaction(tx.id)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }
}

@Composable
fun TransactionItemCard(
    tx: TransactionEntity,
    onDelete: () -> Unit
) {
    val isIncome = tx.type == TransactionType.INCOME.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${tx.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isIncome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (isIncome) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    text = "${tx.date} • ${tx.paymentMethod}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + formatCurrency(tx.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionSheet(
    onSave: (title: String, amount: Double, type: TransactionType, cat: String, date: AppDate, method: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("General") }
    var dateText by remember { mutableStateOf(AppDate.today().isoString) }
    var method by remember { mutableStateOf("Bank Account") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Log Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isIncome,
                onClick = { isIncome = false },
                label = { Text("Expense (-)") }
            )
            FilterChip(
                selected = isIncome,
                onClick = { isIncome = true },
                label = { Text("Income (+)") }
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Transaction Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() } },
            label = { Text("Amount (₹)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = method,
            onValueChange = { method = it },
            label = { Text("Payment Method") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }

        Button(
            onClick = {
                if (title.isBlank()) {
                    error = "Enter title"
                    return@Button
                }
                val amt = amountText.toDoubleOrNull()
                if (amt == null || amt <= 0) {
                    error = "Enter valid amount"
                    return@Button
                }
                val d = runCatching { AppDate.parse(dateText) }.getOrNull()
                if (d == null) {
                    error = "Invalid date (YYYY-MM-DD)"
                    return@Button
                }
                onSave(title, amt, if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, category, d, method, notes)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Transaction")
        }
    }
}
