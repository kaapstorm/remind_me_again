package com.kaapstorm.remindmeagain.notifications

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnoozeLogicTest {

    private lateinit var repository: InMemorySnoozeStateRepository
    private lateinit var snoozeStateManager: SnoozeStateManager

    @Before
    fun setUp() {
        repository = InMemorySnoozeStateRepository()
        snoozeStateManager = SnoozeStateManager(repository)
    }

    @Test
    fun testInitialSnoozeInterval() = runTest {
        val reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )

        // Test initial snooze interval
        snoozeStateManager.setSnoozeAlarmInterval(reminder.id, SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS)
        
        val retrievedInterval = snoozeStateManager.getCompletedSnoozeInterval(reminder.id)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, retrievedInterval)
    }

    @Test
    fun testDoublingSnoozeInterval() = runTest {
        val reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )

        val previousInterval = 60
        snoozeStateManager.setSnoozeAlarmInterval(reminder.id, previousInterval)

        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            previousInterval,
            Long.MAX_VALUE, // No constraint
            System.currentTimeMillis()
        )

        assertEquals(120, nextInterval) // Should double
        assertTrue(shouldShowLater) // Should show later button
    }

    @Test
    fun testDoublingSnoozeIntervalWithConstraint() = runTest {
        val reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )

        val previousInterval = 60
        snoozeStateManager.setSnoozeAlarmInterval(reminder.id, previousInterval)

        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            previousInterval,
            System.currentTimeMillis() + 90000, // 90 seconds from now (less than 120)
            System.currentTimeMillis()
        )

        assertEquals(120, nextInterval) // Should still calculate the doubled interval
        assertFalse(shouldShowLater) // But should not show later button
    }

    @Test
    fun testClearSnoozeState() = runTest {
        val reminderId = 1L
        val storedInterval = 120

        // Set a snooze interval
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, storedInterval)
        
        // Verify it was stored
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(storedInterval, result)

        // Clear the state
        snoozeStateManager.clearSnoozeState(reminderId)

        // Should return default value
        val resultAfterClear = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, resultAfterClear)
    }

    @Test
    fun testGetCompletedSnoozeIntervalWithNoStoredValue() = runTest {
        val reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )

        // Don't set any value, should return default
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminder.id)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, result)
    }

    @Test
    fun testCalculateNextSnoozeIntervalForButtonWithNoConstraint() = runTest {
        val completedInterval = 60
        val nextMainDueTimestamp = Long.MAX_VALUE // No constraint
        val currentTime = System.currentTimeMillis()

        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedInterval,
            nextMainDueTimestamp,
            currentTime
        )

        assertEquals(120, nextInterval) // Should double
        assertTrue(shouldShowLater) // Should show later button
    }

    @Test
    fun testCalculateNextSnoozeIntervalForButtonWithConstraint() = runTest {
        val completedInterval = 60
        val currentTime = System.currentTimeMillis()
        val nextMainDueTimestamp = currentTime + 90000 // 90 seconds from now

        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedInterval,
            nextMainDueTimestamp,
            currentTime
        )

        assertEquals(120, nextInterval) // Should still calculate the doubled interval
        assertFalse(shouldShowLater) // But should not show later button
    }

    @Test
    fun testCalculateNextSnoozeIntervalForButtonWithExactConstraint() = runTest {
        val completedInterval = 60
        val currentTime = System.currentTimeMillis()
        val nextMainDueTimestamp = currentTime + 120000 // Exactly 120 seconds from now

        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            completedInterval,
            nextMainDueTimestamp,
            currentTime
        )

        assertEquals(120, nextInterval) // Should still calculate the doubled interval
        assertFalse(shouldShowLater) // But should not show later button (would exceed)
    }

    @Test
    fun testMultipleSnoozeIntervals() = runTest {
        var currentInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS
        val nextMainDueTimestamp = Long.MAX_VALUE // No constraint

        // First snooze: 60 -> 120
        val (nextInterval1, shouldShow1) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            currentInterval,
            nextMainDueTimestamp,
            System.currentTimeMillis()
        )
        assertEquals(120, nextInterval1)
        assertTrue(shouldShow1)

        // Second snooze: 120 -> 240
        currentInterval = nextInterval1
        val (nextInterval2, shouldShow2) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            currentInterval,
            nextMainDueTimestamp,
            System.currentTimeMillis()
        )
        assertEquals(240, nextInterval2)
        assertTrue(shouldShow2)

        // Third snooze: 240 -> 480
        currentInterval = nextInterval2
        val (nextInterval3, shouldShow3) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            currentInterval,
            nextMainDueTimestamp,
            System.currentTimeMillis()
        )
        assertEquals(480, nextInterval3)
        assertTrue(shouldShow3)
    }
}