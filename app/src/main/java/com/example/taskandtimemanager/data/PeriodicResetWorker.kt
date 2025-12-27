package com.example.taskandtimemanager.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic background worker that performs housekeeping tasks:
 *  - Daily cleanup of purchased app time.
 *  - Weekly coin related reset when due.
 */
class PeriodicResetWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val dataStore = DataStore(applicationContext)
            val lastResetStorage = LastResetStorage(applicationContext)

            try {
                performResets(dataStore, lastResetStorage)
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }

    /**
     * Performs daily and weekly reset logic using [DataStore] and [LastResetStorage].
     */
    private suspend fun performResets(
        dataStore: DataStore,
        lastResetStorage: LastResetStorage,
    ) {
        // Delegate to [ResetCoordinator] so that the same logic can also
        // be reused from foreground flows (e.g. when a tracked app is opened).
        val coordinator = ResetCoordinator(applicationContext)
        coordinator.performResetsIfNeeded()
    }
}
