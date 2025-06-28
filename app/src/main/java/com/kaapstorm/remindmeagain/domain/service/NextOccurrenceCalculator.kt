package com.kaapstorm.remindmeagain.domain.service

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Utility class for calculating the next occurrence of a reminder based on its schedule.
 * This is used to constrain the "Later" button so it doesn't exceed the next main reminder time.
 */
class NextOccurrenceCalculator {
    
    /**
     * Calculates the next main occurrence timestamp for a reminder based on its schedule.
     * 
     * @param reminder The reminder to calculate the next occurrence for
     * @param currentTime The current time to calculate from
     * @return The timestamp in milliseconds of the next main occurrence, or Long.MAX_VALUE if no future occurrence
     */
    fun getNextMainOccurrenceTimestamp(reminder: Reminder, currentTime: LocalDateTime): Long {
        val reminderTime = reminder.time
        val schedule = reminder.schedule
        
        return when (schedule) {
            is ReminderSchedule.Daily -> {
                // For daily reminders, next occurrence is the same time tomorrow (or today if we haven't passed that time)
                val todayAtReminderTime = currentTime.toLocalDate().atTime(reminderTime)
                val nextOccurrence = if (currentTime.isBefore(todayAtReminderTime)) {
                    todayAtReminderTime
                } else {
                    todayAtReminderTime.plusDays(1)
                }
                nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            
            is ReminderSchedule.Weekly -> {
                // Find the next occurrence among the selected days
                val selectedDays = schedule.days.sortedBy { it.value }
                val currentDayOfWeek = currentTime.dayOfWeek
                
                // Find the next day in the selected days
                val nextDay = selectedDays.find { it.value > currentDayOfWeek.value }
                    ?: selectedDays.first() // If no day found, take the first day of next week
                
                val daysToAdd = if (nextDay.value > currentDayOfWeek.value) {
                    nextDay.value - currentDayOfWeek.value
                } else {
                    7 - currentDayOfWeek.value + nextDay.value
                }
                
                val nextOccurrence = currentTime.toLocalDate()
                    .plusDays(daysToAdd.toLong())
                    .atTime(reminderTime)
                
                // If the calculated time is in the past (same day but time has passed), move to next week
                val finalOccurrence = if (nextOccurrence.isBefore(currentTime)) {
                    nextOccurrence.plusWeeks(1)
                } else {
                    nextOccurrence
                }
                
                finalOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            
            is ReminderSchedule.Fortnightly -> {
                // Use the schedule's date as the reference date
                val referenceDate = schedule.date
                val targetDay = schedule.date.dayOfWeek
                
                // Find the next occurrence of the target day
                val currentDate = currentTime.toLocalDate()
                val currentDayOfWeek = currentDate.dayOfWeek
                
                // Calculate days until next occurrence of target day
                val daysUntilTarget = if (targetDay.value > currentDayOfWeek.value) {
                    targetDay.value - currentDayOfWeek.value
                } else {
                    7 - currentDayOfWeek.value + targetDay.value
                }
                
                var nextOccurrence = currentDate.plusDays(daysUntilTarget.toLong()).atTime(reminderTime)
                
                // If the calculated time is in the past, move to next week
                if (nextOccurrence.isBefore(currentTime)) {
                    nextOccurrence = nextOccurrence.plusWeeks(1)
                }
                
                // Check if this is the correct fortnightly occurrence
                val daysSinceReference = nextOccurrence.toLocalDate().toEpochDay() - referenceDate.toEpochDay()
                val weeksSinceReference = daysSinceReference / 7
                
                // If it's not an even week, move to the next occurrence
                if (weeksSinceReference % 2L != 0L) {
                    nextOccurrence = nextOccurrence.plusWeeks(1)
                }
                
                nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            
            is ReminderSchedule.Monthly -> {
                when {
                    schedule.dayOfMonth != null -> {
                        // Monthly by day of month (e.g., 15th of every month)
                        val targetDay = schedule.dayOfMonth
                        val currentDate = currentTime.toLocalDate()
                        
                        // Try to create the target date in current month
                        val targetDateInCurrentMonth = try {
                            currentDate.withDayOfMonth(targetDay)
                        } catch (e: java.time.DateTimeException) {
                            // If the day doesn't exist in this month (e.g., Feb 30), move to next month
                            currentDate.plusMonths(1).withDayOfMonth(1)
                        }
                        
                        var nextOccurrence = targetDateInCurrentMonth.atTime(reminderTime)
                        
                        // If the calculated time is in the past, move to next month
                        if (nextOccurrence.isBefore(currentTime)) {
                            val nextMonth = currentDate.plusMonths(1)
                            nextOccurrence = try {
                                nextMonth.withDayOfMonth(targetDay).atTime(reminderTime)
                            } catch (e: java.time.DateTimeException) {
                                // If the day doesn't exist in next month either, find the next valid occurrence
                                var searchMonth = nextMonth
                                var found = false
                                while (!found) {
                                    searchMonth = searchMonth.plusMonths(1)
                                    try {
                                        nextOccurrence = searchMonth.withDayOfMonth(targetDay).atTime(reminderTime)
                                        found = true
                                    } catch (e: java.time.DateTimeException) {
                                        // Continue searching
                                    }
                                }
                                nextOccurrence
                            }
                        }
                        
                        nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    
                    schedule.dayOfWeek != null && schedule.weekOfMonth != null -> {
                        // Monthly by nth weekday (e.g., 3rd Tuesday of every month)
                        val targetWeekday = schedule.dayOfWeek
                        val targetWeek = schedule.weekOfMonth
                        val currentDate = currentTime.toLocalDate()
                        
                        // Calculate the target date in current month
                        val firstOfMonth = currentDate.withDayOfMonth(1)
                        val firstTargetWeekday = firstOfMonth.plusDays(
                            ((targetWeekday.value - firstOfMonth.dayOfWeek.value + 7) % 7).toLong()
                        )
                        val targetDateInCurrentMonth = firstTargetWeekday.plusWeeks((targetWeek - 1).toLong())
                        
                        var nextOccurrence = targetDateInCurrentMonth.atTime(reminderTime)
                        
                        // If the calculated time is in the past or not in current month, move to next month
                        if (nextOccurrence.isBefore(currentTime) || targetDateInCurrentMonth.month != currentDate.month) {
                            val nextMonth = currentDate.plusMonths(1)
                            val firstOfNextMonth = nextMonth.withDayOfMonth(1)
                            val firstTargetInNext = firstOfNextMonth.plusDays(
                                ((targetWeekday.value - firstOfNextMonth.dayOfWeek.value + 7) % 7).toLong()
                            )
                            nextOccurrence = firstTargetInNext.plusWeeks((targetWeek - 1).toLong()).atTime(reminderTime)
                        }
                        
                        nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    
                    else -> {
                        // Invalid monthly schedule
                        Long.MAX_VALUE
                    }
                }
            }
        }
    }
} 