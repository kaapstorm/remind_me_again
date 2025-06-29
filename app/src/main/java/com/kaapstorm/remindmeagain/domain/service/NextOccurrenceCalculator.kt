package com.kaapstorm.remindmeagain.domain.service

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import java.time.LocalDateTime
import java.time.LocalTime
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
        val schedule = reminder.schedule
        val reminderTime = reminder.time
        
        return when (schedule) {
            is ReminderSchedule.Daily -> {
                val nextOccurrence = currentTime.toLocalDate().atTime(reminderTime)
                val finalOccurrence = if (nextOccurrence.isBefore(currentTime)) {
                    nextOccurrence.plusDays(1)
                } else {
                    nextOccurrence
                }
                finalOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            
            is ReminderSchedule.Weekly -> calculateNextWeeklyOccurrence(schedule, reminderTime, currentTime)
            
            is ReminderSchedule.Fortnightly -> calculateNextFortnightlyOccurrence(schedule, reminderTime, currentTime)
            
            is ReminderSchedule.Monthly -> calculateNextMonthlyOccurrence(schedule, reminderTime, currentTime)
        }
    }
    
    private fun calculateNextWeeklyOccurrence(
        schedule: ReminderSchedule.Weekly,
        reminderTime: LocalTime,
        currentTime: LocalDateTime
    ): Long {
        val selectedDays = schedule.days.sortedBy { it.value }
        val currentDayOfWeek = currentTime.dayOfWeek
        
        // First, check if today is a selected day and the time hasn't passed yet
        if (selectedDays.contains(currentDayOfWeek)) {
            val todayAtReminderTime = currentTime.toLocalDate().atTime(reminderTime)
            if (!todayAtReminderTime.isBefore(currentTime)) {
                return todayAtReminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        
        // Find the next occurrence among the selected days
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
        
        return nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun calculateNextFortnightlyOccurrence(
        schedule: ReminderSchedule.Fortnightly,
        reminderTime: LocalTime,
        currentTime: LocalDateTime
    ): Long {
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
        
        return nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun calculateNextMonthlyOccurrence(
        schedule: ReminderSchedule.Monthly,
        reminderTime: LocalTime,
        currentTime: LocalDateTime
    ): Long {
        return when {
            schedule.dayOfMonth != null -> calculateNextMonthlyByDayOfMonth(schedule.dayOfMonth, reminderTime, currentTime)
            schedule.dayOfWeek != null && schedule.weekOfMonth != null -> calculateNextMonthlyByNthWeekday(schedule.dayOfWeek, schedule.weekOfMonth, reminderTime, currentTime)
            else -> Long.MAX_VALUE
        }
    }
    
    private fun calculateNextMonthlyByDayOfMonth(
        dayOfMonth: Int,
        reminderTime: LocalTime,
        currentTime: LocalDateTime
    ): Long {
        val currentDate = currentTime.toLocalDate()
        
        // Try to create the target date in current month
        val targetDateInCurrentMonth = try {
            currentDate.withDayOfMonth(dayOfMonth)
        } catch (e: java.time.DateTimeException) {
            // If the day doesn't exist in this month (e.g., Feb 30), move to next month
            currentDate.plusMonths(1).withDayOfMonth(1)
        }
        
        var nextOccurrence = targetDateInCurrentMonth.atTime(reminderTime)
        
        // If the calculated time is in the past, move to next month
        if (nextOccurrence.isBefore(currentTime)) {
            val nextMonth = currentDate.plusMonths(1)
            nextOccurrence = try {
                nextMonth.withDayOfMonth(dayOfMonth).atTime(reminderTime)
            } catch (e: java.time.DateTimeException) {
                // If the day doesn't exist in next month either, find the next valid occurrence
                findNextValidDayOfMonth(dayOfMonth, nextMonth, reminderTime)
            }
        }
        
        return nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun findNextValidDayOfMonth(
        dayOfMonth: Int,
        startMonth: java.time.LocalDate,
        reminderTime: LocalTime
    ): LocalDateTime {
        var searchMonth = startMonth
        while (true) {
            searchMonth = searchMonth.plusMonths(1)
            try {
                return searchMonth.withDayOfMonth(dayOfMonth).atTime(reminderTime)
            } catch (e: java.time.DateTimeException) {
                // Continue searching
            }
        }
    }
    
    private fun calculateNextMonthlyByNthWeekday(
        dayOfWeek: java.time.DayOfWeek,
        weekOfMonth: Int,
        reminderTime: LocalTime,
        currentTime: LocalDateTime
    ): Long {
        val currentDate = currentTime.toLocalDate()
        
        // Calculate the target date in current month
        val targetDateInCurrentMonth = calculateNthWeekdayInMonth(currentDate, dayOfWeek, weekOfMonth)
        
        var nextOccurrence = targetDateInCurrentMonth.atTime(reminderTime)
        
        // If the calculated time is in the past or not in current month, move to next month
        if (nextOccurrence.isBefore(currentTime) || targetDateInCurrentMonth.month != currentDate.month) {
            val nextMonth = currentDate.plusMonths(1)
            nextOccurrence = calculateNthWeekdayInMonth(nextMonth, dayOfWeek, weekOfMonth).atTime(reminderTime)
        }
        
        return nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun calculateNthWeekdayInMonth(
        month: java.time.LocalDate,
        dayOfWeek: java.time.DayOfWeek,
        weekOfMonth: Int
    ): java.time.LocalDate {
        val firstOfMonth = month.withDayOfMonth(1)
        val firstTargetWeekday = firstOfMonth.plusDays(
            ((dayOfWeek.value - firstOfMonth.dayOfWeek.value + 7) % 7).toLong()
        )
        return firstTargetWeekday.plusWeeks((weekOfMonth - 1).toLong())
    }
} 