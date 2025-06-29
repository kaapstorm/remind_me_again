package com.kaapstorm.remindmeagain.data.db

import androidx.room.*
import com.kaapstorm.remindmeagain.data.model.DismissAction
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderActionDao {
    @Insert
    suspend fun insertDismissAction(action: DismissAction): Long

    @Insert
    suspend fun insertPostponeAction(action: PostponeAction): Long

    @Query("SELECT * FROM dismiss_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDismissAction(reminderId: Long): Flow<DismissAction?>

    @Query("SELECT * FROM dismiss_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getDismissActionsForReminder(reminderId: Long): Flow<List<DismissAction>>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    fun getLastPostponeAction(reminderId: Long): Flow<PostponeAction?>

    @Query("SELECT * FROM postpone_actions WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    fun getPostponeActionsForReminder(reminderId: Long): Flow<List<PostponeAction>>
}
