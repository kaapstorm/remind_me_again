package com.kaapstorm.remindmeagain.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define the DataStore at the top level
private val Context.snoozeDataStore: DataStore<Preferences> by preferencesDataStore(name = "snooze_prefs")

/**
 * DataStore implementation of SnoozeStateRepository for production use.
 */
class DataStoreSnoozeStateRepository(
    private val context: Context
) : SnoozeStateRepository {

    companion object {
        private const val KEY_SNOOZE_INTERVAL_PREFIX = "snooze_interval_seconds_for_reminder_"
        const val DEFAULT_INITIAL_SNOOZE_SECONDS = 60 // 1 minute
    }

    override suspend fun setSnoozeAlarmInterval(reminderId: Long, intervalSeconds: Int) {
        val key = intPreferencesKey("$KEY_SNOOZE_INTERVAL_PREFIX$reminderId")
        context.snoozeDataStore.edit { preferences ->
            preferences[key] = intervalSeconds
        }
    }

    override suspend fun getCompletedSnoozeInterval(reminderId: Long): Int {
        val key = intPreferencesKey("$KEY_SNOOZE_INTERVAL_PREFIX$reminderId")
        return context.snoozeDataStore.data.map { preferences ->
            preferences[key] ?: DEFAULT_INITIAL_SNOOZE_SECONDS
        }.first()
    }

    override suspend fun clearSnoozeState(reminderId: Long) {
        val key = intPreferencesKey("$KEY_SNOOZE_INTERVAL_PREFIX$reminderId")
        context.snoozeDataStore.edit { preferences ->
            preferences -= key
        }
    }
} 