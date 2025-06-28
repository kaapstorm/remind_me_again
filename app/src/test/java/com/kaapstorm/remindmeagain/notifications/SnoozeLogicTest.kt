package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import android.content.SharedPreferences
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class SnoozeLogicTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var snoozeStateManager: SnoozeStateManager
    private lateinit var reminder: Reminder

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        reminderRepository = mockk(relaxed = true)

        // Mock SharedPreferences
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns mockk(relaxed = true)
        every { sharedPreferences.edit().putInt(any(), any()) } returns mockk(relaxed = true)
        every { sharedPreferences.edit().remove(any()) } returns mockk(relaxed = true)
        every { sharedPreferences.edit().apply() } returns Unit

        snoozeStateManager = SnoozeStateManager(context)

        reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )
    }

    @Test
    fun `first Later tap should snooze by default interval of 1 minute`() {
        // Given - no previous snooze state
        every { sharedPreferences.getInt(any(), SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS

        // When - user taps "Later" for the first time
        snoozeStateManager.setSnoozeAlarmInterval(reminder.id, SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS)

        // Then - verify the interval is stored
        verify { sharedPreferences.edit().putInt("snooze_interval_seconds_for_reminder_1", 60) }
    }

    @Test
    fun `subsequent Later taps should double the interval`() {
        // Given - previous snooze was 60 seconds
        val previousInterval = 60
        every { sharedPreferences.getInt(any(), SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns previousInterval

        // When - calculating next interval
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
            currentTimeMillis = System.currentTimeMillis()
        )

        // Then - interval should be doubled
        assertEquals(120, nextInterval) // 60 * 2 = 120 seconds
        assertTrue(shouldShowLater)
    }

    @Test
    fun `interval doubling continues with each Later tap`() {
        // Given - previous snooze was 120 seconds
        val previousInterval = 120
        every { sharedPreferences.getInt(any(), SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns previousInterval

        // When - calculating next interval
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
            currentTimeMillis = System.currentTimeMillis()
        )

        // Then - interval should be doubled again
        assertEquals(240, nextInterval) // 120 * 2 = 240 seconds
        assertTrue(shouldShowLater)
    }

    @Test
    fun `Done action should clear snooze state`() {
        // Given - reminder has snooze state
        val reminderId = 1L

        // When - user taps "Done"
        snoozeStateManager.clearSnoozeState(reminderId)

        // Then - snooze state should be cleared
        verify { sharedPreferences.edit().remove("snooze_interval_seconds_for_reminder_1") }
    }

    @Test
    fun `main reminder notification should reset snooze state`() {
        // Given - reminder has existing snooze state
        val reminderId = 1L
        every { sharedPreferences.getInt(any(), SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns 120

        // When - main reminder notification is shown (not snoozed)
        snoozeStateManager.clearSnoozeState(reminderId)

        // Then - snooze state should be cleared
        verify { sharedPreferences.edit().remove("snooze_interval_seconds_for_reminder_1") }
    }

    @Test
    fun `getCompletedSnoozeInterval should return stored interval`() {
        // Given - stored interval is 120 seconds
        val storedInterval = 120
        every { sharedPreferences.getInt("snooze_interval_seconds_for_reminder_1", SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns storedInterval

        // When
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminder.id)

        // Then
        assertEquals(storedInterval, result)
    }

    @Test
    fun `getCompletedSnoozeInterval should return default when no stored value`() {
        // Given - no stored interval
        every { sharedPreferences.getInt(any(), SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS) } returns SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS

        // When
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminder.id)

        // Then
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, result)
    }

    @Test
    fun `Later button should be hidden when doubling exceeds next main reminder time`() {
        // Given - previous snooze was 60 seconds, next main reminder is in 90 seconds
        val previousInterval = 60
        val currentTime = System.currentTimeMillis()
        val nextMainReminderTime = currentTime + (90 * 1000L) // 90 seconds from now

        // When - calculating next interval
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = nextMainReminderTime,
            currentTimeMillis = currentTime
        )

        // Then - interval is calculated but button should be hidden
        assertEquals(120, nextInterval) // 60 * 2 = 120 seconds
        assertFalse(shouldShowLater) // Should not show because 120 > 90
    }

    @Test
    fun `Later button should be shown when doubling does not exceed next main reminder time`() {
        // Given - previous snooze was 30 seconds, next main reminder is in 90 seconds
        val previousInterval = 30
        val currentTime = System.currentTimeMillis()
        val nextMainReminderTime = currentTime + (90 * 1000L) // 90 seconds from now

        // When - calculating next interval
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = nextMainReminderTime,
            currentTimeMillis = currentTime
        )

        // Then - interval is calculated and button should be shown
        assertEquals(60, nextInterval) // 30 * 2 = 60 seconds
        assertTrue(shouldShowLater) // Should show because 60 < 90
    }

    @Test
    fun `Later button should be shown when no next main reminder constraint`() {
        // Given - previous snooze was 60 seconds, no next main reminder constraint
        val previousInterval = 60
        val currentTime = System.currentTimeMillis()

        // When - calculating next interval
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedIntervalSeconds = previousInterval,
            reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
            currentTimeMillis = currentTime
        )

        // Then - interval is calculated and button should be shown
        assertEquals(120, nextInterval) // 60 * 2 = 120 seconds
        assertTrue(shouldShowLater)
    }

    @Test
    fun `snooze interval progression follows doubling pattern`() {
        // Given - starting with default interval
        var currentInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS
        val currentTime = System.currentTimeMillis()

        // When - simulating multiple "Later" taps
        val intervals = mutableListOf<Int>()
        val shouldShowButtons = mutableListOf<Boolean>()

        repeat(5) { // Simulate 5 "Later" taps
            val (nextInterval, shouldShow) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
                completedIntervalSeconds = currentInterval,
                reminderNextMainDueTimestamp = Long.MAX_VALUE, // No constraint
                currentTimeMillis = currentTime
            )
            intervals.add(nextInterval)
            shouldShowButtons.add(shouldShow)
            currentInterval = nextInterval
        }

        // Then - intervals should follow doubling pattern: 60, 120, 240, 480, 960
        assertEquals(listOf(120, 240, 480, 960, 1920), intervals)
        assertTrue(shouldShowButtons.all { it }) // All should show when no constraint
    }
}