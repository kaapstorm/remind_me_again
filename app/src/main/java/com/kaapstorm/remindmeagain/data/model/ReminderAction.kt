package com.kaapstorm.remindmeagain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "reminder_actions",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReminderAction::class,
            parentColumns = ["id"],
            childColumns = ["lastActionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
abstract class ReminderAction {
    abstract val reminderId: Long
    abstract val lastActionId: Long?
    abstract val timestamp: Instant
}

@Entity(tableName = "stop_actions")
data class StopAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    override val reminderId: Long,
    override val timestamp: Instant,
    override val lastActionId: Long? = null
) : ReminderAction()

@Entity(tableName = "postpone_actions")
data class PostponeAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    override val reminderId: Long,
    override val timestamp: Instant,
    val intervalSeconds: Int,
    override val lastActionId: Long? = null
) : ReminderAction()
