package com.kaapstorm.remindmeagain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kaapstorm.remindmeagain.R
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReminderScheduleText(schedule: ReminderSchedule): String {
    return when (schedule) {
        is ReminderSchedule.Daily -> stringResource(R.string.schedule_daily)
        
        is ReminderSchedule.Weekly -> {
            val days = schedule.days.joinToString(", ") { dayOfWeek ->
                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
            days
        }

        is ReminderSchedule.Fortnightly -> {
            val dayName = schedule.day.getDisplayName(TextStyle.FULL, Locale.getDefault())
            stringResource(R.string.schedule_fortnightly_format, dayName)
        }
        
        is ReminderSchedule.Monthly -> {
            when {
                schedule.dayOfMonth != null -> {
                    stringResource(R.string.schedule_monthly_day, schedule.dayOfMonth)
                }
                schedule.dayOfWeek != null && schedule.weekOfMonth != null -> {
                    val dayName = schedule.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val ordinal = getOrdinal(schedule.weekOfMonth)
                    stringResource(R.string.schedule_monthly_week, ordinal, dayName)
                }
                else -> stringResource(R.string.schedule_monthly)
            }
        }
    }
}

private fun getOrdinal(number: Int): String {
    return when {
        number % 100 in 11..13 -> "${number}th"
        number % 10 == 1 -> "${number}st"
        number % 10 == 2 -> "${number}nd"
        number % 10 == 3 -> "${number}rd"
        else -> "${number}th"
    }
}