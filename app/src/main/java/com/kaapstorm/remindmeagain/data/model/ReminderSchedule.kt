package com.kaapstorm.remindmeagain.data.model

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate

sealed class ReminderSchedule {
    data object Daily : ReminderSchedule()
    
    data class Weekly(
        val days: Set<DayOfWeek>
    ) : ReminderSchedule()
    
    data class Fortnightly(
        val date: LocalDate
    ) : ReminderSchedule()
    
    data class Monthly(
        val dayOfMonth: Int? = null,
        val dayOfWeek: DayOfWeek? = null,
        val weekOfMonth: Int? = null
    ) : ReminderSchedule()

    companion object {
        @TypeConverter
        fun fromString(value: String): ReminderSchedule {
            return when {
                value.startsWith("DAILY") -> Daily
                value.startsWith("WEEKLY") -> {
                    val days = value.substringAfter("WEEKLY:")
                        .split(",")
                        .map { DayOfWeek.valueOf(it) }
                        .toSet()
                    Weekly(days)
                }
                value.startsWith("FORTNIGHTLY") -> {
                    val dateString = value.substringAfter("FORTNIGHTLY:")
                    val date = LocalDate.parse(dateString)
                    Fortnightly(date)
                }
                value.startsWith("MONTHLY") -> {
                    val parts = value.substringAfter("MONTHLY:").split(",")
                    when {
                        parts.size == 1 -> Monthly(dayOfMonth = parts[0].toInt())
                        parts.size == 2 -> Monthly(
                            dayOfWeek = DayOfWeek.valueOf(parts[0]),
                            weekOfMonth = parts[1].toInt()
                        )
                        else -> throw IllegalArgumentException("Invalid monthly schedule format")
                    }
                }
                else -> throw IllegalArgumentException("Unknown schedule type")
            }
        }

        @TypeConverter
        fun toString(schedule: ReminderSchedule): String {
            return when (schedule) {
                is Daily -> "DAILY"
                is Weekly -> "WEEKLY:${schedule.days.joinToString(",") { it.name }}"
                is Fortnightly -> "FORTNIGHTLY:${schedule.date}"
                is Monthly -> when {
                    schedule.dayOfMonth != null -> "MONTHLY:${schedule.dayOfMonth}"
                    schedule.dayOfWeek != null && schedule.weekOfMonth != null ->
                        "MONTHLY:${schedule.dayOfWeek},${schedule.weekOfMonth}"
                    else -> throw IllegalArgumentException("Invalid monthly schedule")
                }
            }
        }
    }
} 