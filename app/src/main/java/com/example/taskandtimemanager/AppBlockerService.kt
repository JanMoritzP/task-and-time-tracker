package com.example.taskandtimemanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.app.AlertDialog
import android.net.Uri
import com.example.taskandtimemanager.data.AppBlockerStatusHolder
import com.example.taskandtimemanager.data.AppDatabase
import com.example.taskandtimemanager.data.AppUsageManager
import com.example.taskandtimemanager.data.CoinManager
import com.example.taskandtimemanager.data.TrackedAppDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Background service that periodically checks the current foreground app and
 * blocks it once the purchased time is exhausted.
 *
 * IMPORTANT PLATFORM NOTE:
 * - There is no public, fully reliable API to programmatically close another
 *   app on modern Android. This service uses [DevicePolicyManager.setApplicationHidden]
 *   for personal / device-owner style deployments as outlined in requirements.
 * - Foreground app detection requires `PACKAGE_USAGE_STATS` and may be delayed
 *   by the system's batching behaviour.
 */
class AppBlockerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMillis: Long = 5_000 // ~5 seconds for more responsive blocking

    private val notificationChannelId = "app_blocker_channel"
    private val notificationId = 1

    // Data / persistence
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var coinManager: CoinManager
    private lateinit var trackedAppDao: TrackedAppDao

    // System services
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(notificationId, buildNotification())

        val db = AppDatabase.getDatabase(this)
        trackedAppDao = db.trackedAppDao()
        appUsageManager = AppUsageManager(
            trackedAppDao = db.trackedAppDao(),
            appUsageAggregateDao = db.appUsageAggregateDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )
        coinManager = CoinManager(
            taskExecutionDao = db.taskExecutionDao(),
            rewardRedemptionDao = db.rewardRedemptionDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        scheduleNextCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure periodic checks continue as long as the service is running.
        scheduleNextCheck()
        return START_STICKY
    }

    private fun scheduleNextCheck() {
        handler.removeCallbacks(checkRunnable)
        handler.postDelayed(checkRunnable, checkIntervalMillis)
    }

    private val checkRunnable = Runnable {
        scope.launch {
            try {
                checkCurrentForegroundAppAndBlockIfNeeded()
            } finally {
                // Always schedule the next run even if something failed.
                scheduleNextCheck()
            }
        }
    }

    private val TAG = "AppBlockerService"

    /**
     * Core blocking flow:
     *  1. Determine current foreground package.
     *  2. If it matches a [TrackedApp], compute remaining minutes from purchases
     *     vs automatic usage.
     *  3. When remainingMinutes <= 0, show a blocking dialog that lets the user
     *     buy more minutes using coins, or block the app when declined / not enough coins.
     */
    private suspend fun checkCurrentForegroundAppAndBlockIfNeeded() {
        val hasUsageAccess = hasUsageAccessPermission()
        val packageName = getCurrentForegroundPackageNameInternal(hasUsageAccess)

        Log.d(
            TAG,
            "checkCurrentForegroundAppAndBlockIfNeeded() - hasUsageAccess=$hasUsageAccess, " +
                "foreground package=$packageName",
        )

        // For the debug UI we now rely solely on the Settings.Secure flag for
        // whether usage access is granted, and treat an empty stats result as
        // "no recent data yet" instead of "permission not granted".
        AppBlockerStatusHolder.update { previous ->
            previous.copy(
                hasUsageAccess = hasUsageAccess,
                lastCheckedPackage = packageName,
            )
        }

        if (packageName == null) {
            Log.d(TAG, "No foreground package (or no recent stats). Skipping.")
            return
        }

        val trackedApps = appUsageManager.getTrackedApps()
        Log.d(TAG, "Tracked apps: count=${trackedApps.size}")
        val trackedApp = trackedApps.firstOrNull { it.packageName == packageName }
        if (trackedApp == null) {
            Log.d(TAG, "Foreground app $packageName is not a tracked app. Skipping.")
            return
        }

        AppBlockerStatusHolder.update { previous ->
            previous.copy(
                lastTrackedAppName = trackedApp.name,
            )
        }
        Log.d(TAG, "Matched tracked app: id=${trackedApp.id}, name=${trackedApp.name}, costPerMinute=${trackedApp.costPerMinute}, purchasedMinutesTotal=${trackedApp.purchasedMinutesTotal}")

        val today = LocalDate.now()
        val aggregatesToday = appUsageManager.getAppUsageAggregatesForDate(today)
        val usedMinutesAutomatic: Long = aggregatesToday
            .filter { it.appId == trackedApp.id }
            .sumOf { it.usedMinutesAutomatic }
        Log.d(TAG, "Used minutes today (automatic) for appId=${trackedApp.id}: $usedMinutesAutomatic")

        val purchases = appUsageManager.getAppUsagePurchases(trackedApp.id)
        val totalPurchasedMinutes: Long = purchases.sumOf { it.minutesPurchased }
        Log.d(TAG, "Total purchased minutes for appId=${trackedApp.id}: $totalPurchasedMinutes (purchases count=${purchases.size})")

        // If a hard cap is configured, interpret purchasedMinutesTotal as that cap,
        // otherwise fall back to purchased minutes.
        val effectiveLimitMinutes: Long = if (trackedApp.purchasedMinutesTotal > 0L) {
            trackedApp.purchasedMinutesTotal
        } else {
            totalPurchasedMinutes
        }
        Log.d(TAG, "Effective limit minutes for appId=${trackedApp.id}: $effectiveLimitMinutes")

        val remainingMinutes: Long = effectiveLimitMinutes - usedMinutesAutomatic
        Log.d(TAG, "Remaining minutes for package=$packageName: $remainingMinutes")

        AppBlockerStatusHolder.update { previous ->
            previous.copy(
                lastRemainingMinutes = remainingMinutes,
                lastTrackedAppName = trackedApp.name,
                lastCheckedPackage = packageName,
            )
        }
 
         if (remainingMinutes > 0) {
             Log.d(TAG, "Still have remaining time. Not blocking.")
             // If there is an overlay currently showing for this package, tell it to dismiss.
             val dismissIntent = AppBlockerOverlayService.createDismissIntent(this@AppBlockerService)
             startService(dismissIntent)
             return
         }
 
         Log.d(TAG, "No remaining time. Starting overlay for $packageName")
 
         // Start the full-screen overlay service which shows the blocking UI on top of the app.
         val overlayIntent = AppBlockerOverlayService.createStartIntent(
             context = this@AppBlockerService,
             targetPackage = trackedApp.packageName,
             targetAppName = trackedApp.name,
         )
 
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             startForegroundService(overlayIntent)
         } else {
             startService(overlayIntent)
         }
 
         // For debug / diagnostics UI.
         AppBlockerStatusHolder.update { previous ->
             previous.copy(
                 lastBlockActionPackage = trackedApp.packageName,
             )
         }
     }

    /**
     * Attempts to determine the foreground app using UsageStatsManager.
     * This is inherently best-effort and may lag behind real time.
     *
     * [hasUsageAccess] is passed in so we can record it for diagnostics
     * without re-checking it here.
     */
    private fun getCurrentForegroundPackageNameInternal(hasUsageAccess: Boolean): String? {
        // Ensure the user has granted the "Usage Access" permission.
//        if (!hasUsageAccess) {
//            Log.d(TAG, "Usage access permission not granted.")
//            return null
//        }

        val endTimeShort = System.currentTimeMillis()
        val startTimeShort = endTimeShort - 60_000 // last 60 seconds
        val endTimeLong = endTimeShort
        val startTimeLong = endTimeShort - 24 * 60 * 60 * 1000L // last 24 hours

        Log.d(TAG, "Querying usage stats (short window) from $startTimeShort to $endTimeShort")
        val shortStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeShort,
            endTimeShort,
        )

        Log.d(TAG, "Querying usage stats (24h window) from $startTimeLong to $endTimeLong")
        val longStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeLong,
            endTimeLong,
        )

        Log.d(
            TAG,
            "Usage stats sizes: shortWindow=${shortStats?.size ?: -1}, longWindow=${longStats?.size ?: -1}",
        )

        val statsToUse = when {
            !shortStats.isNullOrEmpty() -> shortStats
            !longStats.isNullOrEmpty() -> longStats
            else -> {
                Log.d(TAG, "Both short and long usage stats windows are empty or null")
                return null
            }
        }

        val recent = statsToUse.maxByOrNull { it.lastTimeUsed }
        if (recent == null) {
            Log.d(TAG, "No recent usage stats item found in chosen window")
            return null
        }
        Log.d(TAG, "Most recently used package=${recent.packageName}, lastTimeUsed=${recent.lastTimeUsed}")
        return recent.packageName
    }

    private fun hasUsageAccessPermission(): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                contentResolver,
                "usage_stats_enabled",
                0,
            ) == 1
            Log.d(TAG, "hasUsageAccessPermission() = $enabled")
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access permission", e)
            false
        }
    }

    /**
     * Enforces blocking when no time is left.
     *
     * NOTE: We deliberately do not show any UI from this Service, because
     * modern Android versions restrict background activity launches and
     * overlay windows (TYPE_APPLICATION_OVERLAY) from services. The UI for
     * buying additional minutes should be surfaced from an Activity instead.
     */
    private fun showBlockingDialog(targetPackage: String) {
        // No dialog / overlay from the service. Just enforce the block.
        scope.launch { hideApplication(targetPackage) }
    }

    /**
     * Handles the "buy more minutes" flow from the blocking dialog. When there
     * are insufficient coins, the target app is blocked instead.
     */
    private suspend fun handlePurchaseOrBlock(targetPackage: String, minutesToBuy: Long) {
        Log.d(TAG, "handlePurchaseOrBlock(targetPackage=$targetPackage, minutesToBuy=$minutesToBuy)")

        // We need to translate from package name back to TrackedApp id.
        val trackedApps = appUsageManager.getTrackedApps()
        val app = trackedApps.firstOrNull { it.packageName == targetPackage }
        if (app == null) {
            Log.w(TAG, "No TrackedApp found for package=$targetPackage. Hiding app directly.")
            hideApplication(targetPackage)
            return
        }
        Log.d(TAG, "Found TrackedApp for purchase: id=${app.id}, costPerMinute=${app.costPerMinute}")

        // Compute cost and check coin balance.
        val coinsRequired = (app.costPerMinute * minutesToBuy).toInt()
        val currentBalance = coinManager.getCoinBalance()
        Log.d(TAG, "coinsRequired=$coinsRequired, currentBalance=$currentBalance")

        if (coinsRequired <= 0 || currentBalance < coinsRequired) {
            Log.d(TAG, "Not enough coins or zero cost. Blocking app. coinsRequired=$coinsRequired, balance=$currentBalance")
            // Not enough coins – enforce blocking.
            hideApplication(targetPackage)
            return
        }

        val purchase = com.example.taskandtimemanager.model.AppUsagePurchase(
            id = UUID.randomUUID().toString(),
            appId = app.id,
            minutesPurchased = minutesToBuy,
            coinsSpent = coinsRequired,
            purchaseDateTime = LocalDateTime.now().toString(),
        )
        Log.d(TAG, "Recording purchase: $purchase")
        appUsageManager.addAppUsagePurchase(purchase)

        // After successful purchase we simply allow the app to continue.
        Log.d(TAG, "Purchase successful. Allowing app to continue.")
    }

    /**
     * Hides (effectively blocks) the given application package using the
     * DevicePolicyManager. This requires that the app is an active device admin
     * / device owner, which is acceptable for personal deployments but not for
     * general Play Store apps.
     */
    private suspend fun hideApplication(packageName: String) {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Log.w(TAG, "hideApplication: admin not active, cannot hide $packageName")
            return
        }

        AppBlockerStatusHolder.update { previous ->
            previous.copy(
                lastBlockActionPackage = packageName,
            )
        }

        try {
            Log.d(TAG, "hideApplication: attempting to hide $packageName")
            val success = devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
            Log.d(TAG, "hideApplication: setApplicationHidden returned $success for $packageName")
        } catch (e: SecurityException) {
            Log.e(TAG, "hideApplication: SecurityException while hiding $packageName", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "App Blocker",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps the app blocker running to monitor and block distracting apps."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, notificationChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("App blocker running")
            .setContentText("Monitoring distracting apps and enforcing limits.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
