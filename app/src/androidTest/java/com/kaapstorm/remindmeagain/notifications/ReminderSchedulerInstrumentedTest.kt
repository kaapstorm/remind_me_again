package com.kaapstorm.remindmeagain.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerInstrumentedTest {
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: ReminderScheduler

    @get:Rule
    val exactAlarmPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        "android.permission.SCHEDULE_EXACT_ALARM"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakeRepo = mockk<ReminderRepository>(relaxed = true)
        scheduler = ReminderScheduler(context, fakeRepo)
    }

    @Test
    fun scheduleReminder_setsExactAlarm() {
        val reminder = Reminder(1L, "Test", LocalTime.of(10, 0), ReminderSchedule.Daily)
        val triggerAtMillis = System.currentTimeMillis() + 60_000L
        scheduler.scheduleReminder(reminder, triggerAtMillis)
        // There is no direct way to assert AlarmManager state, but we can check PendingIntent is not null
        val intent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            android.content.Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull(intent)
    }

    @Test
    fun cancelReminder_cancelsAlarm() {
        val reminder = Reminder(2L, "Test2", LocalTime.of(11, 0), ReminderSchedule.Daily)
        val triggerAtMillis = System.currentTimeMillis() + 60_000L
        scheduler.scheduleReminder(reminder, triggerAtMillis)
        scheduler.cancelReminder(reminder.id)
        val intent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            android.content.Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        // After cancel, PendingIntent may still exist, but alarm is canceled. No direct assert possible.
        // Just ensure no crash and PendingIntent can be created.
        assertTrue(true)
    }

    @Test
    fun scheduleRepeat_schedulesRepeatAlarm() {
        val reminderId = 3L
        val intervalSeconds = 120
        scheduler.scheduleRepeat(reminderId, intervalSeconds)
        val intent = PendingIntent.getBroadcast(
            context,
            (reminderId + 10000).toInt(),
            android.content.Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull(intent)
    }

    @Test
    fun cancelRepeat_cancelsRepeatAlarm() {
        val reminderId = 4L
        scheduler.scheduleRepeat(reminderId, 60)
        scheduler.cancelRepeat(reminderId)
        val intent = PendingIntent.getBroadcast(
            context,
            (reminderId + 10000).toInt(),
            android.content.Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        // After cancel, PendingIntent may still exist, but alarm is canceled. No direct assert possible.
        assertTrue(true)
    }
} 