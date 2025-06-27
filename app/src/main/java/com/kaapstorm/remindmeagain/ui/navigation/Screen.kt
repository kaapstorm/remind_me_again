package com.kaapstorm.remindmeagain.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object ReminderList : Screen("reminder_list")
    object AddReminder : Screen("add_reminder")
    
    object EditReminder : Screen("edit_reminder/{reminderId}") {
        val arguments = listOf(
            navArgument("reminderId") { type = NavType.LongType }
        )
        
        fun createRoute(reminderId: Long): String {
            return "edit_reminder/$reminderId"
        }
    }
    
    object ShowReminder : Screen("show_reminder/{reminderId}") {
        val arguments = listOf(
            navArgument("reminderId") { type = NavType.LongType }
        )
        
        fun createRoute(reminderId: Long): String {
            return "show_reminder/$reminderId"
        }
    }
}