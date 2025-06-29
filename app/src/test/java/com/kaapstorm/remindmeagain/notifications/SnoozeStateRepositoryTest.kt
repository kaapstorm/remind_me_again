package com.kaapstorm.remindmeagain.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnoozeStateRepositoryTest {

    private lateinit var repository: InMemorySnoozeStateRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemorySnoozeStateRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should store and retrieve snooze state`() = runTest {
        // Given
        val reminderId = 1L
        val snoozeIntervalSeconds = 120

        // When
        repository.setSnoozeAlarmInterval(reminderId, snoozeIntervalSeconds)
        val retrieved = repository.getCompletedSnoozeInterval(reminderId)

        // Then
        assertEquals(snoozeIntervalSeconds, retrieved)
    }

    @Test
    fun `should return default interval for non-existent reminder`() = runTest {
        // Given
        val reminderId = 999L

        // When
        val result = repository.getCompletedSnoozeInterval(reminderId)

        // Then
        assertEquals(InMemorySnoozeStateRepository.DEFAULT_INITIAL_SNOOZE_SECONDS, result)
    }

    @Test
    fun `should clear snooze state`() = runTest {
        // Given
        val reminderId = 1L
        val snoozeIntervalSeconds = 120
        repository.setSnoozeAlarmInterval(reminderId, snoozeIntervalSeconds)

        // When
        repository.clearSnoozeState(reminderId)
        val result = repository.getCompletedSnoozeInterval(reminderId)

        // Then
        assertEquals(InMemorySnoozeStateRepository.DEFAULT_INITIAL_SNOOZE_SECONDS, result)
    }

    @Test
    fun `should handle multiple reminders independently`() = runTest {
        // Given
        val reminderId1 = 1L
        val reminderId2 = 2L
        val interval1 = 60
        val interval2 = 300

        // When
        repository.setSnoozeAlarmInterval(reminderId1, interval1)
        repository.setSnoozeAlarmInterval(reminderId2, interval2)

        // Then
        assertEquals(interval1, repository.getCompletedSnoozeInterval(reminderId1))
        assertEquals(interval2, repository.getCompletedSnoozeInterval(reminderId2))
    }

    @Test
    fun `should update existing snooze state`() = runTest {
        // Given
        val reminderId = 1L
        val initialInterval = 60
        val updatedInterval = 120

        // When
        repository.setSnoozeAlarmInterval(reminderId, initialInterval)
        repository.setSnoozeAlarmInterval(reminderId, updatedInterval)

        // Then
        assertEquals(updatedInterval, repository.getCompletedSnoozeInterval(reminderId))
    }

    @Test
    fun `should clear non-existent reminder without error`() = runTest {
        // Given
        val reminderId = 999L

        // When & Then (should not throw)
        repository.clearSnoozeState(reminderId)
        assertEquals(InMemorySnoozeStateRepository.DEFAULT_INITIAL_SNOOZE_SECONDS, repository.getCompletedSnoozeInterval(reminderId))
    }
} 