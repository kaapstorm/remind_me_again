package com.kaapstorm.remindmeagain.notifications

import org.junit.Test
import org.junit.Assert.assertEquals

class ReminderSchedulerTest {

    @Test
    fun `ReminderScheduler has correct constants`() {
        // Test ReminderWorker.WORK_NAME constant accessibility
        assertEquals("reminder_check", ReminderWorker.WORK_NAME)
    }
}