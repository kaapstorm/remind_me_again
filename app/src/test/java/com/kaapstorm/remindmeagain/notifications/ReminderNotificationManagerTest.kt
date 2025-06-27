package com.kaapstorm.remindmeagain.notifications

import org.junit.Test
import org.junit.Assert.assertEquals

class ReminderNotificationManagerTest {

    @Test
    fun `EXTRA_REMINDER_ID constant has correct value`() {
        assertEquals("reminder_id", ReminderNotificationManager.EXTRA_REMINDER_ID)
    }

    @Test
    fun `EXTRA_SHOW_REMINDER constant has correct value`() {
        assertEquals("show_reminder", ReminderNotificationManager.EXTRA_SHOW_REMINDER)
    }
}