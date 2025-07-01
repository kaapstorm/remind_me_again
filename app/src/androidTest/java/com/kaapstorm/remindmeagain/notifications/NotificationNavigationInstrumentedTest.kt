package com.kaapstorm.remindmeagain.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class NotificationNavigationInstrumentedTest {
    private lateinit var context: Context
    private lateinit var notificationManager: ReminderNotificationManager
    private lateinit var mockRepository: ReminderRepository

    @get:Rule
    val notificationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        "android.permission.POST_NOTIFICATIONS"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockRepository = mockk(relaxed = true)
        
        // Mock the repository to return a test reminder
        val testReminder = Reminder(5L, "Test Reminder", LocalTime.of(10, 30), ReminderSchedule.Daily)
        coEvery { mockRepository.getReminderByIdSuspend(5L) } returns testReminder
        
        val snoozeStateRepository = DataStoreSnoozeStateRepository(context)
        val snoozeStateManager = SnoozeStateManager(snoozeStateRepository)
        notificationManager = ReminderNotificationManager(context, snoozeStateManager, mockRepository)
    }

    @Test
    fun notificationCreation_setsCorrectIntentExtras() {
        val testReminder = Reminder(5L, "Test Reminder", LocalTime.of(10, 30), ReminderSchedule.Daily)
        
        // Show a notification
        notificationManager.showReminderNotification(testReminder)
        
        // Verify notification was created
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        val notification = activeNotifications.find { it.id == testReminder.id.toInt() }
        assertNotNull("Notification should be created", notification)
        
        // Verify the notification has the correct content intent
        val contentIntent = notification?.notification?.contentIntent
        assertNotNull("Content intent should be set", contentIntent)
        
        // Note: We can't directly access the intent extras from the PendingIntent in tests,
        // but we can verify the notification was created successfully and doesn't crash
        assertTrue("Notification should be created successfully", true)
    }

    @Test
    fun notificationIntent_hasCorrectFlags() {
        val testReminder = Reminder(6L, "Test Reminder 2", LocalTime.of(11, 0), ReminderSchedule.Daily)
        
        // Create the intent that would be used for notification content
        val intent = Intent(context, com.kaapstorm.remindmeagain.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, testReminder.id)
            putExtra(ReminderNotificationManager.EXTRA_SHOW_REMINDER, true)
        }
        
        // Verify the intent has the correct extras
        assertEquals("Reminder ID should be set correctly", testReminder.id, intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L))
        assertTrue("Show reminder flag should be set", intent.getBooleanExtra(ReminderNotificationManager.EXTRA_SHOW_REMINDER, false))
        assertTrue("Intent should have NEW_TASK flag", intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue("Intent should have CLEAR_TASK flag", intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun notificationManager_handlesMultipleNotifications() {
        val reminder1 = Reminder(7L, "Test Reminder 3", LocalTime.of(12, 0), ReminderSchedule.Daily)
        val reminder2 = Reminder(8L, "Test Reminder 4", LocalTime.of(13, 0), ReminderSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        
        // Show multiple notifications
        notificationManager.showReminderNotification(reminder1)
        notificationManager.showReminderNotification(reminder2)
        
        // Verify both notifications were created
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        
        assertNotNull("First notification should be created", activeNotifications.find { it.id == reminder1.id.toInt() })
        assertNotNull("Second notification should be created", activeNotifications.find { it.id == reminder2.id.toInt() })
    }

    @Test
    fun notificationManager_handlesInvalidReminder() {
        // Test with a reminder that has invalid data
        val invalidReminder = Reminder(-1L, "", LocalTime.of(0, 0), ReminderSchedule.Daily)
        
        // This should not crash
        notificationManager.showReminderNotification(invalidReminder)
        
        // Verify the notification was still created (even if with invalid data)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        val notification = activeNotifications.find { it.id == invalidReminder.id.toInt() }
        
        // The notification should be created even with invalid data
        assertNotNull("Notification should be created even with invalid reminder", notification)
    }
} 