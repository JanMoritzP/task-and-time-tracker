package com.example.taskandtimemanager.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores global configuration and active state for nightly free time.
 *
 * The window is defined by [startHour]/[startMinute] and [endHour]/[endMinute]
 * in local time. [isActive] indicates whether nightly free time is currently
 * active; it is deactivated automatically by the blocker service once the
 * free window has passed.
 */
class NightlyFreeTimePreferences(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "nightly_free_time")

    private object Keys {
        val ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("enabled")
        val START_HOUR: Preferences.Key<Int> = intPreferencesKey("start_hour")
        val START_MINUTE: Preferences.Key<Int> = intPreferencesKey("start_minute")
        val END_HOUR: Preferences.Key<Int> = intPreferencesKey("end_hour")
        val END_MINUTE: Preferences.Key<Int> = intPreferencesKey("end_minute")
        val ACTIVE: Preferences.Key<Boolean> = booleanPreferencesKey("active")
    }

    data class State(
        val enabled: Boolean,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val active: Boolean,
    )

    val stateFlow: Flow<State> =
        context.dataStore.data.map { prefs ->
            State(
                enabled = prefs[Keys.ENABLED] ?: false,
                startHour = prefs[Keys.START_HOUR] ?: 22,
                startMinute = prefs[Keys.START_MINUTE] ?: 0,
                endHour = prefs[Keys.END_HOUR] ?: 6,
                endMinute = prefs[Keys.END_MINUTE] ?: 0,
                active = prefs[Keys.ACTIVE] ?: false,
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
            // When disabling the feature entirely, also clear active flag.
            if (!enabled) {
                prefs[Keys.ACTIVE] = false
            }
        }
    }

    suspend fun setWindow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.START_HOUR] = startHour
            prefs[Keys.START_MINUTE] = startMinute
            prefs[Keys.END_HOUR] = endHour
            prefs[Keys.END_MINUTE] = endMinute
        }
    }

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE] = active
        }
    }
}