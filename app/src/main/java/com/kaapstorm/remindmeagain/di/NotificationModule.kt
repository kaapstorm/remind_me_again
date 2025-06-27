package com.kaapstorm.remindmeagain.di

import com.kaapstorm.remindmeagain.notifications.ReminderNotificationManager
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    single { ReminderNotificationManager(androidContext()) }
    single { ReminderScheduler(androidContext()) }
}