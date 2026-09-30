package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdultingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)

        // Reschedule active reminders on startup in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAllActiveReminders(this@AdultingApp)
            } catch (_: Exception) {
            }
        }
    }
}
