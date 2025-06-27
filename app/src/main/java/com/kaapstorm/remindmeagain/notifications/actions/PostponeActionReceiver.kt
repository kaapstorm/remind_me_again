package com.kaapstorm.remindmeagain.notifications.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class PostponeActionReceiver : BroadcastReceiver(), KoinComponent {

    private val reminderRepository: ReminderRepository by inject()
    private val notificationManager: ReminderNotificationManager by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        scope.launch {
            try {
                // Get the last postpone action to determine next interval
                val postponeActions = reminderRepository.getPostponeActionsForReminder(reminderId).first()
                val lastPostpone = postponeActions.maxByOrNull { it.timestamp }
                
                // Double the interval each time, starting with 1 minute
                val previousInterval = lastPostpone?.intervalSeconds ?: 30 // Start with 30 seconds for testing
                val nextInterval = minOf(previousInterval * 2, 720) // Cap at 12 minutes for testing
                
                val action = PostponeAction(
                    reminderId = reminderId,
                    timestamp = Instant.now(),
                    intervalSeconds = nextInterval
                )
                
                reminderRepository.insertPostponeAction(action)
                notificationManager.cancelNotification(reminderId)
                
                // Schedule a new notification for later
                val reminder = reminderRepository.getReminderById(reminderId).first()
                if (reminder != null) {
                    reminderScheduler.schedulePostponeNotification(reminder, nextInterval)
                }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
}