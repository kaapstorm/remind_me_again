package com.kaapstorm.remindmeagain

import android.app.Application
import com.kaapstorm.remindmeagain.di.dataModule
import com.kaapstorm.remindmeagain.di.domainModule
import com.kaapstorm.remindmeagain.di.notificationModule
import com.kaapstorm.remindmeagain.di.uiModule
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin

class RemindMeAgainApplication : Application() {

    private val reminderScheduler: ReminderScheduler by inject()
    private val exactAlarmPermissionManager: ExactAlarmPermissionManager by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@RemindMeAgainApplication)
            modules(
                dataModule,
                domainModule,
                notificationModule,
                uiModule
            )
        }

        // Log exact alarm permission status on app startup
        exactAlarmPermissionManager.logExactAlarmPermissionStatus()

        // TODO: On app startup, reschedule all reminders using AlarmManager
        // Reschedule all reminders using AlarmManager on app startup
        CoroutineScope(Dispatchers.Default).launch {
            reminderScheduler.rescheduleAllReminders()
        }
    }
}
