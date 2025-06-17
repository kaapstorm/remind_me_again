package com.kaapstorm.remindmeagain.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kaapstorm.remindmeagain.data.model.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var reminderDao: ReminderDao
    private lateinit var reminderActionDao: ReminderActionDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries() // For testing only
         .build()
        
        reminderDao = db.reminderDao()
        reminderActionDao = db.reminderActionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testMigration1To2() {
        // Verify that the database can be migrated from version 1 to 2
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "migration-test-db"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
         .build()

        // Verify that the database can be opened and accessed
        db.openHelper.readableDatabase
        db.close()

        // Clean up
        context.getDatabasePath("migration-test-db").delete()
    }

    @Test
    fun testReminderInsertAndRetrieve() = runBlocking {
        // Create test data
        val reminder = Reminder(
            name = "Test Reminder",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )

        // Insert reminder
        val id = reminderDao.insert(reminder)
        assertTrue(id > 0)

        // Retrieve and verify
        val retrieved = reminderDao.getById(id)
        assertNotNull(retrieved)
        assertEquals(reminder.name, retrieved?.name)
        assertEquals(reminder.time, retrieved?.time)
        assertEquals(reminder.schedule, retrieved?.schedule)
    }

    @Test
    fun testStopActionInsertAndRetrieve() = runBlocking {
        // Create test data
        val reminder = Reminder(
            name = "Test Reminder",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )
        val reminderId = reminderDao.insert(reminder)

        val stopAction = StopAction(
            reminderId = reminderId,
            timestamp = Instant.now()
        )

        // Insert action
        val id = reminderActionDao.insertStopAction(stopAction)
        assertTrue(id > 0)

        // Retrieve and verify
        val actions = reminderActionDao.getStopActionsForReminder(reminderId).first()
        assertTrue(actions.isNotEmpty())
        val retrieved = actions.first() as StopAction
        assertEquals(stopAction.reminderId, retrieved.reminderId)
        assertEquals(stopAction.timestamp.epochSecond, retrieved.timestamp.epochSecond)
    }

    @Test
    fun testPostponeActionInsertAndRetrieve() = runBlocking {
        // Create test data
        val reminder = Reminder(
            name = "Test Reminder",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )
        val reminderId = reminderDao.insert(reminder)

        val postponeAction = PostponeAction(
            reminderId = reminderId,
            timestamp = Instant.now(),
            intervalSeconds = 300 // 5 minutes
        )

        // Insert action
        val id = reminderActionDao.insertPostponeAction(postponeAction)
        assertTrue(id > 0)

        // Retrieve and verify
        val actions = reminderActionDao.getPostponeActionsForReminder(reminderId).first()
        assertTrue(actions.isNotEmpty())
        val retrieved = actions.first() as PostponeAction
        assertEquals(postponeAction.reminderId, retrieved.reminderId)
        assertEquals(postponeAction.timestamp.epochSecond, retrieved.timestamp.epochSecond)
        assertEquals(postponeAction.intervalSeconds, retrieved.intervalSeconds)
    }

    @Test
    fun testGetActiveReminders() = runBlocking {
        // Create test data
        val activeReminder = Reminder(
            name = "Active Reminder",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )
        val inactiveReminder = Reminder(
            name = "Inactive Reminder",
            time = LocalTime.of(8, 0),
            schedule = ReminderSchedule.Daily
        )

        // Insert reminders
        reminderDao.insert(activeReminder)
        val inactiveId = reminderDao.insert(inactiveReminder)

        // Create a stop action for the inactive reminder
        val stopAction = StopAction(
            reminderId = inactiveId,
            timestamp = Instant.now()
        )
        reminderActionDao.insertStopAction(stopAction)

        // Verify active reminders
        val activeReminders = reminderDao.getActive().first()
        assertEquals(1, activeReminders.size)
        assertEquals(activeReminder.name, activeReminders[0].name)
    }

    @Test
    fun testGetRemindersByTime() = runBlocking {
        // Create test data
        val time = LocalTime.of(8, 0)
        val reminder1 = Reminder(
            name = "Reminder 1",
            time = time,
            schedule = ReminderSchedule.Daily
        )
        val reminder2 = Reminder(
            name = "Reminder 2",
            time = time,
            schedule = ReminderSchedule.Daily
        )

        // Insert reminders
        reminderDao.insert(reminder1)
        reminderDao.insert(reminder2)

        // Verify reminders by time
        val reminders = reminderDao.getByTime(time).first()
        assertEquals(2, reminders.size)
        assertTrue(reminders.all { it.time == time })
    }
}
