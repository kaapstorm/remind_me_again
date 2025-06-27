package com.kaapstorm.remindmeagain.data.db

import androidx.room.*
import com.kaapstorm.remindmeagain.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id NOT IN (SELECT reminderId FROM complete_actions) ORDER BY time ASC")
    fun getActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE time = :time ORDER BY id ASC")
    fun getByTime(time: LocalTime): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeById(id: Long): Flow<Reminder?>
}
