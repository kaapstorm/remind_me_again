package com.kaapstorm.remindmeagain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Represents a dismiss action when a user dismisses a reminder notification.
 * This does not necessarily mean the task was completed, just that the notification was dismissed.
 */
@Entity(
    tableName = "dismiss_actions",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reminderId"])]
)
data class DismissAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val timestamp: Instant
)

/**
 * Represents a postpone action when a user postpones a reminder notification.
 */
@Entity(
    tableName = "postpone_actions",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reminderId"])]
)
data class PostponeAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val timestamp: Instant,
    val intervalSeconds: Int
)
