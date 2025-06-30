package com.kaapstorm.remindmeagain.notifications

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class RepeatSchedulingHelperTest {

    private lateinit var helper: RepeatSchedulingHelper
    private lateinit var snoozeStateManager: SnoozeStateManager
    private lateinit var nextOccurrenceCalculator: NextOccurrenceCalculator
    private lateinit var snoozeStateRepository: InMemorySnoozeStateRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        snoozeStateRepository = InMemorySnoozeStateRepository()
        snoozeStateManager = SnoozeStateManager(snoozeStateRepository)
        nextOccurrenceCalculator = mockk()
        helper = RepeatSchedulingHelper(snoozeStateManager, nextOccurrenceCalculator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        snoozeStateRepository.clearAll()
    }

    @Test
    fun `should schedule repeat for initial notification`() = runTest {
        // Given
        val reminder = createTestReminder()
        val isRepeat = false
        val currentInterval = 60

        // When
        val decision = helper.shouldScheduleRepeat(reminder, isRepeat, currentInterval)

        // Then
        assertTrue(decision.shouldSchedule)
        assertEquals(currentInterval, decision.intervalSeconds)
        assertEquals("Initial notification - scheduling first repeat", decision.reason)
    }

    @Test
    fun `should schedule repeat for repeat notification when should show later`() = runTest {
        // Given
        val reminder = createTestReminder()
        val isRepeat = true
        val currentInterval = 120
        val completedInterval = 60
        val nextMainDueTimestamp = System.currentTimeMillis() + 3600000L // 1 hour from now

        // Set up the snooze state through the repository
        snoozeStateRepository.setSnoozeAlarmInterval(reminder.id, completedInterval)
        
        every { 
            nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) 
        } returns nextMainDueTimestamp

        // When
        val decision = helper.shouldScheduleRepeat(reminder, isRepeat, currentInterval)

        // Then
        assertTrue(decision.shouldSchedule)
        assertEquals(currentInterval, decision.intervalSeconds)
        assertEquals("Continue repeating with current interval", decision.reason)
    }

    @Test
    fun `should not schedule repeat when next main occurrence is due`() = runTest {
        // Given
        val reminder = createTestReminder()
        val isRepeat = true
        val currentInterval = 120
        val completedInterval = 60
        val nextMainDueTimestamp = System.currentTimeMillis() + 60000L // 1 minute from now (too close)

        // Set up the snooze state through the repository
        snoozeStateRepository.setSnoozeAlarmInterval(reminder.id, completedInterval)
        
        every { 
            nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) 
        } returns nextMainDueTimestamp

        // When
        val decision = helper.shouldScheduleRepeat(reminder, isRepeat, currentInterval)

        // Then
        assertFalse(decision.shouldSchedule)
        assertEquals(0, decision.intervalSeconds)
        assertEquals("Next main occurrence due - stopping repeats", decision.reason)
    }

    @Test
    fun `should not schedule repeat when snooze interval would exceed next main occurrence`() = runTest {
        // Given
        val reminder = createTestReminder()
        val isRepeat = true
        val currentInterval = 300 // 5 minutes
        val completedInterval = 240 // 4 minutes (doubled would be 8 minutes)
        val nextMainDueTimestamp = System.currentTimeMillis() + 300000L // 5 minutes from now

        // Set up the snooze state through the repository
        snoozeStateRepository.setSnoozeAlarmInterval(reminder.id, completedInterval)
        
        every { 
            nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) 
        } returns nextMainDueTimestamp

        // When
        val decision = helper.shouldScheduleRepeat(reminder, isRepeat, currentInterval)

        // Then
        assertFalse(decision.shouldSchedule)
        assertEquals(0, decision.intervalSeconds)
        assertEquals("Next main occurrence due - stopping repeats", decision.reason)
    }

    @Test
    fun `should handle no next main occurrence Long MAX_VALUE`() = runTest {
        // Given
        val reminder = createTestReminder()
        val isRepeat = true
        val currentInterval = 60
        val completedInterval = 30

        // Set up the snooze state through the repository
        snoozeStateRepository.setSnoozeAlarmInterval(reminder.id, completedInterval)
        
        every { 
            nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) 
        } returns Long.MAX_VALUE

        // When
        val decision = helper.shouldScheduleRepeat(reminder, isRepeat, currentInterval)

        // Then
        assertTrue(decision.shouldSchedule)
        assertEquals(currentInterval, decision.intervalSeconds)
        assertEquals("Continue repeating with current interval", decision.reason)
    }

    private fun createTestReminder(): Reminder {
        return Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(10, 0),
            schedule = ReminderSchedule.Daily
        )
    }
} 