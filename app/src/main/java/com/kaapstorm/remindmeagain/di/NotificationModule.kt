package com.kaapstorm.remindmeagain.di

import android.app.AlarmManager
import android.content.Context
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import com.kaapstorm.remindmeagain.notifications.DataStoreSnoozeStateRepository
import com.kaapstorm.remindmeagain.notifications.RepeatSchedulingHelper
import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import com.kaapstorm.remindmeagain.notifications.SnoozeStateManager
import com.kaapstorm.remindmeagain.notifications.SnoozeStateRepository
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager
import org.koin.android.ext.koin.androidContext // For ReminderNotificationManager & ReminderScheduler
import org.koin.dsl.module

val notificationModule = module {
    single<SnoozeStateRepository> { DataStoreSnoozeStateRepository(androidContext()) }
    single { ReminderNotificationManager(androidContext(), get(), get()) }
    single { ReminderScheduler(androidContext(), get()) } // Will use AlarmManager only
    single { SnoozeStateManager(get()) } // Inject the repository
    single { RepeatSchedulingHelper(get(), get()) } // Inject SnoozeStateManager and NextOccurrenceCalculator
    single {
        val context = androidContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ExactAlarmPermissionManager(context, alarmManager)
    } // Exact alarm permission manager with explicit AlarmManager
}
