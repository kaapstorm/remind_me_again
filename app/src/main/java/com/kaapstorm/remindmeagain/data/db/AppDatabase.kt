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
        CompleteAction::class,
        PostponeAction::class
    ],
    version = 1,
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
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Example migration for future schema changes
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is a placeholder for future migrations
                // When we need to make schema changes, we'll add the SQL here
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