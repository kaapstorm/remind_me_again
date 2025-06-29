package com.kaapstorm.remindmeagain.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository

/**
 * Schedules and manages reminder and repeat alarms using AlarmManager.
 */
class ReminderScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule a reminder to trigger at the specified time (in millis).
     * This is used for the initial due notification.
     */
    fun scheduleReminder(reminder: Reminder, triggerAtMillis: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    /**
     * Cancel a scheduled reminder alarm.
     */
    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Schedule a repeat alarm for a reminder (e.g., for snooze/repeat interval).
     * This will trigger a broadcast to ReminderAlarmReceiver.
     */
    fun scheduleRepeat(reminderId: Long, intervalSeconds: Int) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_REPEAT, true)
            putExtra(ReminderAlarmReceiver.EXTRA_REPEAT_INTERVAL_SECONDS, intervalSeconds)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 10000).toInt(), // Use a different request code for repeat
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + intervalSeconds * 1000L
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    /**
     * Cancel a scheduled repeat alarm for a reminder.
     */
    fun cancelRepeat(reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Reschedule all reminders and repeats from the repository and snooze state.
     * Call this on app startup or after device reboot.
     */
    suspend fun rescheduleAllReminders() {
        // TODO: Query all reminders and snooze state, and reschedule alarms as needed
    }

    fun startPeriodicReminderCheck() {
        // TODO: Implement
    }

    fun stopPeriodicReminderCheck() {
        // TODO: Implement
    }

    fun schedulePostponeNotification(reminder: Reminder, intervalSeconds: Int) {
        // TODO: Implement
    }

    fun cancelPostponeNotification(reminderId: Long) {
        // TODO: Implement
    }

    fun triggerImmediateReminderCheck() {
        // TODO: Implement
    }
}