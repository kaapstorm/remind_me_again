package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderWorkerIntegrationTest {

    private lateinit var context: Context
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var schedulingService: ReminderSchedulingService
    private lateinit var notificationManager: ReminderNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        reminderRepository = mockk(relaxed = true)
        schedulingService = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
    }

    @Test
    fun `ReminderWorker executes successfully with active reminder`() = runBlocking {
        // Given
        val testReminder = Reminder(
            id = 1L,
            name = "Test Reminder",
            time = LocalTime.now(),
            schedule = ReminderSchedule.Daily
        )
        
        coEvery { reminderRepository.getRemindersByTime(any()) } returns flowOf(listOf(testReminder))
        every { schedulingService.isReminderActive(any(), any(), any()) } returns true

        // When
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()

        // Then
        assert(result == ListenableWorker.Result.success())
    }

    @Test
    fun `ReminderWorker handles empty reminder list`() = runBlocking {
        // Given
        coEvery { reminderRepository.getRemindersByTime(any()) } returns flowOf(emptyList())

        // When
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()

        // Then
        assert(result == ListenableWorker.Result.success())
    }

    @Test
    fun `ReminderWorker retries on exception`() = runBlocking {
        // Given
        coEvery { reminderRepository.getRemindersByTime(any()) } throws RuntimeException("Test exception")

        // When
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()

        // Then
        assert(result == ListenableWorker.Result.retry())
    }
}