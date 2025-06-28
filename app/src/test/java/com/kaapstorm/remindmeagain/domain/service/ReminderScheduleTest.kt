package com.kaapstorm.remindmeagain.data.model

import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

class ReminderScheduleTest {
    
    private lateinit var schedulingService: ReminderSchedulingService
    private val reminderTime = LocalTime.of(21, 0)
    private val reminderName = "Take out recycling"
    
    @Before
    fun setUp() {
        schedulingService = ReminderSchedulingService()
    }
    
    @Test
    fun testDailySchedule() {
        val schedule = ReminderSchedule.Daily
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test that reminder is returned on any day at 21:00
        val testDate = LocalDate.of(2025, 1, 1) // Wednesday
        val testDateTime = LocalDateTime.of(testDate, reminderTime)
        
        assertTrue("Daily reminder should be active on any day at 21:00", 
            schedulingService.isReminderActive(reminder, testDateTime))
        
        // Test that reminder is not returned at 20:45
        val testDateTimeBefore = LocalDateTime.of(testDate, LocalTime.of(20, 45))
        assertFalse("Daily reminder should not be active at 20:45", 
            schedulingService.isReminderActive(reminder, testDateTimeBefore))
    }
    
    @Test
    fun testWeeklySchedule() {
        val schedule = ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test that reminder is returned on Monday at 21:00
        val mondayDate = LocalDate.of(2025, 1, 6) // Monday
        val mondayDateTime = LocalDateTime.of(mondayDate, reminderTime)
        assertTrue("Weekly reminder should be active on Monday at 21:00", 
            schedulingService.isReminderActive(reminder, mondayDateTime))
        
        // Test that reminder is returned on Thursday at 21:00
        val thursdayDate = LocalDate.of(2025, 1, 9) // Thursday
        val thursdayDateTime = LocalDateTime.of(thursdayDate, reminderTime)
        assertTrue("Weekly reminder should be active on Thursday at 21:00", 
            schedulingService.isReminderActive(reminder, thursdayDateTime))
        
        // Test that reminder is not returned on Wednesday at 21:00 (day before Thursday)
        val wednesdayDate = LocalDate.of(2025, 1, 8) // Wednesday
        val wednesdayDateTime = LocalDateTime.of(wednesdayDate, reminderTime)
        assertFalse("Weekly reminder should not be active on Wednesday at 21:00", 
            schedulingService.isReminderActive(reminder, wednesdayDateTime))
        
        // Test that reminder is not returned at 20:45
        val mondayDateTimeBefore = LocalDateTime.of(mondayDate, LocalTime.of(20, 45))
        assertFalse("Weekly reminder should not be active at 20:45", 
            schedulingService.isReminderActive(reminder, mondayDateTimeBefore))
    }
    
    @Test
    fun testFortnightlySchedule() {
        val schedule = ReminderSchedule.Fortnightly(LocalDate.of(2025, 1, 1)) // Wednesday Jan 1, 2025
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test that reminder is returned on Wednesday January 1, 2025 (first occurrence)
        val firstWednesday = LocalDate.of(2025, 1, 1) // Wednesday
        val firstWednesdayDateTime = LocalDateTime.of(firstWednesday, reminderTime)
        assertTrue("Fortnightly reminder should be active on first Wednesday at 21:00", 
            schedulingService.isReminderActive(reminder, firstWednesdayDateTime))
        
        // Test that reminder is returned on Wednesday January 15, 2025 (second occurrence)
        val secondWednesday = LocalDate.of(2025, 1, 15) // Wednesday
        val secondWednesdayDateTime = LocalDateTime.of(secondWednesday, reminderTime)
        assertTrue("Fortnightly reminder should be active on second Wednesday at 21:00", 
            schedulingService.isReminderActive(reminder, secondWednesdayDateTime))
        
        // Test that reminder is not returned on Wednesday January 8, 2025 (middle week)
        val middleWednesday = LocalDate.of(2025, 1, 8) // Wednesday
        val middleWednesdayDateTime = LocalDateTime.of(middleWednesday, reminderTime)
        assertFalse("Fortnightly reminder should not be active on middle Wednesday at 21:00", 
            schedulingService.isReminderActive(reminder, middleWednesdayDateTime))
        
        // Test that reminder is not returned on Tuesday at 21:00 (day before Wednesday)
        val tuesdayDate = LocalDate.of(2025, 1, 7) // Tuesday
        val tuesdayDateTime = LocalDateTime.of(tuesdayDate, reminderTime)
        assertFalse("Fortnightly reminder should not be active on Tuesday at 21:00", 
            schedulingService.isReminderActive(reminder, tuesdayDateTime))
        
        // Test that reminder is not returned at 20:45
        val firstWednesdayDateTimeBefore = LocalDateTime.of(firstWednesday, LocalTime.of(20, 45))
        assertFalse("Fortnightly reminder should not be active at 20:45", 
            schedulingService.isReminderActive(reminder, firstWednesdayDateTimeBefore))
    }
    
    @Test
    fun testMonthlySchedule() {
        val schedule = ReminderSchedule.Monthly(
            dayOfWeek = DayOfWeek.TUESDAY,
            weekOfMonth = 3
        )
        val reminder = Reminder(
            name = reminderName,
            time = reminderTime,
            schedule = schedule
        )
        
        // Test that reminder is returned on third Tuesday of January 2025 at 21:00
        val thirdTuesday = LocalDate.of(2025, 1, 21) // Third Tuesday of January 2025
        val thirdTuesdayDateTime = LocalDateTime.of(thirdTuesday, reminderTime)
        assertTrue("Monthly reminder should be active on third Tuesday at 21:00", 
            schedulingService.isReminderActive(reminder, thirdTuesdayDateTime))
        
        // Test that reminder is returned on third Tuesday of February 2025 at 21:00
        val thirdTuesdayFeb = LocalDate.of(2025, 2, 18) // Third Tuesday of February 2025
        val thirdTuesdayFebDateTime = LocalDateTime.of(thirdTuesdayFeb, reminderTime)
        assertTrue("Monthly reminder should be active on third Tuesday of February at 21:00", 
            schedulingService.isReminderActive(reminder, thirdTuesdayFebDateTime))
        
        // Test that reminder is not returned on second Tuesday at 21:00 (week before)
        val secondTuesday = LocalDate.of(2025, 1, 14) // Second Tuesday of January 2025
        val secondTuesdayDateTime = LocalDateTime.of(secondTuesday, reminderTime)
        assertFalse("Monthly reminder should not be active on second Tuesday at 21:00", 
            schedulingService.isReminderActive(reminder, secondTuesdayDateTime))
        
        // Test that reminder is not returned on Monday at 21:00 (day before Tuesday)
        val mondayDate = LocalDate.of(2025, 1, 20) // Monday before third Tuesday
        val mondayDateTime = LocalDateTime.of(mondayDate, reminderTime)
        assertFalse("Monthly reminder should not be active on Monday at 21:00", 
            schedulingService.isReminderActive(reminder, mondayDateTime))
        
        // Test that reminder is not returned at 20:45
        val thirdTuesdayDateTimeBefore = LocalDateTime.of(thirdTuesday, LocalTime.of(20, 45))
        assertFalse("Monthly reminder should not be active at 20:45", 
            schedulingService.isReminderActive(reminder, thirdTuesdayDateTimeBefore))
    }
}
