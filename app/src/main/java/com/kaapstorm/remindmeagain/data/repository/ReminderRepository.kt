package com.kaapstorm.remindmeagain.data.repository

import com.kaapstorm.remindmeagain.data.db.ReminderDao
import com.kaapstorm.remindmeagain.data.db.ReminderActionDao
import com.kaapstorm.remindmeagain.data.model.DismissAction
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderActionDao: ReminderActionDao
) {
    // Reminder operations
    suspend fun insertReminder(reminder: Reminder) = reminderDao.insert(reminder)
    
    suspend fun updateReminder(reminder: Reminder) = reminderDao.update(reminder)
    
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)
    
    suspend fun deleteReminder(id: Long) = reminderDao.deleteById(id)
    
    fun getReminderById(id: Long): Flow<Reminder?> = reminderDao.observeById(id)
    
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()
    
    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActive()
    
    fun getRemindersByTime(time: LocalTime): Flow<List<Reminder>> = reminderDao.getByTime(time)

    // Action operations
    suspend fun insertDismissAction(action: DismissAction) = reminderActionDao.insertDismissAction(action)
    
    suspend fun insertPostponeAction(action: PostponeAction) = reminderActionDao.insertPostponeAction(action)
    
    fun getDismissActionsForReminder(reminderId: Long): Flow<List<DismissAction>> =
        reminderActionDao.getDismissActionsForReminder(reminderId)

    fun getPostponeActionsForReminder(reminderId: Long): Flow<List<PostponeAction>> =
        reminderActionDao.getPostponeActionsForReminder(reminderId)

    fun getLastDismissAction(reminderId: Long): Flow<DismissAction?> =
        reminderActionDao.getLastDismissAction(reminderId)

    fun getLastPostponeAction(reminderId: Long): Flow<PostponeAction?> =
        reminderActionDao.getLastPostponeAction(reminderId)
}
