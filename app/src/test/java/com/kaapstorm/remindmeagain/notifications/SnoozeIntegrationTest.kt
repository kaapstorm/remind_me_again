package com.kaapstorm.remindmeagain.notifications

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.fail

class SnoozeIntegrationTest {

    private lateinit var repository: InMemorySnoozeStateRepository
    private lateinit var snoozeStateManager: SnoozeStateManager

    @Before
    fun setUp() {
        repository = InMemorySnoozeStateRepository()
        snoozeStateManager = SnoozeStateManager(repository)
    }

    @Test
    fun testCompleteSnoozeFlow() = runTest {
        val reminderId = 1L
        val reminder = Reminder(
            id = reminderId,
            name = "Test Reminder",
            time = LocalTime.of(9, 0),
            schedule = ReminderSchedule.Daily
        )

        // Step 1: Initial snooze
        val initialInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, initialInterval)

        // Step 2: Calculate next interval for "Later" button
        val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
            initialInterval,
            Long.MAX_VALUE, // No constraint
            System.currentTimeMillis()
        )

        assertEquals(120, nextInterval) // Should double from 60 to 120
        assertEquals(true, shouldShowLater)

        // Step 3: Set the doubled interval
        val doubledInterval = nextInterval
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, doubledInterval)

        // Step 4: Clear snooze state (simulating "Done" action)
        snoozeStateManager.clearSnoozeState(reminderId)

        // Step 5: Verify state is cleared
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, result) // Returns default when cleared
    }

    @Test
    fun testSnoozeStatePersistence() = runTest {
        val reminderId = 1L
        val testInterval = 180 // 3 minutes

        // Set a snooze interval
        snoozeStateManager.setSnoozeAlarmInterval(reminderId, testInterval)

        // Verify it persists
        val result = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(testInterval, result)

        // Clear and verify it's gone
        snoozeStateManager.clearSnoozeState(reminderId)
        val resultAfterClear = snoozeStateManager.getCompletedSnoozeInterval(reminderId)
        assertEquals(SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, resultAfterClear)
    }

    @Test
    fun testMultipleSnoozeIntervals() = runTest {
        var currentInterval = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS
        val nextMainDueTimestamp = Long.MAX_VALUE // No constraint

        // Simulate multiple snooze actions
        repeat(4) { step ->
            val (nextInterval, shouldShowLater) = snoozeStateManager.calculateNextSnoozeIntervalForButton(
                currentInterval,
                nextMainDueTimestamp,
                System.currentTimeMillis()
            )

            // Verify doubling pattern: 60 -> 120 -> 240 -> 480 -> 960
            val expectedInterval = when (step) {
                0 -> 120  // 60 * 2
                1 -> 240  // 120 * 2
                2 -> 480  // 240 * 2
                3 -> 960  // 480 * 2
                else -> fail("Unexpected step")
            }

            assertEquals(expectedInterval, nextInterval)
            assertEquals(true, shouldShowLater)

            currentInterval = nextInterval
        }
    }
}