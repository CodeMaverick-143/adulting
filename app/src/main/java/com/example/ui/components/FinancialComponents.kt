package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncomeCategory
import com.example.data.model.IncomeStatus
import com.example.data.model.PaymentCategory
import com.example.data.model.PaymentStatus
import com.example.domain.AppDate
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    return "₹" + formatter.format(amount)
}

fun formatRelativeDue(dueDate: AppDate, today: AppDate = AppDate.today()): String {
    val diff = dueDate.daysBetween(today)
    return when {
        diff < -1 -> "Overdue by ${-diff} days"
        diff == -1L -> "Overdue yesterday"
        diff == 0L -> "Due today"
        diff == 1L -> "Due tomorrow"
        diff in 2..6 -> "Due in $diff days"
        else -> "Due on ${dueDate.day} ${getShortMonthName(dueDate.month)}"
    }
}

fun getShortMonthName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}

fun getCategoryIcon(category: PaymentCategory): ImageVector {
    return when (category) {
        PaymentCategory.HOUSING -> Icons.Default.Home
        PaymentCategory.UTILITIES -> Icons.Default.ElectricBolt
        PaymentCategory.SUBSCRIPTIONS -> Icons.Default.Subscriptions
        PaymentCategory.RECHARGE -> Icons.Default.PhoneAndroid
        PaymentCategory.INSURANCE -> Icons.Default.Security
        PaymentCategory.LOANS -> Icons.Default.AccountBalance
        PaymentCategory.EDUCATION -> Icons.Default.School
        PaymentCategory.HEALTH -> Icons.Default.MedicalServices
        PaymentCategory.OTHER -> Icons.Default.Receipt
    }
}

fun getIncomeCategoryIcon(category: IncomeCategory): ImageVector {
    return when (category) {
        IncomeCategory.SALARY -> Icons.Default.Work
        IncomeCategory.FREELANCING -> Icons.Default.LocalAtm
        IncomeCategory.BUSINESS -> Icons.Default.BusinessCenter
        IncomeCategory.ALLOWANCE -> Icons.Default.AccountBalance
        IncomeCategory.INVESTMENT -> Icons.Default.AccountBalance
        IncomeCategory.OTHER -> Icons.Default.LocalAtm
    }
}

@Composable
fun CategoryIconBox(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val (bgColor, textColor, icon) = when (status) {
        PaymentStatus.PAID -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), Icons.Default.CheckCircle)
        PaymentStatus.OVERDUE -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.Default.Warning)
        PaymentStatus.UNPAID -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Icons.Default.HourglassEmpty)
        PaymentStatus.SKIPPED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.HourglassEmpty)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.displayName,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun IncomeStatusBadge(status: IncomeStatus) {
    val (bgColor, textColor) = when (status) {
        IncomeStatus.RECEIVED -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        IncomeStatus.EXPECTED -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3))
        IncomeStatus.SKIPPED -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(confirmText, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
