package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.kaapstorm.remindmeagain.data.model.Reminder
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun startPeriodicReminderCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build()

        val reminderCheckRequest = PeriodicWorkRequest.Builder(
            ReminderWorker::class.java,
            15, // Minimum interval for periodic work is 15 minutes
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(ReminderWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderCheckRequest
        )
    }

    fun stopPeriodicReminderCheck() {
        workManager.cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    fun schedulePostponeNotification(reminder: Reminder, intervalSeconds: Int) {
        val postponeRequest = OneTimeWorkRequest.Builder(PostponeNotificationWorker::class.java)
            .setInitialDelay(intervalSeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong(PostponeNotificationWorker.KEY_REMINDER_ID, reminder.id)
                    .build()
            )
            .addTag("postpone_${reminder.id}")
            .build()

        workManager.enqueueUniqueWork(
            "postpone_${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            postponeRequest
        )
    }

    fun cancelPostponeNotification(reminderId: Long) {
        workManager.cancelUniqueWork("postpone_$reminderId")
    }

    fun rescheduleAllReminders() {
        // Cancel existing work
        stopPeriodicReminderCheck()
        
        // Restart periodic check
        startPeriodicReminderCheck()
    }

    fun triggerImmediateReminderCheck() {
        val immediateCheckRequest = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
            .addTag("immediate_reminder_check")
            .build()

        workManager.enqueueUniqueWork(
            "immediate_reminder_check",
            ExistingWorkPolicy.REPLACE,
            immediateCheckRequest
        )
    }
}