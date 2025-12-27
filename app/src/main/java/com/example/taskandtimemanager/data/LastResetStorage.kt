package com.example.taskandtimemanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * Small persistence helper for tracking when coin related periodic resets last ran.
 *
 * Backed by DataStore Preferences so it is lightweight and safe to use from workers
 * and higher level facades without touching the Room schema.
 */
class LastResetStorage(private val context: Context) {

    private val Context.resetDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "last_reset_prefs",
    )

    private val LAST_WEEKLY_COIN_RESET_DATE = longPreferencesKey("last_weekly_coin_reset_date")
    private val LAST_DAILY_PURCHASE_RESET_DATE = longPreferencesKey("last_daily_purchase_reset_date")

    // Tracks the last calendar date (at system zone) when the weekly coin
    // balance was reset to zero using synthetic neutralizing records.
    private val LAST_WEEKLY_COIN_ZERO_RESET_DATE = longPreferencesKey("last_weekly_coin_zero_reset_date")

    /**
     * Returns the last time the weekly coin reset ran, or null when never.
     * Stored as epoch millis to avoid Instant serialization concerns.
     */
    suspend fun getLastWeeklyCoinResetInstant(): Instant? {
        val prefs = context.resetDataStore.data.first()
        val value = prefs[LAST_WEEKLY_COIN_RESET_DATE] ?: return null
        return Instant.ofEpochMilli(value)
    }

    /**
     * Persist the moment when the weekly coin reset logic has just completed.
     */
    suspend fun setLastWeeklyCoinResetInstant(instant: Instant) {
        context.resetDataStore.edit { prefs ->
            prefs[LAST_WEEKLY_COIN_RESET_DATE] = instant.toEpochMilli()
        }
    }

    /**
     * Returns the last time the daily purchase reset ran, or null when never.
     */
    suspend fun getLastDailyPurchaseResetInstant(): Instant? {
        val prefs = context.resetDataStore.data.first()
        val value = prefs[LAST_DAILY_PURCHASE_RESET_DATE] ?: return null
        return Instant.ofEpochMilli(value)
    }
 
    /**
     * Persist the moment when the daily purchase reset logic has just completed.
     */
    suspend fun setLastDailyPurchaseResetInstant(instant: Instant) {
        context.resetDataStore.edit { prefs ->
            prefs[LAST_DAILY_PURCHASE_RESET_DATE] = instant.toEpochMilli()
        }
    }

    /**
     * Last calendar date when the coin balance was explicitly reset to zero via
     * synthetic neutralizing records. Stored as epoch millis at the start of day.
     */
    suspend fun getLastWeeklyCoinZeroResetInstant(): Instant? {
        val prefs = context.resetDataStore.data.first()
        val value = prefs[LAST_WEEKLY_COIN_ZERO_RESET_DATE] ?: return null
        return Instant.ofEpochMilli(value)
    }

    /**
     * Persist the moment representing the Sunday for which the weekly coin
     * zero-reset has just been executed.
     */
    suspend fun setLastWeeklyCoinZeroResetInstant(instant: Instant) {
        context.resetDataStore.edit { prefs ->
            prefs[LAST_WEEKLY_COIN_ZERO_RESET_DATE] = instant.toEpochMilli()
        }
    }
}
