package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.TrackedApp
import java.time.LocalDate

/**
 * Encapsulates operations for tracked apps, automatic usage aggregates and
 * purchased usage minutes.
 */
class AppUsageManager(
    private val trackedAppDao: TrackedAppDao,
    private val appUsageAggregateDao: AppUsageAggregateDao,
    private val appUsagePurchaseDao: AppUsagePurchaseDao,
) {

    suspend fun getTrackedApps(): List<TrackedApp> = trackedAppDao.getAll()

    suspend fun addTrackedApp(app: TrackedApp) = trackedAppDao.insert(app)

    suspend fun updateTrackedApp(app: TrackedApp) = trackedAppDao.update(app)

    suspend fun getAppUsageAggregatesForDate(date: LocalDate): List<AppUsageAggregate> =
        appUsageAggregateDao.getAll().filter { it.date == date.toString() }

    suspend fun addOrUpdateAppUsageAggregate(aggregate: AppUsageAggregate) {
        appUsageAggregateDao.insert(aggregate)
    }

    suspend fun addAppUsagePurchase(purchase: AppUsagePurchase) {
        appUsagePurchaseDao.insert(purchase)
    }

    suspend fun getAppUsagePurchases(appId: String): List<AppUsagePurchase> =
        appUsagePurchaseDao.getAll().filter { it.appId == appId }

    /**
     * Clears all app usage purchases.
     *
     * Currently this deletes every [AppUsagePurchase] record; if per-day semantics
     * are introduced later this can be refined (e.g. clear only today's).
     */
    suspend fun clearAllPurchasesTodayOrAll() {
        appUsagePurchaseDao.deleteAll()
    }
}
