package com.kaapstorm.remindmeagain.data.repository

import com.kaapstorm.remindmeagain.data.db.ReminderDao
import com.kaapstorm.remindmeagain.data.db.ReminderActionDao
import com.kaapstorm.remindmeagain.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderActionDao: ReminderActionDao
) {
    // Reminder operations
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insert(reminder)
    
    suspend fun updateReminder(reminder: Reminder) = reminderDao.update(reminder)
    
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)
    
    fun getReminderById(id: Long): Flow<Reminder?> = reminderDao.observeById(id)
    
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()
    
    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActive()
    
    fun getRemindersByTime(time: LocalTime): Flow<List<Reminder>> = reminderDao.getByTime(time)

    // Action operations
    suspend fun insertStopAction(action: StopAction) = reminderActionDao.insertStopAction(action)
    
    suspend fun insertPostponeAction(action: PostponeAction) = reminderActionDao.insertPostponeAction(action)
    
    fun getStopActionsForReminder(reminderId: Long): Flow<List<StopAction>> =
        reminderActionDao.getStopActionsForReminder(reminderId)

    fun getPostponeActionsForReminder(reminderId: Long): Flow<List<PostponeAction>> =
        reminderActionDao.getPostponeActionsForReminder(reminderId)
}
