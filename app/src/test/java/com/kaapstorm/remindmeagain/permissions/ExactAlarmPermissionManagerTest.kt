package com.kaapstorm.remindmeagain.permissions

import android.app.AlarmManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExactAlarmPermissionManagerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var permissionManager: ExactAlarmPermissionManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        every { context.packageName } returns "com.kaapstorm.remindmeagain"

        permissionManager = ExactAlarmPermissionManager(context, alarmManager)
    }

    @Test
    fun `createPackageUriString returns correct package URI string`() {
        // When
        val packageUriString = permissionManager.createPackageUriString()

        // Then
        assertEquals("package:com.kaapstorm.remindmeagain", packageUriString)
    }

    @Test
    fun `canScheduleExactAlarms returns true when alarm manager allows it`() {
        // Given
        every { alarmManager.canScheduleExactAlarms() } returns true

        // When
        val result = permissionManager.canScheduleExactAlarms()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `canScheduleExactAlarms returns false when alarm manager does not allow it`() {
        // Given
        every { alarmManager.canScheduleExactAlarms() } returns false

        // When
        val result = permissionManager.canScheduleExactAlarms()

        // Then
        assertEquals(false, result)
    }
}
