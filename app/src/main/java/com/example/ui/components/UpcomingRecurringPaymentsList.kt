package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BillOccurrenceItem
import com.example.data.model.PaymentStatus
import com.example.domain.AppDate
import com.example.ui.planner.EmptyStateCard
import com.example.ui.viewmodel.FinancialPlannerViewModel

enum class UpcomingDueUrgency(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val stripColor: Color,
    val icon: ImageVector
) {
    OVERDUE(
        label = "Overdue",
        containerColor = Color(0xFFFEE2E2),
        contentColor = Color(0xFF991B1B),
        stripColor = Color(0xFFDC2626),
        icon = Icons.Default.Warning
    ),
    DUE_TODAY(
        label = "Due Today",
        containerColor = Color(0xFFFEF3C7),
        contentColor = Color(0xFFB45309),
        stripColor = Color(0xFFF59E0B),
        icon = Icons.Default.HourglassTop
    ),
    DUE_TOMORROW(
        label = "Due Tomorrow",
        containerColor = Color(0xFFFEF9C3),
        contentColor = Color(0xFFA16207),
        stripColor = Color(0xFFEAB308),
        icon = Icons.Default.Schedule
    ),
    DUE_THIS_WEEK(
        label = "Due Soon",
        containerColor = Color(0xFFE0F2FE),
        contentColor = Color(0xFF0369A1),
        stripColor = Color(0xFF0284C7),
        icon = Icons.Default.CalendarToday
    ),
    DUE_LATER(
        label = "Upcoming",
        containerColor = Color(0xFFF1F5F9),
        contentColor = Color(0xFF475569),
        stripColor = Color(0xFF94A3B8),
        icon = Icons.Default.Event
    ),
    PAID(
        label = "Paid",
        containerColor = Color(0xFFD1FAE5),
        contentColor = Color(0xFF065F46),
        stripColor = Color(0xFF10B981),
        icon = Icons.Default.CheckCircle
    ),
    SKIPPED(
        label = "Skipped",
        containerColor = Color(0xFFF1F5F9),
        contentColor = Color(0xFF64748B),
        stripColor = Color(0xFFCBD5E1),
        icon = Icons.Default.Schedule
    )
}

/**
 * Calculates urgency of due date relative to today.
 */
fun getDueUrgency(dueDate: AppDate, status: PaymentStatus, today: AppDate = AppDate.today()): UpcomingDueUrgency {
    if (status == PaymentStatus.PAID) return UpcomingDueUrgency.PAID
    if (status == PaymentStatus.SKIPPED) return UpcomingDueUrgency.SKIPPED
    if (status == PaymentStatus.OVERDUE) return UpcomingDueUrgency.OVERDUE
    val diff = dueDate.daysBetween(today)
    return when {
        diff < 0 -> UpcomingDueUrgency.OVERDUE
        diff == 0L -> UpcomingDueUrgency.DUE_TODAY
        diff == 1L -> UpcomingDueUrgency.DUE_TOMORROW
        diff in 2..7 -> UpcomingDueUrgency.DUE_THIS_WEEK
        else -> UpcomingDueUrgency.DUE_LATER
    }
}

/**
 * Visual pill badge indicator for upcoming payment due dates.
 */
@Composable
fun DueDateIndicator(
    dueDate: AppDate,
    status: PaymentStatus,
    today: AppDate = AppDate.today(),
    modifier: Modifier = Modifier
) {
    val urgency = getDueUrgency(dueDate, status, today)
    val diff = dueDate.daysBetween(today)

    val labelText = when {
        status == PaymentStatus.PAID -> "Paid"
        status == PaymentStatus.SKIPPED -> "Skipped"
        urgency == UpcomingDueUrgency.OVERDUE -> if (diff < 0) "Overdue ${-diff}d" else "Overdue"
        urgency == UpcomingDueUrgency.DUE_TODAY -> "Due Today"
        urgency == UpcomingDueUrgency.DUE_TOMORROW -> "Due Tomorrow"
        urgency == UpcomingDueUrgency.DUE_THIS_WEEK -> "Due in ${diff}d"
        else -> "Due ${dueDate.day} ${getShortMonthName(dueDate.month)}"
    }

    Surface(
        color = urgency.containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = urgency.icon,
                contentDescription = null,
                tint = urgency.contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = labelText,
                color = urgency.contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class UpcomingPaymentFilter(val title: String) {
    UPCOMING("Upcoming"),
    DUE_SOON("Due in 7d"),
    OVERDUE("Overdue"),
    PAID("Paid"),
    ALL("All")
}

/**
 * An individual payment item card displaying bill details with due date indicators.
 */
@Composable
fun UpcomingRecurringPaymentCard(
    item: BillOccurrenceItem,
    onMarkPaid: (BillOccurrenceItem) -> Unit,
    onMarkUnpaid: (BillOccurrenceItem) -> Unit,
    onClick: (BillOccurrenceItem) -> Unit,
    modifier: Modifier = Modifier,
    today: AppDate = AppDate.today()
) {
    val urgency = getDueUrgency(item.dueDate, item.status, today)
    val isPaid = item.status == PaymentStatus.PAID

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(item) }
            .testTag("upcoming_payment_card_${item.recurringPaymentId}_${item.dueDate.isoString}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Color strip indicator on the left side
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(urgency.stripColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top row: Category icon, Bill Name, Amount & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconBox(
                        icon = getCategoryIcon(item.category),
                        contentDescription = item.category.displayName,
                        backgroundColor = if (isPaid) {
                            Color(0xFFE2E8F0)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        iconTint = if (isPaid) {
                            Color(0xFF64748B)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isPaid) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = item.category.displayName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.frequency.displayName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.isVariableAmount) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Variable",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(item.amount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isPaid) {
                                Color(0xFF059669)
                            } else if (urgency == UpcomingDueUrgency.OVERDUE) {
                                Color(0xFFDC2626)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom row: Due Date Indicator + Quick Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due Date Indicator with badge and calendar info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DueDateIndicator(dueDate = item.dueDate, status = item.status, today = today)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${item.dueDate.day} ${getShortMonthName(item.dueDate.month)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action Button
                    if (isPaid) {
                        OutlinedButton(
                            onClick = { onMarkUnpaid(item) },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("unpaid_btn_${item.recurringPaymentId}"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Paid",
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        ElevatedButton(
                            onClick = { onMarkPaid(item) },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("mark_paid_btn_${item.recurringPaymentId}"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (urgency == UpcomingDueUrgency.DUE_TODAY || urgency == UpcomingDueUrgency.OVERDUE) {
                                    Color(0xFF065F46)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pay",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable list component displaying upcoming recurring payments with due date indicators,
 * filter chips, and interactive actions.
 */
@Composable
fun UpcomingRecurringPaymentsList(
    bills: List<BillOccurrenceItem>,
    onMarkPaid: (BillOccurrenceItem) -> Unit,
    onMarkUnpaid: (BillOccurrenceItem) -> Unit,
    onBillClick: (BillOccurrenceItem) -> Unit,
    modifier: Modifier = Modifier,
    today: AppDate = AppDate.today(),
    onAddBillClick: (() -> Unit)? = null
) {
    var selectedFilter by remember { mutableStateOf(UpcomingPaymentFilter.UPCOMING) }

    // Sort bills: Overdue first, then upcoming by due date, then paid
    val sortedBills = remember(bills, today) {
        bills.sortedWith(
            compareBy<BillOccurrenceItem> {
                when {
                    it.status == PaymentStatus.OVERDUE || (it.status == PaymentStatus.UNPAID && it.dueDate < today) -> 0
                    it.status == PaymentStatus.UNPAID && it.dueDate == today -> 1
                    it.status == PaymentStatus.UNPAID -> 2
                    else -> 3
                }
            }.thenBy { it.dueDate }
        )
    }

    // Filter counts
    val overdueCount = remember(bills, today) {
        bills.count { it.status == PaymentStatus.OVERDUE || (it.status == PaymentStatus.UNPAID && it.dueDate < today) }
    }
    val dueSoonCount = remember(bills, today) {
        bills.count {
            it.status == PaymentStatus.UNPAID && it.dueDate.daysBetween(today) in 0..7
        }
    }
    val upcomingCount = remember(bills) {
        bills.count { it.status == PaymentStatus.UNPAID || it.status == PaymentStatus.OVERDUE }
    }

    // Filtered list
    val displayedBills = remember(sortedBills, selectedFilter, today) {
        when (selectedFilter) {
            UpcomingPaymentFilter.UPCOMING -> sortedBills.filter { it.status != PaymentStatus.PAID && it.status != PaymentStatus.SKIPPED }
            UpcomingPaymentFilter.DUE_SOON -> sortedBills.filter {
                it.status == PaymentStatus.UNPAID && it.dueDate.daysBetween(today) in 0..7
            }
            UpcomingPaymentFilter.OVERDUE -> sortedBills.filter {
                it.status == PaymentStatus.OVERDUE || (it.status == PaymentStatus.UNPAID && it.dueDate < today)
            }
            UpcomingPaymentFilter.PAID -> sortedBills.filter { it.status == PaymentStatus.PAID }
            UpcomingPaymentFilter.ALL -> sortedBills
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Quick Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(UpcomingPaymentFilter.entries) { filter ->
                val badgeText = when (filter) {
                    UpcomingPaymentFilter.UPCOMING -> if (upcomingCount > 0) "$upcomingCount" else null
                    UpcomingPaymentFilter.DUE_SOON -> if (dueSoonCount > 0) "$dueSoonCount" else null
                    UpcomingPaymentFilter.OVERDUE -> if (overdueCount > 0) "$overdueCount" else null
                    else -> null
                }

                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(filter.title)
                            if (badgeText != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (filter == UpcomingPaymentFilter.OVERDUE) Color(0xFFEF4444)
                                            else MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (filter == UpcomingPaymentFilter.OVERDUE && overdueCount > 0) {
                            Color(0xFFFEE2E2)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        selectedLabelColor = if (filter == UpcomingPaymentFilter.OVERDUE && overdueCount > 0) {
                            Color(0xFF991B1B)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display Items or Empty State
        if (displayedBills.isEmpty()) {
            val emptyMsg = when (selectedFilter) {
                UpcomingPaymentFilter.OVERDUE -> "Great news! You have no overdue payments."
                UpcomingPaymentFilter.DUE_SOON -> "No bills due within the next 7 days."
                UpcomingPaymentFilter.PAID -> "No payments marked as paid yet for this month."
                UpcomingPaymentFilter.UPCOMING -> "All bills paid! No upcoming payments left."
                UpcomingPaymentFilter.ALL -> "No recurring payments recorded for this period."
            }

            EmptyStateCard(
                message = emptyMsg,
                buttonText = if (onAddBillClick != null) "Add Recurring Bill" else null,
                onAction = onAddBillClick ?: {}
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upcoming_payments_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(
                    items = displayedBills,
                    key = { "${it.recurringPaymentId}_${it.dueDate.isoString}" }
                ) { item ->
                    UpcomingRecurringPaymentCard(
                        item = item,
                        onMarkPaid = onMarkPaid,
                        onMarkUnpaid = onMarkUnpaid,
                        onClick = onBillClick,
                        today = today
                    )
                }
            }
        }
    }
}

/**
 * ViewModel-connected overload that observes recurring payments directly from the database
 * via FinancialPlannerViewModel.
 */
@Composable
fun UpcomingRecurringPaymentsList(
    viewModel: FinancialPlannerViewModel,
    onBillClick: (BillOccurrenceItem) -> Unit = {},
    onAddBillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val today = remember { AppDate.today() }

    UpcomingRecurringPaymentsList(
        bills = summary.bills,
        onMarkPaid = { bill -> viewModel.markPaymentAsPaid(bill) },
        onMarkUnpaid = { bill -> viewModel.markPaymentAsUnpaid(bill) },
        onBillClick = onBillClick,
        modifier = modifier,
        today = today,
        onAddBillClick = onAddBillClick
    )
}
