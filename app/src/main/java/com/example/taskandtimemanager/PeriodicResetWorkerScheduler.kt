package com.example.taskandtimemanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.taskandtimemanager.data.PeriodicResetWorker
import java.util.concurrent.TimeUnit

/**
 * Legacy helper responsible for scheduling the [PeriodicResetWorker].
 *
 * The app has been migrated to perform daily / weekly resets from
 * foreground flows (e.g. when tracked apps are opened) via
 * [com.example.taskandtimemanager.data.ResetCoordinator]. To avoid
 * duplicate work and conserve resources, the scheduling logic is now a
 * no-op but kept for backwards compatibility and potential future use.
 */
object PeriodicResetWorkerScheduler {

    private const val UNIQUE_WORK_NAME = "PeriodicResetWorker.daily"

    /**
     * Intentionally left as a no-op; retained to avoid crashes if still
     * referenced from old entry points.
     */
    fun enqueue(appContext: Context) {
        // No-op.
    }
}
