package com.kaapstorm.remindmeagain.notifications.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.notifications.SnoozeStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShowSnoozedNotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val reminderNotificationManager: ReminderNotificationManager by inject()
    private val snoozeStateManager: SnoozeStateManager by inject()
    private val reminderRepository: ReminderRepository by inject() // To get next main due date
    private val nextOccurrenceCalculator: NextOccurrenceCalculator by inject()
    private val scope =
        CoroutineScope(Dispatchers.Main + SupervisorJob()) // Use Main for UI, IO for DB

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(SnoozeStateManager.Companion.EXTRA_REMINDER_ID, -1L)

        if (reminderId == -1L) {
            Log.e("ShowSnoozedReceiver", "Invalid reminderId received")
            return
        }

        scope.launch {
            val reminder = reminderRepository.getReminderById(reminderId).first() // Suspending call
            if (reminder == null) {
                Log.e("ShowSnoozedReceiver", "Reminder with ID $reminderId not found.")
                snoozeStateManager.clearSnoozeState(reminderId) // Clean up if reminder is gone
                return@launch
            }

            // This is the interval that was *just completed* to trigger this receiver
            val completedSnoozeInterval = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
            Log.d("ShowSnoozedReceiver", "Showing snoozed notification for reminder $reminderId. Completed snooze: $completedSnoozeInterval s")

            // Calculate the next main due timestamp for the reminder
            val nextMainDueTimestamp = getNextMainOccurrenceTimestamp(reminder)

            val (nextIntervalForLaterButton, shouldShowLaterButton) =
                snoozeStateManager.calculateNextSnoozeIntervalForButton(
                    completedSnoozeInterval,
                    nextMainDueTimestamp,
                    System.currentTimeMillis()
                )

            reminderNotificationManager.showReminderNotification(
                reminder = reminder,
                nextSnoozeIntervalSeconds = nextIntervalForLaterButton,
                showLaterButton = shouldShowLaterButton,
                isSnoozedNotification = true // Add a flag if needed for slightly different behavior/logging
            )
        }
    }

    /**
     * Calculates the next main occurrence timestamp for a reminder based on its schedule.
     * This is used to constrain the "Later" button so it doesn't exceed the next main reminder time.
     * 
     * @param reminder The reminder to calculate the next occurrence for
     * @return The timestamp in milliseconds of the next main occurrence, or Long.MAX_VALUE if no future occurrence
     */
    private suspend fun getNextMainOccurrenceTimestamp(reminder: com.kaapstorm.remindmeagain.data.model.Reminder): Long {
        val now = java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        return nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(reminder, now)
    }
} 