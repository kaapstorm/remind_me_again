package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PostponeNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val reminderRepository: ReminderRepository by inject()
    private val notificationManager: ReminderNotificationManager by inject()

    override suspend fun doWork(): Result {
        return try {
            val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
            if (reminderId == -1L) return Result.failure()

            val reminder = reminderRepository.getReminderById(reminderId).first()
            if (reminder != null) {
                notificationManager.showReminderNotification(
                    reminder,
                    showLaterButton = true,
                    isSnoozedNotification = false
                )
                Result.success()
            } else {
                Result.failure()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
