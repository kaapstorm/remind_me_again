package com.kaapstorm.remindmeagain.ui.screens.showreminder

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.model.DismissAction
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ShowReminderViewModelTest {

    private lateinit var repository: ReminderRepository
    private lateinit var schedulingService: ReminderSchedulingService
    private lateinit var viewModel: ShowReminderViewModel
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var nextOccurrenceCalculator: NextOccurrenceCalculator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        schedulingService = mockk()
        reminderScheduler = mockk(relaxed = true)
        nextOccurrenceCalculator = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `delete reminder shows dialog when ShowDeleteDialog intent is sent`() = runTest {
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L
        
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Initially dialog should not be shown
        assertFalse(viewModel.state.value.showDeleteDialog)
        
        // Send ShowDeleteDialog intent
        viewModel.handleIntent(ShowReminderIntent.ShowDeleteDialog)
        
        // Dialog should now be shown
        assertTrue(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `delete reminder hides dialog when HideDeleteDialog intent is sent`() = runTest {
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L
        
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Show dialog first
        viewModel.handleIntent(ShowReminderIntent.ShowDeleteDialog)
        assertTrue(viewModel.state.value.showDeleteDialog)
        
        // Hide dialog
        viewModel.handleIntent(ShowReminderIntent.HideDeleteDialog)
        
        // Dialog should now be hidden
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `delete reminder calls repository deleteReminder and sets isDeleted to true`() = runTest {
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        coEvery { repository.deleteReminder(reminderId) } returns Unit
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L
        
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Initially not deleted
        assertFalse(viewModel.state.value.isDeleted)
        
        // Send DeleteReminder intent
        viewModel.handleIntent(ShowReminderIntent.DeleteReminder)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify repository method was called
        coVerify { repository.deleteReminder(reminderId) }
        coVerify { reminderScheduler.cancelReminder(reminderId) }
        
        // Verify state is updated
        assertTrue(viewModel.state.value.isDeleted)
        assertFalse(viewModel.state.value.showDeleteDialog)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `delete reminder handles error gracefully`() = runTest {
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        val errorMessage = "Delete failed"
        
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        coEvery { repository.deleteReminder(reminderId) } throws RuntimeException(errorMessage)
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L
        
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Send DeleteReminder intent
        viewModel.handleIntent(ShowReminderIntent.DeleteReminder)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error is handled
        assertEquals(errorMessage, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isDeleted)
        assertFalse(viewModel.state.value.isProcessing)
    }

    @Test
    fun `loading reminder works correctly`() = runTest {
        val reminderId = 1L
        val now = java.time.LocalDateTime.now()
        val reminderTime = now.toLocalTime().plusMinutes(30) // within the next hour
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = reminderTime, schedule = ReminderSchedule.Daily)
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        every { schedulingService.isReminderActive(any(), any()) } returns true
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L

        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state is loaded correctly
        assertEquals(reminder, viewModel.state.value.reminder)
        assertTrue(viewModel.state.value.isDue)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.lastDismissAction)
    }

    @Test
    fun `last dismissed info is shown when DismissAction exists`() = runTest {
        val reminderId = 2L
        val now = java.time.LocalDateTime.now()
        val reminderTime = now.toLocalTime().plusMinutes(30)
        val reminder = Reminder(id = reminderId, name = "Test Reminder 2", time = reminderTime, schedule = ReminderSchedule.Daily)
        val dismissAction = DismissAction(reminderId = reminderId, timestamp = java.time.Instant.now())
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(listOf(dismissAction))
        every { schedulingService.isReminderActive(any(), any()) } returns true
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L

        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(dismissAction, viewModel.state.value.lastDismissAction)
    }

    @Test
    fun `should dismiss reminder successfully`() = runTest {
        // Given
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(emptyList())
        coEvery { repository.insertDismissAction(any()) } returns 1L
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L

        // When
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        viewModel.handleIntent(ShowReminderIntent.DismissReminder)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.isDismissed)
        assertFalse(viewModel.state.value.isProcessing)
        coVerify { repository.insertDismissAction(any()) }
    }

    @Test
    fun `should dismiss reminder successfully with DismissAction`() = runTest {
        // Given
        val reminderId = 1L
        val reminder = Reminder(id = reminderId, name = "Test Reminder", time = LocalTime.of(10, 0), schedule = ReminderSchedule.Daily)
        val dismissAction = DismissAction(reminderId = reminderId, timestamp = Instant.now())
        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)
        coEvery { repository.getDismissActionsForReminder(reminderId) } returns flowOf(listOf(dismissAction))
        coEvery { repository.insertDismissAction(any()) } returns 1L
        every { schedulingService.isReminderActive(any(), any()) } returns false
        every { nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(any(), any()) } returns 123456789L

        // When
        viewModel = ShowReminderViewModel(reminderId, repository, schedulingService, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(dismissAction, viewModel.state.value.lastDismissAction)
    }
}