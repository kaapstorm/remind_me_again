package com.kaapstorm.remindmeagain.data.db

import androidx.room.*
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.model.CompleteAction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderActionDao {
    @Insert
    suspend fun insertCompleteAction(action: CompleteAction): Long

    @Insert
    suspend fun insertPostponeAction(action: PostponeAction): Long

    @Query("SELECT * FROM complete_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastCompleteAction(reminderId: Long): Flow<CompleteAction?>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastPostponeAction(reminderId: Long): Flow<PostponeAction?>

    @Query("SELECT * FROM complete_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getCompleteActionsForReminder(reminderId: Long): Flow<List<CompleteAction>>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getPostponeActionsForReminder(reminderId: Long): Flow<List<PostponeAction>>
}
