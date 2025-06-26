package com.kaapstorm.remindmeagain.domain.service

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Service responsible for reminder scheduling business logic.
 * Determines when reminders should be active based on their schedule.
 */
class ReminderSchedulingService {
    
    /**
     * Determines if a reminder should be active at a given date/time.
     * 
     * @param reminder The reminder to check
     * @param dateTime The date/time to check against
     * @return true if the reminder should be active at the given date/time
     */
    fun isReminderActive(reminder: Reminder, dateTime: LocalDateTime): Boolean {
        val reminderTime = reminder.time
        val schedule = reminder.schedule
        
        // Check if the time matches
        if (dateTime.toLocalTime() != reminderTime) {
            return false
        }
        
        return when (schedule) {
            is ReminderSchedule.Daily -> true
            
            is ReminderSchedule.Weekly -> {
                schedule.days.contains(dateTime.dayOfWeek)
            }
            
            is ReminderSchedule.Fortnightly -> {
                if (dateTime.dayOfWeek != schedule.day) {
                    return false
                }
                
                // For fortnightly, we need to check if this is an even or odd week
                // Let's use January 1, 2025 as the reference date (week 0)
                val referenceDate = LocalDate.of(2025, 1, 1)
                val currentDate = dateTime.toLocalDate()
                
                // Calculate weeks since reference date
                val daysSinceReference = currentDate.toEpochDay() - referenceDate.toEpochDay()
                val weeksSinceReference = daysSinceReference / 7
                
                // Check if it's an even week (0, 2, 4, etc.)
                weeksSinceReference % 2L == 0L
            }
            
            is ReminderSchedule.Monthly -> {
                if (schedule.dayOfWeek != null && schedule.weekOfMonth != null) {
                    // Check if it's the correct day of week
                    if (dateTime.dayOfWeek != schedule.dayOfWeek) {
                        return false
                    }
                    
                    // Check if it's the correct week of month
                    val dayOfMonth = dateTime.dayOfMonth
                    val weekOfMonth = ((dayOfMonth - 1) / 7) + 1
                    weekOfMonth == schedule.weekOfMonth
                } else if (schedule.dayOfMonth != null) {
                    dateTime.dayOfMonth == schedule.dayOfMonth
                } else {
                    false
                }
            }
        }
    }
    
    /**
     * Gets the next occurrence of a reminder after a given date/time.
     * 
     * @param reminder The reminder to find the next occurrence for
     * @param afterDateTime The date/time to find the next occurrence after
     * @return The next occurrence date/time, or null if no future occurrences
     */
    fun getNextOccurrence(reminder: Reminder, afterDateTime: LocalDateTime): LocalDateTime? {
        // This is a placeholder for future implementation
        // Would calculate the next time the reminder should be active
        return null
    }
    
    /**
     * Gets all occurrences of a reminder within a date range.
     * 
     * @param reminder The reminder to find occurrences for
     * @param startDateTime The start of the range
     * @param endDateTime The end of the range
     * @return List of date/times when the reminder should be active
     */
    fun getOccurrencesInRange(
        reminder: Reminder, 
        startDateTime: LocalDateTime, 
        endDateTime: LocalDateTime
    ): List<LocalDateTime> {
        // This is a placeholder for future implementation
        // Would calculate all occurrences within the given range
        return emptyList()
    }
}
