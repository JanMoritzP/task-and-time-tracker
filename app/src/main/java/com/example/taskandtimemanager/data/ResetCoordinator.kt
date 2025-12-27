package com.example.taskandtimemanager.data

import android.content.Context
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Central coordinator for daily and weekly reset logic.
 *
 * This helper is intended to be invoked from foreground flows (e.g. when
 * a tracked app is opened) instead of from a periodic WorkManager.
 */
class ResetCoordinator(
    private val context: Context,
) {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val lastResetStorage: LastResetStorage = LastResetStorage(context)
    private val dataStore: DataStore = DataStore(context)

    /**
     * Perform any due daily / weekly resets based on the current time and
     * the previously persisted reset timestamps.
     */
    suspend fun performResetsIfNeeded(nowInstant: Instant = Instant.now()) {
        val nowZoned = nowInstant.atZone(zoneId)
        val today = nowZoned.toLocalDate()

        performDailyResetIfNeeded(nowInstant, today, nowZoned.toLocalTime())
        performWeeklyResetsIfNeeded(nowInstant, today)
    }

    private suspend fun performDailyResetIfNeeded(
        nowInstant: Instant,
        today: LocalDate,
        currentTime: LocalTime,
    ) {
        // Daily purchase reset is tied to crossing the 06:00 boundary in the
        // system time zone. Before 06:00 we still consider it "yesterday" for
        // purchase purposes.
        val cutoff = LocalTime.of(6, 0)

        val lastDailyResetInstant = lastResetStorage.getLastDailyPurchaseResetInstant()
        val lastDailyResetZoned = lastDailyResetInstant?.atZone(zoneId)

        val logicalResetDateForNow = if (currentTime.isBefore(cutoff)) {
            // Still treat as previous day until 06:00
            today.minusDays(1)
        } else {
            today
        }

        val logicalResetDateForLast = when {
            lastDailyResetZoned == null -> null
            lastDailyResetZoned.toLocalTime().isBefore(cutoff) ->
                lastDailyResetZoned.toLocalDate().minusDays(1)
            else -> lastDailyResetZoned.toLocalDate()
        }

        val boundaryCrossed =
            logicalResetDateForLast == null || logicalResetDateForLast.isBefore(logicalResetDateForNow)

        if (boundaryCrossed) {
            performDailyPurchaseCleanup()
            lastResetStorage.setLastDailyPurchaseResetInstant(nowInstant)
        }
    }

    private suspend fun performWeeklyResetsIfNeeded(
        nowInstant: Instant,
        today: LocalDate,
    ) {
        // Weekly logic is evaluated when we are in a Sunday-to-Monday window.
        if (today.dayOfWeek != DayOfWeek.SUNDAY) {
            return
        }

        val lastWeeklyResetInstant = lastResetStorage.getLastWeeklyCoinResetInstant()
        val lastWeeklyResetDate = lastWeeklyResetInstant?.atZone(zoneId)?.toLocalDate()

        val startOfCurrentWeek = today.minusDays(
            (today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong(),
        )
        val alreadyResetThisWeek =
            lastWeeklyResetDate != null && !lastWeeklyResetDate.isBefore(startOfCurrentWeek)

        if (!alreadyResetThisWeek) {
            performWeeklyCoinReset()
            lastResetStorage.setLastWeeklyCoinResetInstant(nowInstant)
        }

        val lastZeroResetInstant = lastResetStorage.getLastWeeklyCoinZeroResetInstant()
        val lastZeroResetDate = lastZeroResetInstant?.atZone(zoneId)?.toLocalDate()

        val alreadyZeroResetThisSunday = lastZeroResetDate == today
        if (!alreadyZeroResetThisSunday) {
            performWeeklyCoinZeroReset()

            val todayStartOfDay = today.atStartOfDay(zoneId).toInstant()
            lastResetStorage.setLastWeeklyCoinZeroResetInstant(todayStartOfDay)
        }
    }

    private suspend fun performDailyPurchaseCleanup() {
        dataStore.clearAllAppUsagePurchases()
        dataStore.recalculateCoinBalance()
    }

    private suspend fun performWeeklyCoinReset() {
        dataStore.recalculateCoinBalance()
    }

    private suspend fun performWeeklyCoinZeroReset() {
        dataStore.neutralizeAllCoins(useRewardRedemption = true)
    }
}
