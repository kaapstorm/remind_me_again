package com.kaapstorm.remindmeagain.domain.service

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId

class NextOccurrenceCalculatorTest {
    
    private lateinit var calculator: NextOccurrenceCalculator
    private val reminderTime = LocalTime.of(21, 30) // 21:30
    private val reminderName = "Test Reminder"
    
    @Before
    fun setUp() {
        calculator = NextOccurrenceCalculator()
    }
    
    @Test
    fun testDailySchedule() {
        val schedule = ReminderSchedule.Daily
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test when current time is before reminder time (same day)
        val beforeTime = LocalDateTime.of(2025, 1, 1, 20, 0) // 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, beforeTime)
        val expectedTime = LocalDateTime.of(2025, 1, 1, 21, 30) // Same day at 21:30
        
        assertEquals("Daily reminder should occur same day if time hasn't passed", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
        
        // Test when current time is after reminder time (next day)
        val afterTime = LocalDateTime.of(2025, 1, 1, 22, 0) // 22:00
        val nextOccurrenceAfter = calculator.getNextMainOccurrenceTimestamp(reminder, afterTime)
        val expectedTimeAfter = LocalDateTime.of(2025, 1, 2, 21, 30) // Next day at 21:30
        
        assertEquals("Daily reminder should occur next day if time has passed", 
            expectedTimeAfter.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceAfter)
    }
    
    @Test
    fun testWeeklySchedule() {
        val schedule = ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test on Tuesday (should find Thursday of same week)
        val tuesdayTime = LocalDateTime.of(2025, 1, 7, 20, 0) // Tuesday 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, tuesdayTime)
        val expectedTime = LocalDateTime.of(2025, 1, 9, 21, 30) // Thursday 21:30
        
        assertEquals("Weekly reminder should find next selected day in same week", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
        
        // Test on Friday (should find Monday of next week)
        val fridayTime = LocalDateTime.of(2025, 1, 10, 20, 0) // Friday 20:00
        val nextOccurrenceFriday = calculator.getNextMainOccurrenceTimestamp(reminder, fridayTime)
        val expectedTimeFriday = LocalDateTime.of(2025, 1, 13, 21, 30) // Monday 21:30
        
        assertEquals("Weekly reminder should find first selected day of next week", 
            expectedTimeFriday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceFriday)
        
        // Test on Thursday after reminder time (should find Monday of next week)
        val thursdayAfterTime = LocalDateTime.of(2025, 1, 9, 22, 0) // Thursday 22:00 (after 21:30)
        val nextOccurrenceAfter = calculator.getNextMainOccurrenceTimestamp(reminder, thursdayAfterTime)
        val expectedTimeAfter = LocalDateTime.of(2025, 1, 13, 21, 30) // Monday 21:30
        
        assertEquals("Weekly reminder should find next week if current day time has passed", 
            expectedTimeAfter.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceAfter)
    }
    
    @Test
    fun testFortnightlySchedule() {
        val schedule = ReminderSchedule.Fortnightly(LocalDate.of(2025, 1, 1)) // Wednesday Jan 1, 2025
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test on Monday before first Wednesday (should find Wednesday of week 2 since week 0 is in the past)
        val mondayTime = LocalDateTime.of(2025, 1, 6, 20, 0) // Monday 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, mondayTime)
        val expectedTime = LocalDateTime.of(2025, 1, 15, 21, 30) // Wednesday Jan 15 (week 2)
        
        assertEquals("Fortnightly reminder should find next Wednesday in even week", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
        
        // Test on Thursday after first Wednesday (should find Wednesday of week 2)
        val thursdayTime = LocalDateTime.of(2025, 1, 2, 20, 0) // Thursday 20:00
        val nextOccurrenceThursday = calculator.getNextMainOccurrenceTimestamp(reminder, thursdayTime)
        val expectedTimeThursday = LocalDateTime.of(2025, 1, 15, 21, 30) // Wednesday Jan 15 (week 2)
        
        assertEquals("Fortnightly reminder should find next Wednesday in even week", 
            expectedTimeThursday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceThursday)
    }
    
    @Test
    fun testMonthlyScheduleByDayOfMonth() {
        val schedule = ReminderSchedule.Monthly(dayOfMonth = 15)
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test on January 10th (should find January 15th)
        val jan10Time = LocalDateTime.of(2025, 1, 10, 20, 0) // Jan 10 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, jan10Time)
        val expectedTime = LocalDateTime.of(2025, 1, 15, 21, 30) // Jan 15 21:30
        
        assertEquals("Monthly reminder should find 15th of current month", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
        
        // Test on January 16th (should find February 15th)
        val jan16Time = LocalDateTime.of(2025, 1, 16, 20, 0) // Jan 16 20:00
        val nextOccurrenceAfter = calculator.getNextMainOccurrenceTimestamp(reminder, jan16Time)
        val expectedTimeAfter = LocalDateTime.of(2025, 2, 15, 21, 30) // Feb 15 21:30
        
        assertEquals("Monthly reminder should find 15th of next month", 
            expectedTimeAfter.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceAfter)
    }
    
    @Test
    fun testMonthlyScheduleByNthWeekday() {
        val schedule = ReminderSchedule.Monthly(
            dayOfWeek = DayOfWeek.TUESDAY,
            weekOfMonth = 3
        )
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test on January 10th (should find 3rd Tuesday of January)
        val jan10Time = LocalDateTime.of(2025, 1, 10, 20, 0) // Jan 10 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, jan10Time)
        val expectedTime = LocalDateTime.of(2025, 1, 21, 21, 30) // 3rd Tuesday of Jan
        
        assertEquals("Monthly reminder should find 3rd Tuesday of current month", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
        
        // Test on January 22nd (should find 3rd Tuesday of February)
        val jan22Time = LocalDateTime.of(2025, 1, 22, 20, 0) // Jan 22 20:00
        val nextOccurrenceAfter = calculator.getNextMainOccurrenceTimestamp(reminder, jan22Time)
        val expectedTimeAfter = LocalDateTime.of(2025, 2, 18, 21, 30) // 3rd Tuesday of Feb
        
        assertEquals("Monthly reminder should find 3rd Tuesday of next month", 
            expectedTimeAfter.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrenceAfter)
    }
    
    @Test
    fun testMonthlyScheduleEdgeCaseFebruary30th() {
        val schedule = ReminderSchedule.Monthly(dayOfMonth = 30)
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test on January 31st (should find March 30th since Feb 30th doesn't exist)
        val jan31Time = LocalDateTime.of(2025, 1, 31, 20, 0) // Jan 31 20:00
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, jan31Time)
        val expectedTime = LocalDateTime.of(2025, 3, 30, 21, 30) // Mar 30 21:30 (next valid 30th)
        
        assertEquals("Monthly reminder should handle non-existent days gracefully", 
            expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 
            nextOccurrence)
    }
    
    @Test
    fun testInvalidMonthlySchedule() {
        val schedule = ReminderSchedule.Monthly() // Invalid: no dayOfMonth or dayOfWeek/weekOfMonth
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        val currentTime = LocalDateTime.of(2025, 1, 1, 20, 0)
        val nextOccurrence = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)
        
        assertEquals("Invalid monthly schedule should return Long.MAX_VALUE", 
            Long.MAX_VALUE, 
            nextOccurrence)
    }

    @Test
    fun `daily reminder - same day future time`() {
        val reminder = Reminder(
            id = 1L,
            name = "Daily Test",
            time = LocalTime.of(18, 0),
            schedule = ReminderSchedule.Daily
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 12, 0) // 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 15, 18, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `daily reminder - same day past time`() {
        val reminder = Reminder(
            id = 1L,
            name = "Daily Test",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 12, 0) // 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 16, 8, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `weekly reminder - next occurrence this week`() {
        val reminder = Reminder(
            id = 1L,
            name = "Weekly Test",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 8, 0) // Wednesday 8:00 AM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 15, 9, 0) // Wednesday 9:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `weekly reminder - next occurrence next week`() {
        val reminder = Reminder(
            id = 1L,
            name = "Weekly Test",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 10, 0) // Wednesday 10:00 AM (past reminder time)

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 17, 9, 0) // Friday 9:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `weekly reminder - wraps to next week`() {
        val reminder = Reminder(
            id = 1L,
            name = "Weekly Test",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY))
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 10, 0) // Wednesday 10:00 AM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 20, 9, 0) // Next Monday 9:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `fortnightly reminder - correct week`() {
        val reminder = Reminder(
            id = 1L,
            name = "Fortnightly Test",
            time = LocalTime.of(14, 0),
            schedule = ReminderSchedule.Fortnightly(LocalDate.of(2025, 1, 1)) // Wednesday
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 12, 0) // Wednesday 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 29, 14, 0) // Next fortnightly Wednesday 2:00 PM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `fortnightly reminder - wrong week`() {
        val reminder = Reminder(
            id = 1L,
            name = "Fortnightly Test",
            time = LocalTime.of(14, 0),
            schedule = ReminderSchedule.Fortnightly(LocalDate.of(2025, 1, 1)) // Wednesday
        )
        val currentTime = LocalDateTime.of(2025, 1, 22, 12, 0) // Next Wednesday 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 29, 14, 0) // Week after next Wednesday 2:00 PM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by day of month - valid day`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Monthly(dayOfMonth = 15)
        )
        val currentTime = LocalDateTime.of(2025, 1, 10, 8, 0) // January 10th 8:00 AM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 15, 10, 0) // January 15th 10:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by day of month - past time this month`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Monthly(dayOfMonth = 15)
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 12, 0) // January 15th 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 2, 15, 10, 0) // February 15th 10:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by day of month - invalid day (February 30th)`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Monthly(dayOfMonth = 30)
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 8, 0) // January 15th 8:00 AM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 30, 10, 0) // January 30th 10:00 AM
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by day of month - invalid day in February`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Monthly(dayOfMonth = 30)
        )
        val currentTime = LocalDateTime.of(2025, 2, 15, 8, 0) // February 15th 8:00 AM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 3, 1, 10, 0) // March 1st 10:00 AM (next month)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by nth weekday - 3rd Wednesday`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(16, 0),
            schedule = ReminderSchedule.Monthly(dayOfWeek = DayOfWeek.WEDNESDAY, weekOfMonth = 3)
        )
        val currentTime = LocalDateTime.of(2025, 1, 10, 12, 0) // January 10th 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 15, 16, 0) // 3rd Wednesday of January
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by nth weekday - past time this month`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(16, 0),
            schedule = ReminderSchedule.Monthly(dayOfWeek = DayOfWeek.WEDNESDAY, weekOfMonth = 3)
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 18, 0) // January 15th 6:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 2, 19, 16, 0) // 3rd Wednesday of February
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly by nth weekday - 5th week doesn't exist`() {
        val reminder = Reminder(
            id = 1L,
            name = "Monthly Test",
            time = LocalTime.of(16, 0),
            schedule = ReminderSchedule.Monthly(dayOfWeek = DayOfWeek.WEDNESDAY, weekOfMonth = 5)
        )
        val currentTime = LocalDateTime.of(2025, 1, 10, 12, 0) // January 10th 12:00 PM

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        val expected = LocalDateTime.of(2025, 1, 29, 16, 0) // 5th Wednesday of January
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `monthly invalid schedule returns max value`() {
        val reminder = Reminder(
            id = 1L,
            name = "Invalid Monthly Test",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Monthly() // No day specified
        )
        val currentTime = LocalDateTime.of(2025, 1, 15, 8, 0)

        val result = calculator.getNextMainOccurrenceTimestamp(reminder, currentTime)

        assertEquals(Long.MAX_VALUE, result)
    }
} 