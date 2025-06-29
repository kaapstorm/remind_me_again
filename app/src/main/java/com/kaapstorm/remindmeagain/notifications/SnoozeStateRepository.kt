package com.kaapstorm.remindmeagain.notifications

/**
 * Repository interface for persisting snooze state.
 * This allows for different implementations in production (DataStore) and tests (in-memory).
 */
interface SnoozeStateRepository {
    /**
     * Stores the interval that the snooze ALARM should be set for.
     */
    suspend fun setSnoozeAlarmInterval(reminderId: Long, intervalSeconds: Int)

    /**
     * Retrieves the interval for which the current snooze alarm was set.
     * Returns the default interval if no value is found.
     */
    suspend fun getCompletedSnoozeInterval(reminderId: Long): Int

    /**
     * Resets/clears the snooze state for a reminder.
     */
    suspend fun clearSnoozeState(reminderId: Long)
} 