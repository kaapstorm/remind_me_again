package com.kaapstorm.remindmeagain.ui.screens.reminderlist

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class ReminderListViewModelTest {

    private lateinit var repository: ReminderRepository
    private lateinit var viewModel: ReminderListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load reminders successfully`() = runTest {
        // Given
        val reminders = listOf(
            Reminder(id = 1L, name = "Morning Meds", time = LocalTime.of(8, 0), schedule = ReminderSchedule.Daily),
            Reminder(id = 2L, name = "Evening Walk", time = LocalTime.of(18, 0), schedule = ReminderSchedule.Weekly(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)))
        )
        coEvery { repository.getAllReminders() } returns flowOf(reminders)

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(reminders, viewModel.state.value.reminders)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun `should handle empty reminder list`() = runTest {
        // Given
        coEvery { repository.getAllReminders() } returns flowOf(emptyList())

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.reminders.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun `should handle repository error`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { repository.getAllReminders() } returns kotlinx.coroutines.flow.flow { throw RuntimeException(errorMessage) }

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.reminders.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(errorMessage, viewModel.state.value.error)
    }

    @Test
    fun `should refresh reminders when LoadReminders intent is sent`() = runTest {
        // Given
        val initialReminders = listOf(
            Reminder(id = 1L, name = "Initial", time = LocalTime.of(8, 0), schedule = ReminderSchedule.Daily)
        )
        val updatedReminders = listOf(
            Reminder(id = 1L, name = "Updated", time = LocalTime.of(8, 0), schedule = ReminderSchedule.Daily),
            Reminder(id = 2L, name = "New", time = LocalTime.of(18, 0), schedule = ReminderSchedule.Weekly(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)))
        )
        
        coEvery { repository.getAllReminders() } returnsMany listOf(
            flowOf(initialReminders),
            flowOf(updatedReminders)
        )

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(initialReminders, viewModel.state.value.reminders)
        
        viewModel.handleIntent(ReminderListIntent.LoadReminders)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(updatedReminders, viewModel.state.value.reminders)
    }

    @Test
    fun `should delete reminder successfully`() = runTest {
        // Given
        val reminder = Reminder(id = 1L, name = "Test", time = LocalTime.of(8, 0), schedule = ReminderSchedule.Daily)
        coEvery { repository.getAllReminders() } returns flowOf(listOf(reminder))
        coEvery { repository.deleteReminder(reminder) } returns Unit

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.handleIntent(ReminderListIntent.DeleteReminder(reminder))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coEvery { repository.deleteReminder(reminder) }
    }

    @Test
    fun `should handle delete reminder error`() = runTest {
        // Given
        val reminder = Reminder(id = 1L, name = "Test", time = LocalTime.of(8, 0), schedule = ReminderSchedule.Daily)
        val errorMessage = "Delete failed"
        coEvery { repository.getAllReminders() } returns flowOf(listOf(reminder))
        coEvery { repository.deleteReminder(reminder) } throws RuntimeException(errorMessage)

        // When
        viewModel = ReminderListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.handleIntent(ReminderListIntent.DeleteReminder(reminder))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(errorMessage, viewModel.state.value.error)
    }
} 