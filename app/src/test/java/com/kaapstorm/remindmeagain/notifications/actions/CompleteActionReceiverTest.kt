package com.kaapstorm.remindmeagain.notifications.actions

import android.content.Context
import android.content.Intent
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class CompleteActionReceiverTest {

    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var receiver: CompleteActionReceiver

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        intent = mockk(relaxed = true)
        receiver = CompleteActionReceiver()
    }

    @Test
    fun `onReceive with valid reminder ID executes without error`() {
        // Given
        val reminderId = 123L
        every { intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L) } returns reminderId

        // When & Then - verify no exception is thrown
        receiver.onReceive(context, intent)
    }

    @Test
    fun `onReceive with invalid reminder ID does nothing`() {
        // Given
        every { intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L) } returns -1L

        // When & Then - verify no exception is thrown
        receiver.onReceive(context, intent)
    }
}