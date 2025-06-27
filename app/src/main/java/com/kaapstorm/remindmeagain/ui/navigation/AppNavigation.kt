package com.kaapstorm.remindmeagain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaapstorm.remindmeagain.ui.screens.reminderlist.ReminderListScreen
import com.kaapstorm.remindmeagain.ui.screens.addeditreminder.AddEditReminderScreen
import com.kaapstorm.remindmeagain.ui.screens.showreminder.ShowReminderScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ReminderList.route
    ) {
        composable(Screen.ReminderList.route) {
            ReminderListScreen(
                onAddReminder = {
                    navController.navigate(Screen.AddReminder.route)
                },
                onEditReminder = { reminderId ->
                    navController.navigate(Screen.EditReminder.createRoute(reminderId))
                }
            )
        }
        
        composable(Screen.AddReminder.route) {
            AddEditReminderScreen(
                reminderId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditReminder.route,
            arguments = Screen.EditReminder.arguments
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: 0L
            AddEditReminderScreen(
                reminderId = reminderId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ShowReminder.route,
            arguments = Screen.ShowReminder.arguments
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: 0L
            ShowReminderScreen(
                reminderId = reminderId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}