package com.kaapstorm.remindmeagain.notifications

/**
 * In-memory implementation of SnoozeStateRepository for testing.
 */
class InMemorySnoozeStateRepository : SnoozeStateRepository {
    
    companion object {
        const val DEFAULT_INITIAL_SNOOZE_SECONDS = 60 // 1 minute
    }

    private val snoozeState = mutableMapOf<Long, Int>()

    override suspend fun setSnoozeAlarmInterval(reminderId: Long, intervalSeconds: Int) {
        snoozeState[reminderId] = intervalSeconds
    }

    override suspend fun getCompletedSnoozeInterval(reminderId: Long): Int {
        return snoozeState[reminderId] ?: DEFAULT_INITIAL_SNOOZE_SECONDS
    }

    override suspend fun clearSnoozeState(reminderId: Long) {
        snoozeState.remove(reminderId)
    }

    /**
     * Clear all stored state (useful for test cleanup).
     */
    fun clearAll() {
        snoozeState.clear()
    }
} 