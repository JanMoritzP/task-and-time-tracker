package com.example.taskandtimemanager.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.taskandtimemanager.model.AppUsageAggregate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Wrapper around [UsageStatsManager] used to derive automatic app usage.
 *
 * This class is responsible for:
 *  - Exposing convenience methods for per-day / per-week usage per package
 *  - Periodically updating [AppUsageAggregate.usedMinutesAutomatic] for all [TrackedApp] entries
 */
class UsageTracker(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val trackedAppDao: TrackedAppDao,
    private val appUsageAggregateDao: AppUsageAggregateDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {

    private var updateJob: Job? = null

    /** Start periodic background updates of [AppUsageAggregate.usedMinutesAutomatic]. */
    fun startAutomaticUpdates(intervalMinutes: Long = 15L) {
        if (updateJob?.isActive == true) return

        updateJob = scope.launch {
            while (isActive) {
                try {
                    updateAllTrackedAppsForToday()
                } catch (_: SecurityException) {
                    // Missing PACKAGE_USAGE_STATS permission – fail silently.
                }
                delay(TimeUnit.MINUTES.toMillis(intervalMinutes))
            }
        }
    }

    /** Stop periodic background updates. */
    fun stopAutomaticUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    /** Returns total minutes of foreground usage for the given package on the given date. */
    fun getDailyUsageMinutes(packageName: String, date: LocalDate): Long {
        val (start, end) = getDayBounds(date)
        val stats = queryUsageStats(start, end)
        val totalMs = stats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
        return TimeUnit.MILLISECONDS.toMinutes(totalMs)
    }

    /** Returns total minutes of foreground usage for the given package in the week ending at [endDateInclusive]. */
    fun getWeeklyUsageMinutes(packageName: String, endDateInclusive: LocalDate): Long {
        val endBounds = getDayBounds(endDateInclusive)
        val end = endBounds.second
        val start = endDateInclusive.minusDays(6).atStartOfDay(defaultZone()).toInstant().toEpochMilli()

        val stats = queryUsageStats(start, end)
        val totalMs = stats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
        return TimeUnit.MILLISECONDS.toMinutes(totalMs)
    }

    /** Returns a map of packageName -> minutes for all apps for the given date. */
    fun getAllAppsDailyUsageMinutes(date: LocalDate): Map<String, Long> {
        val (start, end) = getDayBounds(date)
        val stats = queryUsageStats(start, end)
        val grouped: MutableMap<String, Long> = mutableMapOf()
        for (entry in stats) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(entry.totalTimeInForeground)
            if (minutes <= 0L) continue
            grouped[entry.packageName] = (grouped[entry.packageName] ?: 0L) + minutes
        }
        return grouped
    }

    /** Updates / inserts today's [AppUsageAggregate] rows for all tracked apps. */
    suspend fun updateAllTrackedAppsForToday() {
        val today = LocalDate.now()
        val usageByPackage = getAllAppsDailyUsageMinutes(today)
        val trackedApps = trackedAppDao.getAll()
        val existingAggregates = appUsageAggregateDao.getAll().filter { it.date == today.toString() }
        val existingByAppId = existingAggregates.associateBy { it.appId }

        for (app in trackedApps) {
            val minutes = usageByPackage[app.packageName] ?: 0L
            val existing = existingByAppId[app.id]
            val aggregate = if (existing != null) {
                existing.copy(usedMinutesAutomatic = minutes)
            } else {
                AppUsageAggregate(
                    id = UUID.randomUUID().toString(),
                    appId = app.id,
                    date = today.toString(),
                    usedMinutesAutomatic = minutes,
                )
            }
            appUsageAggregateDao.insert(aggregate)
        }
    }

    private fun queryUsageStats(startMillis: Long, endMillis: Long): List<UsageStats> {
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startMillis,
            endMillis,
        ) ?: emptyList()
    }

    private fun getDayBounds(date: LocalDate): Pair<Long, Long> {
        val zone = defaultZone()
        val start: Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end: Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun defaultZone(): ZoneId = ZoneId.systemDefault()
}
