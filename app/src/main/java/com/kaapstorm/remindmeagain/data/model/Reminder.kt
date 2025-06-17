package com.kaapstorm.remindmeagain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val time: LocalTime,
    val schedule: ReminderSchedule
) 