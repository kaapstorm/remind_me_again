package com.kaapstorm.remindmeagain.notifications.actions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.kaapstorm.remindmeagain.notifications.SnoozeStateManager
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SnoozeActionHandlerReceiver : BroadcastReceiver(), KoinComponent {

    private val snoozeStateManager: SnoozeStateManager by inject()
    private val exactAlarmPermissionManager: ExactAlarmPermissionManager by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(SnoozeStateManager.EXTRA_REMINDER_ID, -1L)
        // This is the interval that the upcoming alarm should be set for
        val intervalToScheduleNow = intent.getIntExtra(SnoozeStateManager.EXTRA_SNOOZE_INTERVAL_SECONDS, SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS)

        if (reminderId == -1L) {
            Log.e("SnoozeActionHandler", "Invalid reminderId received")
            return
        }

        Log.d("SnoozeActionHandler", "Snoozing reminder $reminderId for $intervalToScheduleNow seconds.")

        scope.launch {
            snoozeStateManager.setSnoozeAlarmInterval(reminderId, intervalToScheduleNow)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val showSnoozedIntent = Intent(context, ShowSnoozedNotificationReceiver::class.java).apply {
            action = SnoozeStateManager.ACTION_SHOW_SNOOZED_NOTIFICATION
            putExtra(SnoozeStateManager.EXTRA_REMINDER_ID, reminderId)
            // No need to pass interval here, ShowSnoozedNotificationReceiver will get it from SnoozeStateManager
        }

        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val showSnoozedPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(), // Use reminderId for unique PendingIntent per reminder
            showSnoozedIntent,
            pendingIntentFlags
        )

        val triggerAtMillis = System.currentTimeMillis() + (intervalToScheduleNow * 1000L)

        try {
            if (!exactAlarmPermissionManager.canScheduleExactAlarms()) {
                Log.w("SnoozeActionHandler", "Cannot schedule exact alarms. Snooze functionality may not work properly.")
                // Note: We could show a notification to the user here to guide them to settings
                // For now, we'll try to schedule the alarm anyway, but it might not work
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, showSnoozedPendingIntent)
        } catch (se: SecurityException) {
            Log.e("SnoozeActionHandler", "SecurityException on setting exact alarm. Check SCHEDULE_EXACT_ALARM permission.", se)
            // Handle error: Maybe show a toast or a different notification
            return
        }

        // Dismiss the current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt()) // Assuming notification ID is reminderId
    }
}
