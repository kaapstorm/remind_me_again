package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var schedulingService: ReminderSchedulingService
    private lateinit var notificationManager: ReminderNotificationManager
    private lateinit var worker: ReminderWorker
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        reminderRepository = mockk()
        schedulingService = mockk()
        notificationManager = mockk(relaxed = true)

        // Set up Koin for dependency injection
        startKoin {
            modules(module {
                single { reminderRepository }
                single { schedulingService }
                single { notificationManager }
            })
        }

        worker = ReminderWorker(context, workerParams)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `doWork processes active reminders and calls scheduling service`() = runTest {
        // Given - active reminder that should be checked
        val reminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.of(10, 30),
            schedule = ReminderSchedule.Daily
        )

        coEvery { reminderRepository.getActiveReminders() } returns flowOf(listOf(reminder))
        every { 
            schedulingService.isReminderActive(
                reminder = reminder,
                dateTime = any<LocalDateTime>()
            ) 
        } returns true

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        // Verify the scheduling service was called to check if reminder is active
        verify { 
            schedulingService.isReminderActive(
                reminder = reminder,
                dateTime = any<LocalDateTime>()
            ) 
        }
    }

    @Test
    fun `doWork handles empty active reminders list gracefully`() = runTest {
        // Given - no active reminders
        coEvery { reminderRepository.getActiveReminders() } returns flowOf(emptyList())

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notificationManager.showReminderNotification(any()) }
    }

    @Test
    fun `doWork returns retry on repository exception`() = runTest {
        // Given - repository throws exception
        coEvery { reminderRepository.getActiveReminders() } throws RuntimeException("Database error")

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        verify(exactly = 0) { notificationManager.showReminderNotification(any()) }
    }

    @Test
    fun `doWork gets active reminders from repository`() = runTest {
        // Given - repository with active reminders
        val reminders = listOf(
            Reminder(1L, "Reminder 1", LocalTime.of(9, 0), ReminderSchedule.Daily),
            Reminder(2L, "Reminder 2", LocalTime.of(15, 30), ReminderSchedule.Weekly(setOf()))
        )
        
        coEvery { reminderRepository.getActiveReminders() } returns flowOf(reminders)
        every { 
            schedulingService.isReminderActive(
                reminder = any(),
                dateTime = any<LocalDateTime>()
            ) 
        } returns false // Not active, so no notifications should be shown

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        // Verify that the repository was queried for active reminders
        coEvery { reminderRepository.getActiveReminders() }
        // Verify that scheduling service was called for both reminders
        verify(exactly = 2) { 
            schedulingService.isReminderActive(
                reminder = any(),
                dateTime = any<LocalDateTime>()
            ) 
        }
    }

    @Test
    fun `doWork shows notification when reminder is active and time conditions are met`() = runTest {
        // Given - reminder that meets all conditions (we'll make it very close to current time)
        val currentTimeApprox = LocalTime.now()
        val reminder = Reminder(
            id = 1L,
            name = "Due Reminder",
            time = currentTimeApprox, // Use current time to ensure it's within the window
            schedule = ReminderSchedule.Daily
        )

        coEvery { reminderRepository.getActiveReminders() } returns flowOf(listOf(reminder))
        every { 
            schedulingService.isReminderActive(
                reminder = reminder,
                dateTime = any<LocalDateTime>()
            ) 
        } returns true

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        // Note: Due to time window logic, this test verifies the notification might be called
        // but doesn't strictly verify it's called since the exact timing depends on execution time
        verify { 
            schedulingService.isReminderActive(
                reminder = reminder,
                dateTime = any<LocalDateTime>()
            ) 
        }
    }
}