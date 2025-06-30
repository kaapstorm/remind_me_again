package com.kaapstorm.remindmeagain.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import java.time.Instant
import java.time.ZoneId

/**
 * Handles AlarmManager alarms for reminders and repeat notifications.
 * Shows the notification and schedules the next repeat if needed.
 */
class ReminderAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationManager: ReminderNotificationManager by inject()
    private val reminderRepository: com.kaapstorm.remindmeagain.data.repository.ReminderRepository by inject()
    private val snoozeStateManager: SnoozeStateManager by inject()
    private val nextOccurrenceCalculator: NextOccurrenceCalculator by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val repeatSchedulingHelper: RepeatSchedulingHelper by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val isRepeat = intent.getBooleanExtra(EXTRA_IS_REPEAT, false)
        val repeatIntervalSeconds = intent.getIntExtra(EXTRA_REPEAT_INTERVAL_SECONDS, SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS)

        if (reminderId == -1L) {
            Log.e("ReminderAlarmReceiver", "Invalid reminderId received")
            return
        }

        Log.d("ReminderAlarmReceiver", "Alarm received for reminder $reminderId (isRepeat=$isRepeat, interval=$repeatIntervalSeconds)")

        scope.launch {
            val reminder = reminderRepository.getReminderByIdSuspend(reminderId)
            if (reminder == null) {
                Log.e("ReminderAlarmReceiver", "Reminder with ID $reminderId not found.")
                snoozeStateManager.clearSnoozeState(reminderId)
                return@launch
            }

            // Show the notification
            notificationManager.showReminderNotification(
                reminder = reminder,
                nextSnoozeIntervalSeconds = repeatIntervalSeconds,
                showLaterButton = true,
                isSnoozedNotification = isRepeat
            )

            // Determine if we should schedule the next repeat
            val decision = repeatSchedulingHelper.shouldScheduleRepeat(
                reminder = reminder,
                isRepeat = isRepeat,
                currentIntervalSeconds = repeatIntervalSeconds
            )

            if (decision.shouldSchedule) {
                reminderScheduler.scheduleRepeat(reminderId, decision.intervalSeconds)
                Log.d("ReminderAlarmReceiver", "Scheduled repeat for reminder $reminderId in ${decision.intervalSeconds} seconds: ${decision.reason}")
            } else {
                Log.d("ReminderAlarmReceiver", "No repeat scheduled for reminder $reminderId: ${decision.reason}")
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_IS_REPEAT = "is_repeat"
        const val EXTRA_REPEAT_INTERVAL_SECONDS = "repeat_interval_seconds"
    }
} 