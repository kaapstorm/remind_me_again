package com.kaapstorm.remindmeagain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager
import com.kaapstorm.remindmeagain.ui.components.ExactAlarmPermissionHandler
import com.kaapstorm.remindmeagain.ui.navigation.AppNavigation
import com.kaapstorm.remindmeagain.ui.navigation.Screen
import com.kaapstorm.remindmeagain.ui.theme.RemindMeAgainTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val exactAlarmPermissionManager: ExactAlarmPermissionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindMeAgainTheme {
                val navController = rememberNavController()

                // Check exact alarm permission
                ExactAlarmPermissionHandler(
                    context = this,
                    exactAlarmPermissionManager = exactAlarmPermissionManager,
                    onPermissionGranted = {
                        // Permission granted, continue with normal app flow
                    },
                    onPermissionDenied = {
                        // Permission denied, but app can still function with limited snooze capability
                    }
                )

                // Handle notification navigation
                val reminderId = intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
                val showReminder = intent.getBooleanExtra(ReminderNotificationManager.EXTRA_SHOW_REMINDER, false)

                if (showReminder && reminderId != -1L) {
                    // Navigate to ShowReminderScreen immediately
                    navController.navigate(Screen.ShowReminder.createRoute(reminderId))
                }

                AppNavigation(navController)
            }
        }
    }
}
