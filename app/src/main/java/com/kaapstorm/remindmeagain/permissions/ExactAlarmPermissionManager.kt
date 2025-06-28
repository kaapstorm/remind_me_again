package com.kaapstorm.remindmeagain.permissions

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

class ExactAlarmPermissionManager(
    private val context: Context,
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
) {

    /**
     * Check if the app can schedule exact alarms
     * @return true if exact alarms can be scheduled, false otherwise
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Create the package URI string for the current app
     * @return URI string for the current package
     */
    fun createPackageUriString(): String {
        return "package:${context.packageName}"
    }

    /**
     * Request the SCHEDULE_EXACT_ALARM permission by opening the system settings
     * @return Intent to open the exact alarm settings page
     */
    fun getExactAlarmSettingsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse(createPackageUriString())
        }
    }

    /**
     * Log the current exact alarm permission status
     */
    fun logExactAlarmPermissionStatus() {
        val canSchedule = canScheduleExactAlarms()
        Log.d("ExactAlarmPermission", "Can schedule exact alarms: $canSchedule")

        if (!canSchedule) {
            Log.w("ExactAlarmPermission", "Exact alarm permission not granted. Snooze functionality may not work properly.")
        }
    }
}
