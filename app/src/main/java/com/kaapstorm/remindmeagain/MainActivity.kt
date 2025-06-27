package com.kaapstorm.remindmeagain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.ui.navigation.AppNavigation
import com.kaapstorm.remindmeagain.ui.navigation.Screen
import com.kaapstorm.remindmeagain.ui.theme.RemindMeAgainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindMeAgainTheme {
                val navController = rememberNavController()
                
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
