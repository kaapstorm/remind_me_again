package com.kaapstorm.remindmeagain.notifications.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaapstorm.remindmeagain.data.model.CompleteAction
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class CompleteActionReceiver : BroadcastReceiver(), KoinComponent {

    private val reminderRepository: ReminderRepository by inject()
    private val notificationManager: ReminderNotificationManager by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        scope.launch {
            try {
                val action = CompleteAction(
                    reminderId = reminderId,
                    timestamp = Instant.now()
                )
                reminderRepository.insertCompleteAction(action)
                notificationManager.cancelNotification(reminderId)
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
}