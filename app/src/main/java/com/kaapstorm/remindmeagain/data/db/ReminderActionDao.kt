package com.kaapstorm.remindmeagain.data.db

import androidx.room.*
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.model.StopAction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderActionDao {
    @Insert
    suspend fun insertStopAction(action: StopAction): Long

    @Insert
    suspend fun insertPostponeAction(action: PostponeAction): Long

    @Query("SELECT * FROM stop_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastStopAction(reminderId: Long): Flow<StopAction?>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastPostponeAction(reminderId: Long): Flow<PostponeAction?>

    @Query("SELECT * FROM stop_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getStopActionsForReminder(reminderId: Long): Flow<List<StopAction>>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getPostponeActionsForReminder(reminderId: Long): Flow<List<PostponeAction>>
}
