package com.kaapstorm.remindmeagain.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.kaapstorm.remindmeagain.data.model.*
import java.time.Instant
import java.time.LocalTime
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Reminder::class,
        DismissAction::class,
        PostponeAction::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderActionDao(): ReminderActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remind_me_again.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration for fortnightly schedule format change
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update fortnightly schedules from FORTNIGHTLY:DAY to FORTNIGHTLY:DATE
                // Map each day to a corresponding date in January 2025
                db.execSQL("""
                    UPDATE reminders
                    SET schedule = CASE
                        WHEN schedule = 'FORTNIGHTLY:MONDAY' THEN 'FORTNIGHTLY:2025-01-06'
                        WHEN schedule = 'FORTNIGHTLY:TUESDAY' THEN 'FORTNIGHTLY:2025-01-07'
                        WHEN schedule = 'FORTNIGHTLY:WEDNESDAY' THEN 'FORTNIGHTLY:2025-01-01'
                        WHEN schedule = 'FORTNIGHTLY:THURSDAY' THEN 'FORTNIGHTLY:2025-01-02'
                        WHEN schedule = 'FORTNIGHTLY:FRIDAY' THEN 'FORTNIGHTLY:2025-01-03'
                        WHEN schedule = 'FORTNIGHTLY:SATURDAY' THEN 'FORTNIGHTLY:2025-01-04'
                        WHEN schedule = 'FORTNIGHTLY:SUNDAY' THEN 'FORTNIGHTLY:2025-01-05'
                        ELSE schedule
                    END
                    WHERE schedule LIKE 'FORTNIGHTLY:%'
                """.trimIndent())
            }
        }

        // Migration for renaming CompleteAction to DismissAction
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename complete_actions table to dismiss_actions
                db.execSQL("ALTER TABLE complete_actions RENAME TO dismiss_actions")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalTime? {
        return value?.let { LocalTime.ofSecondOfDay(it) }
    }

    @TypeConverter
    fun localTimeToTimestamp(time: LocalTime?): Long? {
        return time?.toSecondOfDay()?.toLong()
    }

    @TypeConverter
    fun fromInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochSecond(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.epochSecond
    }

    @TypeConverter
    fun fromReminderSchedule(value: String?): ReminderSchedule? {
        return value?.let { ReminderSchedule.fromString(it) }
    }

    @TypeConverter
    fun reminderScheduleToString(schedule: ReminderSchedule?): String? {
        return schedule?.let { ReminderSchedule.toString(it) }
    }
}
