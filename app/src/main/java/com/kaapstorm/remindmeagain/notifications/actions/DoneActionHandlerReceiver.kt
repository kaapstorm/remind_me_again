package com.kaapstorm.remindmeagain.notifications.actions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kaapstorm.remindmeagain.notifications.SnoozeStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DoneActionHandlerReceiver : BroadcastReceiver(), KoinComponent {

    private val snoozeStateManager: SnoozeStateManager by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(SnoozeStateManager.EXTRA_REMINDER_ID, -1L)

        if (reminderId == -1L) {
            Log.e("DoneActionHandler", "Invalid reminderId")
            return
        }
        Log.d("DoneActionHandler", "Reminder $reminderId marked as done (notification dismissed).")

        scope.launch {
            snoozeStateManager.clearSnoozeState(reminderId)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt()) // Assuming notification ID is reminderId
    }
}