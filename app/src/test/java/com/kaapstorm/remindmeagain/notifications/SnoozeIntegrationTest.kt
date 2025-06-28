package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import android.content.SharedPreferences
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.LocalTime

class SnoozeIntegrationTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var snoozeStateManager: SnoozeStateManager
    private lateinit var reminder: Reminder
    private lateinit var snoozeMap: MutableMap<String, Int>

    @Before
    fun setup() {
        stopKoin() // Ensure Koin is stopped before starting
        
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        reminderRepository = mockk(relaxed = true)
        snoozeMap = mutableMapOf()
        
        // Mock SharedPreferences with a mutable map for snooze state
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.getInt(any(), any()) } answers {
            val key = firstArg<String>()
            val def = secondArg<Int>()
            snoozeMap[key] ?: def
        }
        every { sharedPreferences.edit() } returns mockk(relaxed = true)
        every { sharedPreferences.edit().putInt(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<Int>()
            snoozeMap[key] = value
            mockk(relaxed = true) { every { apply() } returns Unit }
        }
        every { sharedPreferences.edit().remove(any()) } answers {
            val key = firstArg<String>()
            snoozeMap.remove(key)
            mockk(relaxed = true) { every { apply() } returns Unit }
        }
        every { sharedPreferences.edit().apply() } returns Unit
        
        snoozeStateManager = SnoozeStateManager(context)
        
        reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `complete snooze flow should work correctly`() {
        // Given - starting with no snooze state
        val reminderId = 1L
        val initialInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS

        // When - user taps "Later" for the first time
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, initialInterval)
        assertEquals(initialInterval, snoozeMap["snooze_interval_seconds_for_reminder_1"])

        // And - snooze completes and user taps "Later" again
        val doubledInterval = initialInterval * 2
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = initialInterval,
            reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
            currentTimeMillis = System.currentTimeMillis()
        )
        assertEquals(doubledInterval, nextInterval)
        assertEquals(true, shouldShowLater)

        // And - user taps "Later" again with doubled interval
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, doubledInterval)
        assertEquals(doubledInterval, snoozeMap["snooze_interval_seconds_for_reminder_1"])

        // And - user taps "Done"
        snoozeStateManager.clearSnoozeState(reminderId)
        assertEquals(null, snoozeMap["snooze_interval_seconds_for_reminder_1"])

        // Then - snooze state should be cleared
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, result) // Returns default when cleared
    }

    @Test
    fun `snooze constraint should prevent Later button when exceeding next main reminder`() {
        // Given - previous snooze was 60 seconds, next main reminder is in 90 seconds
        val previousInterval = 60
        val currentTime = System.currentTimeMillis()
        val nextMainReminderTime = currentTime + (90 * 1000L) // 90 seconds from now

        // When - calculating next interval with constraint
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = nextMainReminderTime,
            currentTimeMillis = currentTime
        )

        // Then - interval is calculated but button should be hidden
        assertEquals(120, nextInterval) // 60 * 2 = 120 seconds
        assertEquals(false, shouldShowLater) // Should not show because 120 > 90
    }

    @Test
    fun `snooze state should persist across multiple operations`() {
        // Given - reminder with snooze state
        val reminderId = 1L
        val storedInterval = 120
        snoozeMap["snooze_interval_seconds_for_reminder_1"] = storedInterval

        // When - retrieving the stored interval
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminderId)

        // Then - should return the stored value
        assertEquals(storedInterval, result)

        // And - when clearing the state
        snoozeStateManager.clearSnoozeState(reminderId)
        assertEquals(null, snoozeMap["snooze_interval_seconds_for_reminder_1"])

        // And - retrieving again after clearing
        val resultAfterClear = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, resultAfterClear)
    }

    @Test
    fun `snooze interval progression should follow doubling pattern`() {
        // Given - starting with default interval
        var currentInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS
        val currentTime = System.currentTimeMillis()
        val expectedIntervals = listOf(120, 240, 480, 960, 1920) // Doubling pattern

        // When - simulating multiple "Later" taps
        expectedIntervals.forEach { expectedInterval ->
            val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
                completedIntervalSeconds = currentInterval,
                reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
                currentTimeMillis = currentTime
            )
            
            // Then - interval should follow doubling pattern
            assertEquals(expectedInterval, nextInterval)
            assertEquals(true, shouldShowLater)
            
            currentInterval = nextInterval
        }
    }
}