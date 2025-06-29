package com.kaapstorm.remindmeagain.ui.screens.addeditreminder

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
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.LocalTime
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditReminderViewModelTest {

    private lateinit var repository: ReminderRepository
    private lateinit var viewModel: AddEditReminderViewModel
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var nextOccurrenceCalculator: NextOccurrenceCalculator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        reminderScheduler = mockk(relaxed = true)
        nextOccurrenceCalculator = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new reminder has default time of midday`() = runTest {
        // Given - creating a new reminder (reminderId = null)
        viewModel = AddEditReminderViewModel(reminderId = null, repository, reminderScheduler, nextOccurrenceCalculator)

        // When - checking initial state
        val state = viewModel.state.value

        // Then - time should be default 12:00 PM
        assertEquals(LocalTime.of(12, 0), state.time)
    }

    @Test
    fun `editing reminder loads correct time from repository`() = runTest {
        // Given - existing reminder with 10:30 AM time
        val reminderId = 1L
        val expectedTime = LocalTime.of(10, 30)
        val reminder = Reminder(
            id = reminderId,
            name = "Test Reminder",
            time = expectedTime,
            schedule = ReminderSchedule.Daily
        )

        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)

        // When - creating ViewModel for editing
        viewModel = AddEditReminderViewModel(reminderId = reminderId, repository, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - state should contain the loaded reminder's time
        val state = viewModel.state.value
        assertEquals(expectedTime, state.time)
        assertEquals("Test Reminder", state.name)
        assertEquals(ReminderSchedule.Daily, state.schedule)
        assertEquals(ScheduleType.DAILY, state.scheduleType)
    }

    @Test
    fun `editing reminder with different schedule types loads correct time`() = runTest {
        // Given - reminder with weekly schedule and afternoon time
        val reminderId = 2L
        val expectedTime = LocalTime.of(15, 45) // 3:45 PM
        val reminder = Reminder(
            id = reminderId,
            name = "Weekly Reminder",
            time = expectedTime,
            schedule = ReminderSchedule.Weekly(setOf())
        )

        coEvery { repository.getReminderById(reminderId) } returns flowOf(reminder)

        // When - creating ViewModel for editing
        viewModel = AddEditReminderViewModel(reminderId = reminderId, repository, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - time should be correctly loaded regardless of schedule type
        val state = viewModel.state.value
        assertEquals(expectedTime, state.time)
        assertEquals(ScheduleType.WEEKLY, state.scheduleType)
    }

    @Test
    fun `time updates correctly when changed via intent`() = runTest {
        // Given - new reminder with default time
        viewModel = AddEditReminderViewModel(reminderId = null, repository, reminderScheduler, nextOccurrenceCalculator)
        val newTime = LocalTime.of(14, 15) // 2:15 PM

        // When - updating time via intent
        viewModel.handleIntent(AddEditReminderIntent.UpdateTime(newTime))

        // Then - state should reflect the new time
        val state = viewModel.state.value
        assertEquals(newTime, state.time)
    }

    @Test
    fun `editing reminder handles repository errors gracefully`() = runTest {
        // Given - repository error when loading reminder
        val reminderId = 3L
        coEvery { repository.getReminderById(reminderId) } returns flowOf(null)

        // When - creating ViewModel for editing
        viewModel = AddEditReminderViewModel(reminderId = reminderId, repository, reminderScheduler, nextOccurrenceCalculator)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should maintain default state without crashing
        val state = viewModel.state.value
        assertEquals(LocalTime.of(12, 0), state.time) // Default time
        assertEquals("", state.name) // Default empty name
    }
}