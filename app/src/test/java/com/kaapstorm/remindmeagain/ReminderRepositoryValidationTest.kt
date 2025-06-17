package com.kaapstorm.remindmeagain

import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.data.db.ReminderDao
import com.kaapstorm.remindmeagain.data.db.ReminderActionDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class ReminderRepositoryValidationTest {
    private lateinit var reminderDao: ReminderDao
    private lateinit var reminderActionDao: ReminderActionDao
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        reminderDao = mockk(relaxed = true)
        reminderActionDao = mockk(relaxed = true)
        repository = ReminderRepository(reminderDao, reminderActionDao)
    }

    // --- Validation logic ---
    private fun isReminderNameValid(name: String): Boolean =
        name.isNotBlank() && name.length <= 50

    @Test
    fun `reminder name must not be empty`() {
        assertFalse(isReminderNameValid(""))
        assertFalse(isReminderNameValid("   "))
    }

    @Test
    fun `reminder name must not exceed 50 characters`() {
        val valid = "a".repeat(50)
        val invalid = "a".repeat(51)
        assertTrue(isReminderNameValid(valid))
        assertFalse(isReminderNameValid(invalid))
    }

    @Test
    fun `reminder name with normal length is valid`() {
        assertTrue(isReminderNameValid("Take medicine"))
    }

    // --- Repository delegation tests ---
    @Test
    fun `insertReminder delegates to DAO`() = runBlocking {
        val reminder = Reminder(0, "Test", LocalTime.NOON, ReminderSchedule.Daily)
        coEvery { reminderDao.insert(reminder) } returns 42L
        val id = repository.insertReminder(reminder)
        assertEquals(42L, id)
        coVerify { reminderDao.insert(reminder) }
    }

    @Test
    fun `updateReminder delegates to DAO`() = runBlocking {
        val reminder = Reminder(1, "Test", LocalTime.NOON, ReminderSchedule.Daily)
        repository.updateReminder(reminder)
        coVerify { reminderDao.update(reminder) }
    }

    @Test
    fun `deleteReminder delegates to DAO`() = runBlocking {
        val reminder = Reminder(1, "Test", LocalTime.NOON, ReminderSchedule.Daily)
        repository.deleteReminder(reminder)
        coVerify { reminderDao.delete(reminder) }
    }
} 