package com.example.taskandtimemanager.data

/**
 * Simple diagnostics model + holder for the app blocker service so we can see
 * what it thinks is happening without needing logcat.
 */

data class AppBlockerStatus(
    val hasUsageAccess: Boolean = false,
    val lastCheckedPackage: String? = null,
    val lastTrackedAppName: String? = null,
    val lastRemainingMinutes: Long? = null,
    val lastBlockActionPackage: String? = null,
)

/**
 * In-memory holder for the latest [AppBlockerStatus]. This is intentionally
 * simple and process-local; it is only meant for manual debugging on-device.
 */
object AppBlockerStatusHolder {

    @Volatile
    private var current: AppBlockerStatus = AppBlockerStatus()

    fun update(transform: (AppBlockerStatus) -> AppBlockerStatus) {
        current = transform(current)
    }

    fun get(): AppBlockerStatus = current
}
