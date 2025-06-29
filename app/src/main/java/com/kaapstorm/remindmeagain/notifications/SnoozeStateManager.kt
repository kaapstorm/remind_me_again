package com.kaapstorm.remindmeagain.notifications

import com.kaapstorm.remindmeagain.data.model.Reminder // Assuming you have this
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository // For fetching next due time

/**
 * Manages snooze state for reminders.
 * Uses dependency injection to allow for different storage implementations.
 */
class SnoozeStateManager(
    private val repository: SnoozeStateRepository
) {

    companion object {
        const val DEFAULT_INITIAL_SNOOZE_SECONDS = 60 // 1 minute

        // Action for the intent that triggers SnoozeActionHandlerReceiver
        const val ACTION_SNOOZE_REMINDER = "com.kaapstorm.remindmeagain.ACTION_SNOOZE_REMINDER"
        // Action for the intent that triggers ShowSnoozedNotificationReceiver
        const val ACTION_SHOW_SNOOZED_NOTIFICATION = "com.kaapstorm.remindmeagain.ACTION_SHOW_SNOOZED_NOTIFICATION"
        // Action for the intent that triggers DoneActionHandlerReceiver
        const val ACTION_DONE_REMINDER = "com.kaapstorm.remindmeagain.ACTION_DONE_REMINDER"

        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SNOOZE_INTERVAL_SECONDS = "snooze_interval_seconds"
    }

    /**
     * Stores the interval that the snooze ALARM should be set for.
     * This is called from SnoozeActionHandlerReceiver when "Later" is tapped.
     * The value stored here is the duration of the upcoming snooze.
     */
    suspend fun setSnoozeAlarmInterval(reminderId: Long, intervalSeconds: Int) {
        repository.setSnoozeAlarmInterval(reminderId, intervalSeconds)
    }

    /**
     * Retrieves the interval for which the current snooze alarm was set.
     * This is called in ShowSnoozedNotificationReceiver to know how long the just-finished snooze was.
     * If no value is found, it means this might be the first time after an app restart,
     * or the state was cleared. Defaulting to a sensible value or handling it is important.
     * However, since alarms are restored, this should ideally always find a value if an alarm fired.
     */
    suspend fun getCompletedSnoozeInterval(reminderId: Long): Int {
        return repository.getCompletedSnoozeInterval(reminderId)
    }

    /**
     * Resets/clears the snooze state for a reminder.
     * Called when "Done" is tapped or when a new main instance of a reminder is shown.
     */
    suspend fun clearSnoozeState(reminderId: Long) {
        repository.clearSnoozeState(reminderId)
    }

    /**
     * Calculates the next snooze interval to be used if the user taps "Later" again.
     *
     * @param completedIntervalSeconds The interval that was just completed (led to the current notification).
     * @param reminderNextMainDueTimestamp The timestamp of the next main scheduled occurrence of the reminder.
     *                                     Can be Long.MAX_VALUE if not applicable or not easily available.
     * @param currentTimeMillis The current time.
     * @return Pair: The next interval (for the "Later" button on the new notification) in seconds,
     *               and a boolean indicating if "Later" button should still be shown.
     */
    fun calculateNextSnoozeIntervalForButton(
        completedIntervalSeconds: Int,
        reminderNextMainDueTimestamp: Long,
        currentTimeMillis: Long
    ): Pair<Int, Boolean> {
        val nextButtonIntervalSeconds = completedIntervalSeconds * 2
        // Time when the notification would show if snoozed with nextButtonIntervalSeconds
        val potentialNextSnoozeTimeMillis = currentTimeMillis + (nextButtonIntervalSeconds * 1000L)

        // As per design: "The "Later" interval cannot exceed the time when the reminder is due again."
        if (reminderNextMainDueTimestamp != Long.MAX_VALUE && potentialNextSnoozeTimeMillis >= reminderNextMainDueTimestamp) {
            // If doubling exceeds the next main due time, "Later" button should not be shown.
            // The interval calculated is still returned but the boolean is false.
            return Pair(nextButtonIntervalSeconds, false)
        }
        // TODO: Consider adding an absolute maximum snooze (e.g., 1 hour = 3600 seconds)
        // if (nextButtonIntervalSeconds > 3600) return Pair(3600, true) // Example max
        return Pair(nextButtonIntervalSeconds, true)
    }
}
