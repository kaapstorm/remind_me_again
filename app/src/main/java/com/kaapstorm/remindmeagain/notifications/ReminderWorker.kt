package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZoneId

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val reminderRepository: ReminderRepository by inject()
    private val schedulingService: ReminderSchedulingService by inject()
    private val notificationManager: ReminderNotificationManager by inject()

    override suspend fun doWork(): Result {
        return try {
            val now = Instant.now().atZone(ZoneId.systemDefault())
            val currentTime = now.toLocalTime()
            val currentDateTime = now.toLocalDateTime()

            // Get all active reminders (not completed)
            val allActiveReminders = reminderRepository.getActiveReminders().first()

            for (reminder in allActiveReminders) {
                // Check if this reminder is due right now
                val isActive = schedulingService.isReminderActive(
                    reminder = reminder,
                    dateTime = currentDateTime
                )

                // Check if the reminder time is within the current minute
                // (to handle the fact that WorkManager runs every 15+ minutes)
                val reminderTime = reminder.time
                val timeDifferenceMinutes = kotlin.math.abs(
                    currentTime.toSecondOfDay() - reminderTime.toSecondOfDay()
                ) / 60

                // Show notification if reminder is active and time is within 15 minutes of current time
                if (isActive && timeDifferenceMinutes <= 15) {
                    notificationManager.showReminderNotification(
                        reminder,
                        showLaterButton = true,
                        isSnoozedNotification = false
                    )
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "reminder_check"
        const val TAG = "reminder_worker"
    }
}