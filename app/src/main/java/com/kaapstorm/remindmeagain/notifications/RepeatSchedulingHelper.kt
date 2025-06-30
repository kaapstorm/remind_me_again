package com.kaapstorm.remindmeagain.notifications

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import java.time.Instant
import java.time.ZoneId

/**
 * Helper class for managing repeat notification scheduling logic.
 * This encapsulates the business logic for determining when to schedule repeats
 * and makes it more testable.
 */
class RepeatSchedulingHelper(
    private val snoozeStateManager: SnoozeStateManager,
    private val nextOccurrenceCalculator: NextOccurrenceCalculator
) {

    /**
     * Determines whether a repeat should be scheduled and with what interval.
     * 
     * @param reminder The reminder for which to check repeat scheduling
     * @param isRepeat Whether this is a repeat notification (vs initial)
     * @param currentIntervalSeconds The current interval being used for repeats
     * @return RepeatSchedulingDecision containing whether to schedule and the interval to use
     */
    suspend fun shouldScheduleRepeat(
        reminder: Reminder,
        isRepeat: Boolean,
        currentIntervalSeconds: Int
    ): RepeatSchedulingDecision {
        if (!isRepeat) {
            // For initial notifications, always schedule the first repeat
            return RepeatSchedulingDecision(
                shouldSchedule = true,
                intervalSeconds = currentIntervalSeconds,
                reason = "Initial notification - scheduling first repeat"
            )
        }

        // For repeat notifications, check if we should continue repeating
        val completedInterval = snoozeStateManager.getCompletedSnoozeInterval(reminder.id)
        val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val nextMainDueTimestamp = nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(reminder, now)
        
        val (nextButtonInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedInterval,
            nextMainDueTimestamp,
            System.currentTimeMillis()
        )

        return if (shouldShowLater) {
            RepeatSchedulingDecision(
                shouldSchedule = true,
                intervalSeconds = currentIntervalSeconds,
                reason = "Continue repeating with current interval"
            )
        } else {
            RepeatSchedulingDecision(
                shouldSchedule = false,
                intervalSeconds = 0,
                reason = "Next main occurrence due - stopping repeats"
            )
        }
    }

    /**
     * Data class representing the decision about whether to schedule a repeat.
     */
    data class RepeatSchedulingDecision(
        val shouldSchedule: Boolean,
        val intervalSeconds: Int,
        val reason: String
    )
} 