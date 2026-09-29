package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.ReminderEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduleStatus
import com.example.domain.AppDate
import com.example.domain.RecurrenceCalculator
import com.example.domain.YearMonth
import java.util.Calendar

object ReminderScheduler {

    const val CHANNEL_ID = "adulting_bill_reminders"
    const val CHANNEL_NAME = "Bill Due Reminders"
    const val ACTION_SHOW_REMINDER = "com.example.action.REMINDER_NOTIFICATION"
    const val ACTION_MARK_PAID = "com.example.action.MARK_BILL_PAID"
    const val ACTION_SNOOZE = "com.example.action.SNOOZE_BILL"

    const val EXTRA_BILL_ID = "extra_bill_id"
    const val EXTRA_BILL_NAME = "extra_bill_name"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_DUE_DATE = "extra_due_date"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming and due bills in Adulting"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun getRequestCode(billId: Long, daysBefore: Int): Int {
        return (billId * 100 + daysBefore).toInt()
    }

    /**
     * Calculates the next upcoming due date for an active recurring payment.
     */
    fun findNextDueDate(bill: RecurringPaymentEntity, afterDate: AppDate = AppDate.today()): AppDate? {
        val freq = runCatching { RecurrenceFrequency.valueOf(bill.frequency) }.getOrDefault(RecurrenceFrequency.MONTHLY)
        val startDate = runCatching { AppDate.parse(bill.startDate) }.getOrDefault(AppDate.today())
        val endDate = bill.endDate?.let { runCatching { AppDate.parse(it) }.getOrNull() }

        var currentYM = YearMonth(afterDate.year, afterDate.month)
        // Search current month and subsequent 12 months for the next occurrence >= afterDate
        for (i in 0..12) {
            val occurrences = RecurrenceCalculator.generateOccurrencesForMonth(
                frequency = freq,
                startDate = startDate,
                endDate = endDate,
                preferredDayOfMonth = bill.dueDay,
                targetMonth = currentYM
            )
            val next = occurrences.firstOrNull { it >= afterDate }
            if (next != null) {
                return next
            }
            currentYM = currentYM.nextMonth()
        }
        return null
    }

    fun scheduleRemindersForBill(
        context: Context,
        bill: RecurringPaymentEntity,
        reminders: List<ReminderEntity>
    ) {
        if (bill.status != ScheduleStatus.ACTIVE.name) {
            cancelRemindersForBill(context, bill.id, reminders)
            return
        }

        val nextDueDate = findNextDueDate(bill) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (reminder in reminders) {
            if (!reminder.isEnabled) continue

            val triggerDate = nextDueDate.plusDays(-reminder.daysBefore)
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, triggerDate.year)
                set(Calendar.MONTH, triggerDate.month - 1)
                set(Calendar.DAY_OF_MONTH, triggerDate.day)
                set(Calendar.HOUR_OF_DAY, reminder.reminderHour)
                set(Calendar.MINUTE, reminder.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val triggerMillis = cal.timeInMillis
            // Only schedule if in future
            if (triggerMillis > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    action = ACTION_SHOW_REMINDER
                    putExtra(EXTRA_BILL_ID, bill.id)
                    putExtra(EXTRA_BILL_NAME, bill.name)
                    putExtra(EXTRA_AMOUNT, bill.amount)
                    putExtra(EXTRA_DUE_DATE, nextDueDate.isoString)
                    putExtra(EXTRA_DAYS_BEFORE, reminder.daysBefore)
                    putExtra(EXTRA_NOTIFICATION_ID, getRequestCode(bill.id, reminder.daysBefore))
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    getRequestCode(bill.id, reminder.daysBefore),
                    intent,
                    flags
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } catch (_: SecurityException) {
                    // Fallback if exact alarm permissions not permitted
                }
            }
        }
    }

    fun cancelRemindersForBill(
        context: Context,
        billId: Long,
        reminders: List<ReminderEntity>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (reminder in reminders) {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
            }
            val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(billId, reminder.daysBefore),
                intent,
                flags
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    suspend fun rescheduleAllActiveReminders(context: Context) {
        val database = AppDatabase.getInstance(context)
        val activeBills = database.recurringPaymentDao().getActiveRecurringPayments()
        // Read current active bills once
        val bills = database.recurringPaymentDao().getAllRecurringPaymentsOnce().filter {
            it.status == ScheduleStatus.ACTIVE.name
        }
        for (bill in bills) {
            val reminders = database.reminderDao().getRemindersForPaymentOnce(bill.id)
            scheduleRemindersForBill(context, bill, reminders)
        }
    }

    fun showNotification(
        context: Context,
        billId: Long,
        billName: String,
        amount: Double,
        dueDateStr: String,
        daysBefore: Int,
        notificationId: Int
    ) {
        createNotificationChannel(context)

        val title = if (daysBefore == 0) {
            "Bill Due Today: $billName"
        } else {
            "Upcoming Bill: $billName in $daysBefore days"
        }
        val text = "Amount due: ₹${amount.toLong()} on $dueDateStr"

        // Open app on click
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Paid
        val markPaidIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_MARK_PAID
            putExtra(EXTRA_BILL_ID, billId)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DUE_DATE, dueDateStr)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 1 Day
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_BILL_ID, billId)
            putExtra(EXTRA_BILL_NAME, billName)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DUE_DATE, dueDateStr)
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Paid", markPaidPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 1 Day", snoozePendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission might have been revoked
        }
    }
}
