package com.kaapstorm.remindmeagain.di

import android.app.AlarmManager
import android.content.Context
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import com.kaapstorm.remindmeagain.notifications.SnoozeStateManager
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager
import org.koin.android.ext.koin.androidContext // For ReminderNotificationManager & ReminderScheduler
import org.koin.dsl.module

val notificationModule = module {
    single { ReminderNotificationManager(androidContext(), get(), get()) } // Added SnoozeStateManager, ReminderRepository
    single { ReminderScheduler(androidContext(), get()) } // Context and ReminderRepository
    single { SnoozeStateManager(androidContext()) } // Pass application context
    single {
        val context = androidContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ExactAlarmPermissionManager(context, alarmManager)
    } // Exact alarm permission manager with explicit AlarmManager
}
