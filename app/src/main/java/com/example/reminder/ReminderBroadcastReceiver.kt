package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.repository.FinancialPlannerRepository
import com.example.domain.AppDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)

        when (action) {
            ReminderScheduler.ACTION_SHOW_REMINDER -> {
                val billId = intent.getLongExtra(ReminderScheduler.EXTRA_BILL_ID, -1L)
                val billName = intent.getStringExtra(ReminderScheduler.EXTRA_BILL_NAME) ?: "Bill"
                val amount = intent.getDoubleExtra(ReminderScheduler.EXTRA_AMOUNT, 0.0)
                val dueDateStr = intent.getStringExtra(ReminderScheduler.EXTRA_DUE_DATE) ?: ""
                val daysBefore = intent.getIntExtra(ReminderScheduler.EXTRA_DAYS_BEFORE, 0)

                ReminderScheduler.showNotification(
                    context = context,
                    billId = billId,
                    billName = billName,
                    amount = amount,
                    dueDateStr = dueDateStr,
                    daysBefore = daysBefore,
                    notificationId = notificationId
                )
            }

            ReminderScheduler.ACTION_MARK_PAID -> {
                val pendingResult = goAsync()
                val billId = intent.getLongExtra(ReminderScheduler.EXTRA_BILL_ID, -1L)
                val amount = intent.getDoubleExtra(ReminderScheduler.EXTRA_AMOUNT, 0.0)
                val dueDateStr = intent.getStringExtra(ReminderScheduler.EXTRA_DUE_DATE) ?: ""

                NotificationManagerCompat.from(context).cancel(notificationId)

                if (billId != -1L && dueDateStr.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            val repo = FinancialPlannerRepository(db)
                            val dueDate = AppDate.parse(dueDateStr)
                            repo.markPaymentAsPaid(
                                occurrenceId = 0L,
                                recurringPaymentId = billId,
                                dueDate = dueDate,
                                amount = amount,
                                paidDate = AppDate.today(),
                                paymentMethod = "Reminder Action"
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }

            ReminderScheduler.ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(notificationId)

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val snoozeTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L // 24 hours later

                val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    this.action = ReminderScheduler.ACTION_SHOW_REMINDER
                    putExtras(intent)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 50000,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                } catch (_: SecurityException) {
                }
            }
        }
    }
}
