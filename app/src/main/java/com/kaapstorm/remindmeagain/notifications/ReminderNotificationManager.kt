package com.kaapstorm.remindmeagain.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kaapstorm.remindmeagain.MainActivity
import com.kaapstorm.remindmeagain.R
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.notifications.actions.DoneActionHandlerReceiver
import com.kaapstorm.remindmeagain.notifications.actions.SnoozeActionHandlerReceiver
import kotlinx.coroutines.runBlocking // For simple example, ideally use proper scope from caller

class ReminderNotificationManager(
    private val context: Context,
    private val snoozeStateManager: SnoozeStateManager, // Injected
    private val reminderRepository: ReminderRepository  // Injected (if needed directly here, or pass data)
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    fun showReminderNotification(
        reminder: Reminder,
        nextSnoozeIntervalSeconds: Int = SnoozeStateManager.DEFAULT_INITIAL_SNOOZE_SECONDS, // Interval for NEXT snooze if "Later" is tapped
        showLaterButton: Boolean = true, // Whether to show the "Later" button
        isSnoozedNotification: Boolean = false // Flag to know if this is from a snooze
    ) {
        // If this is the first notification for a main reminder instance (not a snooze), reset its snooze state
        if (!isSnoozedNotification) {
            snoozeStateManager.clearSnoozeState(reminder.id)
        }

        val notificationId = reminder.id.toInt() // Using reminder ID as notification ID

        // Intent for tapping the notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Or specific flags
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            // Add other extras if MainActivity needs to navigate to a specific reminder
        }
        val contentPendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val contentPendingIntent = PendingIntent.getActivity(context, notificationId, contentIntent, contentPendingIntentFlags)

        // "Done" action
        val doneIntent = Intent(context, DoneActionHandlerReceiver::class.java).apply {
            action = SnoozeStateManager.ACTION_DONE_REMINDER
            putExtra(SnoozeStateManager.EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000, // Ensure unique request code for this PendingIntent
            doneIntent,
            contentPendingIntentFlags // Re-using flags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // REPLACE with your actual icon
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(reminder.name)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights (if channel allows)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground, // REPLACE icon R.drawable.ic_done_icon
                context.getString(R.string.done),
                donePendingIntent)

        // "Later" action - only add if showLaterButton is true
        if (showLaterButton) {
            val laterIntent = Intent(context, SnoozeActionHandlerReceiver::class.java).apply {
                action = SnoozeStateManager.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeStateManager.EXTRA_REMINDER_ID, reminder.id)
                putExtra(SnoozeStateManager.EXTRA_SNOOZE_INTERVAL_SECONDS, nextSnoozeIntervalSeconds)
            }
            val laterPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2000, // Ensure unique request code
                laterIntent,
                contentPendingIntentFlags // Re-using flags
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground, // REPLACE icon R.drawable.ic_later_icon
                context.getString(R.string.later),
                laterPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            enableLights(true)
            // setShowBadge(true) // Optional: show a badge on app icon
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SHOW_REMINDER = "show_reminder"
    }
}